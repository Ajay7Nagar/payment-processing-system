package com.example.payments.infra.gateway;

import com.example.payments.domain.payments.GatewayTransactionResult;
import com.example.payments.domain.payments.Money;
public interface AuthorizeNetClient {

    GatewayTransactionResult authorize(Money amount, String paymentNonce, String orderId);

    GatewayTransactionResult capture(Money amount, String transactionId);

    GatewayTransactionResult purchase(Money amount, String paymentNonce, String orderId);

    GatewayTransactionResult refund(Money amount, String transactionId, String lastFour);

    GatewayTransactionResult voidTransaction(String transactionId);
}
