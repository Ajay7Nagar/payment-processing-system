package com.example.payments.infra.gateway;

import com.example.payments.domain.payments.GatewayTransactionResult;
import com.example.payments.domain.payments.Money;
import java.time.OffsetDateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class AuthorizeNetMockClient implements AuthorizeNetClient {

    @Override
    public GatewayTransactionResult authorize(Money amount, String paymentNonce, String orderId) {
        return success("auth-" + orderId);
    }

    @Override
    public GatewayTransactionResult capture(Money amount, String transactionId) {
        return success("cap-" + transactionId);
    }

    @Override
    public GatewayTransactionResult purchase(Money amount, String paymentNonce, String orderId) {
        return success("purchase-" + orderId);
    }

    @Override
    public GatewayTransactionResult refund(Money amount, String transactionId, String lastFour) {
        return success("refund-" + transactionId);
    }

    @Override
    public GatewayTransactionResult voidTransaction(String transactionId) {
        return success("void-" + transactionId);
    }

    private GatewayTransactionResult success(String reference) {
        return GatewayTransactionResult.success(reference, "1", "Mocked transaction", OffsetDateTime.now());
    }
}
