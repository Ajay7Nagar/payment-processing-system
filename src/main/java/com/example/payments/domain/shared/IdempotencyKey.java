package com.example.payments.domain.shared;

import java.util.Objects;
import java.util.regex.Pattern;

public record IdempotencyKey(String value) {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,128}$");

    public IdempotencyKey {
        Objects.requireNonNull(value, "Idempotency key cannot be null");
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid idempotency key format");
        }
    }
}
