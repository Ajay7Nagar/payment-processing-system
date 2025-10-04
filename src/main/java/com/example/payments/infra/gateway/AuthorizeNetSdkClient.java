package com.example.payments.infra.gateway;

import net.authorize.Environment;
import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.base.ApiOperationBase;
import com.example.payments.domain.payments.GatewayTransactionResult;
import com.example.payments.domain.payments.Money;
import com.example.payments.domain.payments.PaymentException;
import com.example.payments.infra.gateway.sdk.AuthorizeNetEnvironmentResolver;
import com.example.payments.infra.gateway.sdk.AuthorizeNetMapper;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
//@Profile("sdk")
@Primary
@Profile("!test")
public class AuthorizeNetSdkClient implements AuthorizeNetClient {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeNetSdkClient.class);

    private final AuthorizeNetProperties properties;
    private final ObservationRegistry observationRegistry;
    private final AuthorizeNetMapper mapper;
    private final AuthorizeNetEnvironmentResolver environmentResolver;

    public AuthorizeNetSdkClient(AuthorizeNetProperties properties, ObservationRegistry observationRegistry,
            AuthorizeNetMapper mapper, AuthorizeNetEnvironmentResolver environmentResolver) {
        log.info("Using Authorize.Net SDK client for payment gateway integration");
        this.properties = properties;
        this.observationRegistry = observationRegistry;
        this.mapper = mapper;
        this.environmentResolver = environmentResolver;
    }

    @Override
    public GatewayTransactionResult authorize(Money amount, String paymentNonce, String orderId) {
        return execute(TransactionTypeEnum.AUTH_ONLY_TRANSACTION, amount, paymentNonce, orderId, null);
    }

    @Override
    public GatewayTransactionResult capture(Money amount, String transactionId) {
        return execute(TransactionTypeEnum.PRIOR_AUTH_CAPTURE_TRANSACTION, amount, null, transactionId, transactionId);
    }

    @Override
    public GatewayTransactionResult purchase(Money amount, String paymentNonce, String orderId) {
        return execute(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION, amount, paymentNonce, orderId, null);
    }

    @Override
    public GatewayTransactionResult refund(Money amount, String transactionId, String lastFour) {
        return execute(TransactionTypeEnum.REFUND_TRANSACTION, amount, lastFour, transactionId, transactionId);
    }

    @Override
    public GatewayTransactionResult voidTransaction(String transactionId) {
        return execute(TransactionTypeEnum.VOID_TRANSACTION, Money.zero(properties.getDefaultCurrency()), null, transactionId,
                transactionId);
    }

    private GatewayTransactionResult execute(TransactionTypeEnum type, Money amount, String paymentData, String referenceId,
            String refTransId) {
        return Observation.createNotStarted("authorize.net.call", observationRegistry)
                .lowCardinalityKeyValue("authorize.action", type.value())
                .observe(() -> doExecute(type, amount, paymentData, referenceId, refTransId));
    }

    private GatewayTransactionResult doExecute(TransactionTypeEnum type, Money amount, String paymentData, String referenceId,
            String refTransId) {
        try {
            CreateTransactionRequest request = mapper.buildTransactionRequest(properties, type, amount, paymentData,
                    referenceId, refTransId);

            Environment environment = environmentResolver.resolve(properties);
            ApiOperationBase.setEnvironment(environment);
            ApiOperationBase.setMerchantAuthentication(mapper.buildMerchantAuthentication(properties));

            CreateTransactionController controller = new CreateTransactionController(request);
            controller.execute();

            CreateTransactionResponse response = controller.getApiResponse();
            return mapper.toGatewayResult(type, request, response);
        } catch (Exception ex) {
            log.error("Authorize.Net SDK call failed type={} reference={}", type, referenceId, ex);
            throw new PaymentException("GATEWAY_ERROR", "Authorize.Net call failed", ex);
        }
    }
}
