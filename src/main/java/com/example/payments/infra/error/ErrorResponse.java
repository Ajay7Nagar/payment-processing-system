package com.example.payments.infra.error;

import java.time.OffsetDateTime;

public record ErrorResponse(String errorCode, String message, OffsetDateTime timestamp) {
    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, OffsetDateTime.now());
    }
}
