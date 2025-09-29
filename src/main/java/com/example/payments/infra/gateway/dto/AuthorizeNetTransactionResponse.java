package com.example.payments.infra.gateway.dto;

import java.time.OffsetDateTime;

public record AuthorizeNetTransactionResponse(
        boolean success,
        String transactionId,
        String responseCode,
        String responseMessage,
        OffsetDateTime processedAt) {

    public static AuthorizeNetTransactionResponse success(String transactionId, String responseCode,
            String responseMessage, OffsetDateTime processedAt) {
        return new AuthorizeNetTransactionResponse(true, transactionId, responseCode, responseMessage, processedAt);
    }

    public static AuthorizeNetTransactionResponse failure(String responseCode, String responseMessage) {
        return new AuthorizeNetTransactionResponse(false, null, responseCode, responseMessage, OffsetDateTime.now());
    }
}
