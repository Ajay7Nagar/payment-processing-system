package com.example.payments.adapters.api.subscriptions.dto;

import jakarta.validation.constraints.FutureOrPresent;
import java.time.OffsetDateTime;
import java.util.Optional;

public record SubscriptionActionRequest(Optional<@FutureOrPresent OffsetDateTime> nextBillingAt) {
}
