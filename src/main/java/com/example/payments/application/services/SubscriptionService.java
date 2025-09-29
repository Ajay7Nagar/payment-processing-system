package com.example.payments.application.services;

import com.example.payments.adapters.persistence.DunningHistoryRepository;
import com.example.payments.adapters.persistence.SubscriptionRepository;
import com.example.payments.adapters.persistence.SubscriptionScheduleRepository;
import com.example.payments.application.properties.SubscriptionProperties;
import com.example.payments.domain.billing.DunningHistory;
import com.example.payments.domain.billing.Subscription;
import com.example.payments.domain.billing.SubscriptionBillingCycle;
import com.example.payments.domain.billing.SubscriptionException;
import com.example.payments.domain.billing.SubscriptionSchedule;
import com.example.payments.domain.billing.SubscriptionSchedule.ScheduleStatus;
import com.example.payments.domain.billing.SubscriptionStatus;
import com.example.payments.domain.shared.CorrelationId;
import com.example.payments.domain.shared.IdempotencyKey;
import com.example.payments.infra.gateway.AuthorizeNetClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service orchestrating subscription lifecycle, billing schedules, retries, and Authorize.Net charges.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionScheduleRepository subscriptionScheduleRepository;
    private final DunningHistoryRepository dunningHistoryRepository;
    private final AuthorizeNetClient authorizeNetClient;
    private final Clock clock;
    private final IdempotencyService idempotencyService;
    private final SubscriptionProperties properties;
    private final Counter subscriptionCreateCounter;
    private final Counter subscriptionChargeSuccessCounter;
    private final Counter subscriptionChargeFailureCounter;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
            SubscriptionScheduleRepository subscriptionScheduleRepository,
            DunningHistoryRepository dunningHistoryRepository, AuthorizeNetClient authorizeNetClient, Clock clock,
            IdempotencyService idempotencyService, SubscriptionProperties properties, MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionScheduleRepository = subscriptionScheduleRepository;
        this.dunningHistoryRepository = dunningHistoryRepository;
        this.authorizeNetClient = authorizeNetClient;
        this.clock = clock;
        this.idempotencyService = idempotencyService;
        this.properties = properties;
        this.subscriptionCreateCounter = Counter.builder("subscriptions.create.count").register(meterRegistry);
        this.subscriptionChargeSuccessCounter = Counter.builder("subscriptions.charge.success.count").register(meterRegistry);
        this.subscriptionChargeFailureCounter = Counter.builder("subscriptions.charge.failure.count").register(meterRegistry);
    }

    @Transactional
    public Subscription createSubscription(UUID customerId, String planCode, String clientReference,
            BigDecimal amount, String currency, SubscriptionBillingCycle cycle, OffsetDateTime trialEnd,
            OffsetDateTime firstBilling, IdempotencyKey idempotencyKey, CorrelationId correlationId,
            String paymentMethodToken, int maxRetryAttempts, Integer intervalDays) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        idempotencyService.findExisting(idempotencyKey).ifPresent(existing -> {
            throw new SubscriptionException("DUPLICATE_REQUEST", "Subscription creation already processed");
        });
        subscriptionRepository.findByClientReference(clientReference)
                .ifPresent(existing -> {
                    throw new SubscriptionException("DUPLICATE_SUBSCRIPTION",
                            "Subscription already exists for reference " + clientReference);
                });
        Subscription subscription = Subscription.create(customerId, planCode, cycle, intervalDays, amount,
                currency, paymentMethodToken, clientReference, trialEnd, firstBilling, maxRetryAttempts, now);
        Subscription saved = subscriptionRepository.save(subscription);
        SubscriptionSchedule schedule = SubscriptionSchedule.pending(saved, 0, firstBilling, now);
        subscriptionScheduleRepository.save(schedule);
        idempotencyService.persist(idempotencyKey,
                correlationId.value() + "-subscription-create", saved.getId().toString(), 201);
        log.info("Subscription created subscription={} correlationId={}", saved.getId(),
                correlationId.value());
        subscriptionCreateCounter.increment();
        return saved;
    }

    @Transactional
    public void pauseSubscription(UUID subscriptionId) {
        Subscription subscription = loadSubscription(subscriptionId);
        subscription.pause(OffsetDateTime.now(clock));
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription resumeSubscription(UUID subscriptionId, OffsetDateTime nextBillingAt) {
        Subscription subscription = loadSubscription(subscriptionId);
        subscription.resume(nextBillingAt, OffsetDateTime.now(clock));
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription cancelSubscription(UUID subscriptionId) {
        Subscription subscription = loadSubscription(subscriptionId);
        subscription.cancel(OffsetDateTime.now(clock));
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription updateSubscription(UUID subscriptionId, Optional<String> newPlanCode,
            Optional<BigDecimal> newAmount, Optional<String> newCurrency, Optional<String> paymentToken,
            Optional<Integer> maxRetryAttempts, Optional<Integer> intervalDays, Optional<OffsetDateTime> nextBillingAt,
            OffsetDateTime now) {
        Subscription subscription = loadSubscription(subscriptionId);

        String updatedPlan = newPlanCode.orElse(subscription.getPlanCode());
        BigDecimal updatedAmount = newAmount.orElse(subscription.getAmount());
        String updatedCurrency = newCurrency.orElse(subscription.getCurrency());

        if (!updatedPlan.equals(subscription.getPlanCode()) || updatedAmount.compareTo(subscription.getAmount()) != 0
                || !updatedCurrency.equals(subscription.getCurrency())) {
            subscription.updatePlan(updatedPlan, updatedAmount, updatedCurrency, now);
        }

        paymentToken.ifPresent(token -> subscription.updatePaymentMethod(token, now));
        maxRetryAttempts.ifPresent(value -> subscription.setMaxRetryAttempts(value, now));
        intervalDays.ifPresent(days -> subscription.setIntervalDays(days, now));
        nextBillingAt.ifPresent(subscription::setNextBillingAt);

        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public Subscription getSubscription(UUID subscriptionId) {
        return loadSubscription(subscriptionId);
    }

    @Transactional(readOnly = true)
    public List<Subscription> listSubscriptions() {
        return subscriptionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionSchedule> getSchedules(UUID subscriptionId) {
        Subscription subscription = loadSubscription(subscriptionId);
        return subscriptionScheduleRepository.findBySubscriptionIdOrderByScheduledAtAsc(subscription.getId());
    }

    @Transactional(readOnly = true)
    public List<DunningHistory> getDunningHistory(UUID subscriptionId) {
        Subscription subscription = loadSubscription(subscriptionId);
        return dunningHistoryRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscription.getId());
    }

    @Transactional
    public void processDueSubscriptions(OffsetDateTime threshold) {
        List<Subscription> due = subscriptionRepository.findByStatusInAndNextBillingAtLessThanEqual(
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.DELINQUENT), threshold);
        for (Subscription subscription : due) {
            var pendingSchedules = subscriptionScheduleRepository.findBySubscriptionIdAndStatus(subscription.getId(),
                    ScheduleStatus.PENDING);
            pendingSchedules.forEach(schedule -> processSchedule(subscription, schedule));
        }
    }

    private void processSchedule(Subscription subscription, SubscriptionSchedule schedule) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        try {
            authorizeNetClient.purchase(
                    new com.example.payments.domain.payments.Money(subscription.getAmount(), subscription.getCurrency()),
                    subscription.getPaymentMethodToken(), subscription.getClientReference());
            subscription.recordSuccessfulCharge(now);
            subscriptionRepository.save(subscription);
            schedule.markSuccess(now);
            subscriptionScheduleRepository.save(schedule);
            log.info("Subscription charge success subscription={} nextBilling={}", subscription.getId(),
                    subscription.getNextBillingAt());
            subscriptionChargeSuccessCounter.increment();
        } catch (Exception ex) {
            OffsetDateTime nextAttempt = calculateRetryTime(subscription, now);
            subscription.recordFailedCharge(nextAttempt, now);
            subscriptionRepository.save(subscription);
            schedule.markFailure(ex.getMessage(), now);
            subscriptionScheduleRepository.save(schedule);
            dunningHistoryRepository.save(DunningHistory.record(subscription, now, "FAILED", "GATEWAY_ERROR",
                    ex.getMessage(), now));
            log.error("Subscription charge failed subscription={} error={}", subscription.getId(), ex.getMessage(), ex);
            subscriptionChargeFailureCounter.increment();
            if (subscription.hasExceededRetryAttempts()) {
                subscription.cancel(now);
                subscriptionRepository.save(subscription);
                log.warn("Subscription auto-cancelled after max retries subscription={}", subscription.getId());
                return;
            }
            int autoCancelDays = properties.getRetry().getAutoCancelDays();
            if (subscription.shouldAutoCancel(now, autoCancelDays)) {
                subscription.cancel(now);
                subscriptionRepository.save(subscription);
                log.warn("Subscription auto-cancelled after delinquency window subscription={}",
                        subscription.getId());
            } else {
                SubscriptionSchedule retrySchedule = SubscriptionSchedule.pending(subscription,
                        schedule.getAttemptNumber() + 1, nextAttempt, now);
                subscriptionScheduleRepository.save(retrySchedule);
            }
        }
    }

    private OffsetDateTime calculateRetryTime(Subscription subscription, OffsetDateTime now) {
        int retry = subscription.getRetryCount();
        return switch (retry) {
            case 0 -> now.plusDays(1);
            case 1 -> now.plusDays(3);
            case 2 -> now.plusDays(7);
            default -> subscription.calculateNextBillingAfter(now);
        };
    }

    private Subscription loadSubscription(UUID subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionException("SUBSCRIPTION_NOT_FOUND",
                        "Subscription not found for id " + subscriptionId));
    }
}
