package com.example.payments.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.payments.adapters.persistence.DunningHistoryRepository;
import com.example.payments.adapters.persistence.SubscriptionRepository;
import com.example.payments.adapters.persistence.SubscriptionScheduleRepository;
import com.example.payments.application.properties.SubscriptionProperties;
import com.example.payments.domain.billing.Subscription;
import com.example.payments.domain.billing.SubscriptionBillingCycle;
import com.example.payments.domain.billing.SubscriptionException;
import com.example.payments.domain.billing.SubscriptionSchedule;
import com.example.payments.domain.billing.SubscriptionStatus;
import com.example.payments.domain.shared.CorrelationId;
import com.example.payments.domain.shared.IdempotencyKey;
import com.example.payments.domain.payments.GatewayTransactionResult;
import com.example.payments.infra.gateway.AuthorizeNetClient;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionScheduleRepository subscriptionScheduleRepository;
    @Mock
    private DunningHistoryRepository dunningHistoryRepository;
    @Mock
    private AuthorizeNetClient authorizeNetClient;
    @Mock
    private IdempotencyService idempotencyService;

    private Clock clock;

    private SubscriptionService subscriptionService;

    private UUID customerId;
    private OffsetDateTime now;
    private SubscriptionProperties properties;

    @Captor
    private ArgumentCaptor<Subscription> subscriptionCaptor;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.of(2025, 9, 29, 0, 0, 0, 0, ZoneOffset.UTC);
        clock = Clock.fixed(now.toInstant(), ZoneOffset.UTC);
        properties = new SubscriptionProperties();
        subscriptionService = new SubscriptionService(subscriptionRepository, subscriptionScheduleRepository,
                dunningHistoryRepository, authorizeNetClient, clock, idempotencyService, properties, new SimpleMeterRegistry());
        customerId = UUID.randomUUID();
    }

    @Test
    void createSubscription_shouldPersistAndSchedule() {
        doReturn(Optional.empty()).when(idempotencyService).findExisting(any());
        doReturn(Optional.empty()).when(subscriptionRepository).findByClientReference(any());
        doReturn(Subscription.create(customerId, "plan-basic", SubscriptionBillingCycle.MONTHLY, null,
                new BigDecimal("99.99"), "USD", "token", "client-1", null, now.plusDays(1), 4, now)).when(subscriptionRepository)
                        .save(any());

        var result = subscriptionService.createSubscription(customerId, "plan-basic", "client-1",
                new BigDecimal("99.99"), "USD", SubscriptionBillingCycle.MONTHLY, null, now.plusDays(1),
                new IdempotencyKey("create-1"), CorrelationId.newId(), "token", 4, null);

        assertThat(result.getPlanCode()).isEqualTo("plan-basic");
        verify(subscriptionScheduleRepository).save(any(SubscriptionSchedule.class));
    }

    @Test
    void createSubscription_shouldRejectDuplicateRequest() {
        doReturn(Optional.ofNullable(null)).when(idempotencyService).findExisting(any());
        doReturn(Optional.of(Subscription.create(customerId, "plan-basic", SubscriptionBillingCycle.MONTHLY,
                null, new BigDecimal("99.99"), "USD", "token", "client-1", null, now.plusDays(1), 4, now)))
                        .when(subscriptionRepository).findByClientReference(any());

        assertThatThrownBy(() -> subscriptionService.createSubscription(customerId, "plan-basic", "client-1",
                new BigDecimal("99.99"), "USD", SubscriptionBillingCycle.MONTHLY, null, now.plusDays(1),
                new IdempotencyKey("duplicate"), CorrelationId.newId(), "token", 4, null))
                        .isInstanceOf(SubscriptionException.class)
                        .hasMessageContaining("Subscription already exists");
    }

    @Test
    void updateSubscription_shouldApplyChanges() {
        Subscription existing = Subscription.create(customerId, "plan-basic",
                SubscriptionBillingCycle.MONTHLY, null, new BigDecimal("99.99"), "USD", "token", "client-1", null,
                now.plusDays(1), 4, now);
        doReturn(Optional.of(existing)).when(subscriptionRepository).findById(existing.getId());
        doReturn(existing).when(subscriptionRepository).save(any(Subscription.class));

        var updated = subscriptionService.updateSubscription(existing.getId(), Optional.of("plan-pro"),
                Optional.of(new BigDecimal("129.99")), Optional.of("EUR"), Optional.of("token-2"), Optional.of(6),
                Optional.of(15), Optional.empty(), now);

        assertThat(updated.getPlanCode()).isEqualTo("plan-pro");
        assertThat(updated.getAmount()).isEqualTo(new BigDecimal("129.99"));
        assertThat(updated.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void processDueSubscriptions_shouldScheduleRetriesOnFailure() {
        Subscription subscription = Subscription.create(customerId, "plan-basic",
                SubscriptionBillingCycle.MONTHLY, null, new BigDecimal("99.99"), "USD", "token", "client-1", null,
                now.plusDays(1), 4, now);
        SubscriptionSchedule schedule = SubscriptionSchedule.pending(subscription, 0, now.minusMinutes(1), now);

        doReturn(List.of(subscription)).when(subscriptionRepository)
                .findByStatusInAndNextBillingAtLessThanEqual(any(), any());
        doReturn(List.of(schedule)).when(subscriptionScheduleRepository).findBySubscriptionIdAndStatus(any(), any());
        doReturn(subscription).when(subscriptionRepository).save(any());
        doReturn(schedule).when(subscriptionScheduleRepository).save(any());
        doThrow(new RuntimeException("Declined")).when(authorizeNetClient).purchase(any(), any(), any());

        subscriptionService.processDueSubscriptions(now);

        verify(subscriptionRepository, atLeastOnce()).save(any());
        verify(subscriptionScheduleRepository, atLeastOnce()).save(any());
        assertThat(subscription.getRetryCount()).isEqualTo(1);
        assertThat(schedule.getStatus()).isEqualTo(SubscriptionSchedule.ScheduleStatus.FAILED);
    }
}
