package com.example.payments.domain.shared;

import java.util.Objects;
import java.util.UUID;

public record CorrelationId(String value) {

    public CorrelationId {
        Objects.requireNonNull(value, "CorrelationId cannot be null");
        UUID.fromString(value);
    }

    public static CorrelationId newId() {
        return new CorrelationId(UUID.randomUUID().toString());
    }
}
