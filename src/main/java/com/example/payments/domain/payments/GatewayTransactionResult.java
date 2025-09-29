package com.example.payments.domain.payments;

import java.time.OffsetDateTime;

public record GatewayTransactionResult(
        boolean success,
        String transactionId,
        String responseCode,
        String responseMessage,
        OffsetDateTime processedAt) {

    public static GatewayTransactionResult success(String transactionId, String responseCode, String responseMessage,
            OffsetDateTime processedAt) {
        return new GatewayTransactionResult(true, transactionId, responseCode, responseMessage, processedAt);
    }

    public static GatewayTransactionResult failure(String responseCode, String responseMessage, OffsetDateTime processedAt) {
        return new GatewayTransactionResult(false, null, responseCode, responseMessage, processedAt);
    }
}
