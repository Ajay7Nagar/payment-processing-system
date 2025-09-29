package com.example.payments.infra.gateway;

import com.example.payments.domain.payments.GatewayTransactionResult;
import com.example.payments.domain.payments.Money;
import com.example.payments.domain.payments.PaymentException;
import com.example.payments.infra.gateway.dto.AuthorizeNetTransactionRequest;
import com.example.payments.infra.gateway.dto.AuthorizeNetTransactionRequest.MerchantAuthentication;
import com.example.payments.infra.gateway.dto.AuthorizeNetTransactionRequest.OpaqueData;
import com.example.payments.infra.gateway.dto.AuthorizeNetTransactionRequest.Payment;
import com.example.payments.infra.gateway.dto.AuthorizeNetTransactionRequest.TransactionRequest;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("!test")
public class AuthorizeNetHttpClient implements AuthorizeNetClient {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeNetHttpClient.class);

    private final RestTemplate restTemplate;
    private final AuthorizeNetProperties properties;
    private final ObservationRegistry observationRegistry;

    public AuthorizeNetHttpClient(AuthorizeNetProperties properties, RestTemplateBuilder builder,
            ObservationRegistry observationRegistry) {
        this.properties = properties;
        this.restTemplate = builder.build();
        this.observationRegistry = observationRegistry;
    }

    @Override
    public GatewayTransactionResult authorize(Money amount, String paymentNonce, String orderId) {
        return execute("authOnlyTransaction", amount, paymentNonce, orderId);
    }

    @Override
    public GatewayTransactionResult capture(Money amount, String transactionId) {
        return execute("priorAuthCaptureTransaction", amount, transactionId, transactionId);
    }

    @Override
    public GatewayTransactionResult purchase(Money amount, String paymentNonce, String orderId) {
        return execute("authCaptureTransaction", amount, paymentNonce, orderId);
    }

    @Override
    public GatewayTransactionResult refund(Money amount, String transactionId, String lastFour) {
        return execute("refundTransaction", amount, lastFour, transactionId);
    }

    @Override
    public GatewayTransactionResult voidTransaction(String transactionId) {
        return execute("voidTransaction", new Money(amountZero(), "USD"), transactionId, transactionId);
    }

    private GatewayTransactionResult execute(String transactionType, Money amount, String paymentData, String referenceId) {
        return Observation.createNotStarted("authorize.net.call", observationRegistry)
                .lowCardinalityKeyValue("authorize.action", transactionType)
                .observe(() -> doExecute(transactionType, amount, paymentData, referenceId));
    }

    private GatewayTransactionResult doExecute(String transactionType, Money amount, String paymentData, String referenceId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("User-Agent", "payments-system/1.0");

            AuthorizeNetTransactionRequest request = buildRequest(transactionType, amount.amount(), paymentData, referenceId);
            HttpEntity<AuthorizeNetTransactionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(URI.create(properties.getEndpoint()), HttpMethod.POST,
                    entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new PaymentException("GATEWAY_ERROR", "Gateway error: " + response.getStatusCode());
            }

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new PaymentException("GATEWAY_ERROR", "Empty response from gateway");
            }

            boolean success = Boolean.TRUE.equals(body.get("success"));
            if (!success) {
                String error = String.valueOf(body.getOrDefault("responseMessage", "Gateway declined"));
                throw new PaymentException("GATEWAY_DECLINED", error);
            }

            return GatewayTransactionResult.success(
                    String.valueOf(body.get("transactionId")),
                    String.valueOf(body.getOrDefault("responseCode", "1")),
                    String.valueOf(body.getOrDefault("responseMessage", "Approved")),
                    OffsetDateTime.now());
        } catch (RestClientException e) {
            log.error("Authorize.Net HTTP call failed action={} reference={}", transactionType, referenceId, e);
            throw new PaymentException("GATEWAY_ERROR", "Authorize.Net call failed", e);
        }
    }

    private AuthorizeNetTransactionRequest buildRequest(String transactionType, BigDecimal amount, String paymentData,
            String referenceId) {
        MerchantAuthentication auth = new MerchantAuthentication(properties.getApiLoginId(), properties.getTransactionKey());
        Payment payment;
        String refTransId = null;

        switch (transactionType) {
            case "refundTransaction" -> {
                // paymentData is last four digits for refund
                payment = new Payment(null, new AuthorizeNetTransactionRequest.CreditCard(paymentData, "XXXX"));
                refTransId = referenceId;
            }
            case "priorAuthCaptureTransaction", "voidTransaction" -> {
                payment = null;
                refTransId = referenceId;
            }
            default -> {
                OpaqueData opaqueData = new OpaqueData(properties.getAcceptPaymentDescriptor(), paymentData);
                payment = new Payment(opaqueData, null);
            }
        }

        TransactionRequest transactionRequest = new TransactionRequest(transactionType, amount, payment, refTransId);
        return new AuthorizeNetTransactionRequest(referenceId, auth, transactionRequest);
    }

    private BigDecimal amountZero() {
        return BigDecimal.ZERO;
    }
}
