package com.example.payments.infra.gateway;

import com.example.payments.domain.payments.GatewayTransactionResult;
import com.example.payments.domain.payments.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * REST-based Authorize.Net client. Keeps the legacy SDK client available but
 * switches the default integration to plain JSON calls so we can debug
 * sandbox behaviour more easily.
 */
@Component
@Profile("rest-test")
//@Primary
//@Profile("!test")
public class AuthorizeNetRestClient implements AuthorizeNetClient {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeNetRestClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AuthorizeNetProperties properties;

    public AuthorizeNetRestClient(RestTemplate restTemplate, ObjectMapper objectMapper,
            AuthorizeNetProperties properties) {
        log.info("Using Authorize.Net REST client for payment gateway integration");
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public GatewayTransactionResult authorize(Money amount, String paymentNonce, String orderId) {
        return sendTransaction("authOnlyTransaction", amount, paymentNonce, orderId, null, null);
    }

    @Override
    public GatewayTransactionResult capture(Money amount, String transactionId) {
        return sendTransaction("priorAuthCaptureTransaction", amount, null, transactionId, transactionId, null);
    }

    @Override
    public GatewayTransactionResult purchase(Money amount, String paymentNonce, String orderId) {
        return sendTransaction("authCaptureTransaction", amount, paymentNonce, orderId, null, null);
    }

    @Override
    public GatewayTransactionResult refund(Money amount, String transactionId, String lastFour) {
        return sendTransaction("refundTransaction", amount, null, transactionId, transactionId, lastFour);
    }

    @Override
    public GatewayTransactionResult voidTransaction(String transactionId) {
        return sendTransaction("voidTransaction", Money.zero(properties.getDefaultCurrency()), null, transactionId,
                transactionId, null);
    }

    private GatewayTransactionResult sendTransaction(String type, Money amount, String paymentData, String referenceId,
            String refTransId, String lastFour) {
        try {
            Map<String, Object> payload = buildPayload(type, amount, paymentData, referenceId, refTransId, lastFour);
            String body = objectMapper.writeValueAsString(Map.of("createTransactionRequest", payload));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(properties.getEndpoint(), HttpMethod.POST, entity,
                    Map.class);

            return mapResponse(type, response.getBody());
        } catch (HttpStatusCodeException ex) {
            log.warn("Authorize.Net REST {} failed with status {} and body {}", type, ex.getRawStatusCode(),
                    ex.getResponseBodyAsString());
            return GatewayTransactionResult.failure(String.valueOf(ex.getRawStatusCode()),
                    Optional.ofNullable(ex.getResponseBodyAsString()).orElse("HTTP error"), OffsetDateTime.now());
        } catch (Exception ex) {
            log.error("Authorize.Net REST {} failed: {}", type, ex.getMessage(), ex);
            return GatewayTransactionResult.failure("REST_ERROR", ex.getMessage(), OffsetDateTime.now());
        }
    }

    @SuppressWarnings("unchecked")
    private GatewayTransactionResult mapResponse(String type, Map<String, Object> body) {
        if (body == null) {
            return GatewayTransactionResult.failure("EMPTY_RESPONSE", "Authorize.Net returned empty body",
                    OffsetDateTime.now());
        }

        Map<String, Object> response = (Map<String, Object>) body.get("createTransactionResponse");
        if (response == null) {
            response = body;
        }

        Map<String, Object> messages = (Map<String, Object>) response.get("messages");
        String resultCode = messages != null ? (String) messages.get("resultCode") : null;

        Map<String, Object> transactionResponse = (Map<String, Object>) response.get("transactionResponse");
        String transId = transactionResponse != null ? (String) transactionResponse.get("transId") : null;
        String responseCode = transactionResponse != null ? String.valueOf(transactionResponse.get("responseCode")) :
                (messages != null ? collectFirstMessageCode(messages) : "0");
        String responseMessage = transactionResponse != null && transactionResponse.get("messages") instanceof Iterable<?> iterable
                ? iterable.iterator().hasNext() ? String.valueOf(((Map<?, ?>) iterable.iterator().next()).get("description")) : null
                : collectFirstMessageText(messages);

        if ("Ok".equalsIgnoreCase(resultCode) && transId != null) {
            return GatewayTransactionResult.success(transId, responseCode, responseMessage, OffsetDateTime.now());
        }

        if (transactionResponse != null && transactionResponse.get("errors") instanceof Iterable<?> errors) {
            var iterator = errors.iterator();
            if (iterator.hasNext()) {
                Map<?, ?> err = (Map<?, ?>) iterator.next();
                responseCode = String.valueOf(err.get("errorCode"));
                responseMessage = String.valueOf(err.get("errorText"));
            }
        }

        if (responseMessage == null) {
            responseMessage = "Authorize.Net REST call failed";
        }

        return GatewayTransactionResult.failure(responseCode, responseMessage, OffsetDateTime.now());
    }

    private String collectFirstMessageCode(Map<String, Object> messages) {
        Object messageObj = messages.get("message");
        if (messageObj instanceof Iterable<?> iterable && iterable.iterator().hasNext()) {
            return String.valueOf(((Map<?, ?>) iterable.iterator().next()).get("code"));
        }
        return "0";
    }

    private String collectFirstMessageText(Map<String, Object> messages) {
        Object messageObj = messages.get("message");
        if (messageObj instanceof Iterable<?> iterable && iterable.iterator().hasNext()) {
            return String.valueOf(((Map<?, ?>) iterable.iterator().next()).get("text"));
        }
        return null;
    }

    private Map<String, Object> buildPayload(String type, Money amount, String paymentData, String referenceId,
            String refTransId, String lastFour) {
        Map<String, Object> request = new LinkedHashMap<>();
        Map<String, Object> merchantAuth = new LinkedHashMap<>();
        merchantAuth.put("name", properties.getApiLoginId());
        merchantAuth.put("transactionKey", properties.getTransactionKey());
        request.put("merchantAuthentication", merchantAuth);
        request.put("refId", AuthorizeNetReferenceIdSanitizer.resolve(referenceId));

        Map<String, Object> transactionRequest = new LinkedHashMap<>();
        transactionRequest.put("transactionType", type);
        transactionRequest.put("amount", new BigDecimal(amount.amount().toString()));

        if (paymentData != null || lastFour != null) {
            transactionRequest.put("payment", buildPayment(type, paymentData, lastFour));
        }

        if (refTransId != null) {
            transactionRequest.put("refTransId", refTransId);
        }

        request.put("transactionRequest", transactionRequest);
        return request;
    }

    private Map<String, Object> buildPayment(String type, String paymentData, String lastFour) {
        if (lastFour != null) {
            Map<String, Object> creditCard = new LinkedHashMap<>();
            creditCard.put("cardNumber", lastFour);
            creditCard.put("expirationDate", "XXXX");
            Map<String, Object> payment = new LinkedHashMap<>();
            payment.put("creditCard", creditCard);
            return payment;
        }

        if (paymentData != null && paymentData.startsWith("card|")) {
            String[] parts = paymentData.split("\\|");
            String number = parts.length > 1 ? parts[1] : "";
            String expiry = parts.length > 2 ? parts[2].replaceAll("[^0-9]", "") : "";
            String cvv = parts.length > 3 ? parts[3] : null;

            Map<String, Object> creditCard = new LinkedHashMap<>();
            creditCard.put("cardNumber", number);
            creditCard.put("expirationDate", normaliseExpiry(expiry));
            if (cvv != null) {
                creditCard.put("cardCode", cvv);
            }
            Map<String, Object> payment = new LinkedHashMap<>();
            payment.put("creditCard", creditCard);
            return payment;
        }

        if (paymentData != null) {
            Map<String, Object> opaqueData = new LinkedHashMap<>();
            opaqueData.put("dataDescriptor", properties.getAcceptPaymentDescriptor());
            opaqueData.put("dataValue", paymentData);
            Map<String, Object> payment = new LinkedHashMap<>();
            payment.put("opaqueData", opaqueData);
            return payment;
        }

        throw new IllegalArgumentException("Payment data required for transaction type " + type);
    }

    private String normaliseExpiry(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 4) {
            return digits.substring(0, 2) + digits.substring(2);
        }
        if (digits.length() == 6) {
            return digits.substring(4) + digits.substring(2, 4);
        }
        if (digits.length() == 2) {
            return digits + "00";
        }
        return "2030";
    }
}
