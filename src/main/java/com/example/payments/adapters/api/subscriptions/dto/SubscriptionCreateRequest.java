package com.example.payments.adapters.api.subscriptions.dto;

import com.example.payments.domain.billing.SubscriptionBillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionCreateRequest(
        @NotNull UUID customerId,
        @NotBlank String planCode,
        @NotBlank String clientReference,
        @NotNull SubscriptionBillingCycle billingCycle,
        Integer intervalDays,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotBlank String paymentMethodToken,
        @FutureOrPresent OffsetDateTime trialEnd,
        @NotNull @FutureOrPresent OffsetDateTime firstBillingAt,
        Integer maxRetryAttempts,
        @NotBlank String idempotencyKey,
        @NotBlank String correlationId) {
}
