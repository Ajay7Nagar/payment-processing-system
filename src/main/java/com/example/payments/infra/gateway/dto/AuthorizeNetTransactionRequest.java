package com.example.payments.infra.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthorizeNetTransactionRequest(
        String refId,
        MerchantAuthentication merchantAuthentication,
        TransactionRequest transactionRequest) {

    public record MerchantAuthentication(String name, String transactionKey) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TransactionRequest(String transactionType, BigDecimal amount, Payment payment, String refTransId) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Payment(OpaqueData opaqueData, CreditCard creditCard) {
    }

    public record OpaqueData(String dataDescriptor, String dataValue) {
    }

    public record CreditCard(String cardNumber, String expirationDate) {
    }
}
