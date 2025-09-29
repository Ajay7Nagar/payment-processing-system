package com.example.payments.adapters.api.subscriptions.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

public record SubscriptionUpdateRequest(
        Optional<String> planCode,
        Optional<@DecimalMin("0.01") BigDecimal> amount,
        Optional<@Pattern(regexp = "^[A-Z]{3}$") String> currency,
        Optional<String> paymentMethodToken,
        Optional<Integer> maxRetryAttempts,
        Optional<Integer> intervalDays,
        Optional<@FutureOrPresent OffsetDateTime> nextBillingAt) {
}
