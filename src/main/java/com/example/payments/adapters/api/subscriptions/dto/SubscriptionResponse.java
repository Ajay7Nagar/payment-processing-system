package com.example.payments.adapters.api.subscriptions.dto;

import com.example.payments.domain.billing.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID customerId,
        String planCode,
        BigDecimal amount,
        String currency,
        SubscriptionStatus status,
        OffsetDateTime nextBillingAt,
        OffsetDateTime trialEnd,
        OffsetDateTime delinquentSince,
        int retryCount,
        int maxRetryAttempts,
        String clientReference) {
}
