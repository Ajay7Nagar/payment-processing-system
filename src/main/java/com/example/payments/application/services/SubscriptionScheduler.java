package com.example.payments.application.services;

import com.example.payments.adapters.persistence.SubscriptionRepository;
import com.example.payments.application.properties.SubscriptionProperties;
import com.example.payments.domain.billing.Subscription;
import com.example.payments.domain.billing.SubscriptionStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final Clock clock;
    private final SubscriptionProperties properties;
    private final MeterRegistry meterRegistry;

    public SubscriptionScheduler(SubscriptionRepository subscriptionRepository,
            SubscriptionService subscriptionService, Clock clock, SubscriptionProperties properties, MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.clock = clock;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(cron = "${subscription.retry.schedule-cron:0 */5 * * * *}")
    public void processDueSubscriptions() {
        OffsetDateTime threshold = OffsetDateTime.now(clock);
        List<Subscription> dueSubscriptions = subscriptionRepository.findByStatusInAndNextBillingAtLessThanEqual(
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.DELINQUENT), threshold);
        if (!dueSubscriptions.isEmpty()) {
            log.debug("Processing {} due subscriptions threshold={}", dueSubscriptions.size(), threshold);
            subscriptionService.processDueSubscriptions(threshold);
            meterRegistry.counter("subscriptions.scheduler.due", "count", String.valueOf(dueSubscriptions.size())).increment();
        }
    }
}
