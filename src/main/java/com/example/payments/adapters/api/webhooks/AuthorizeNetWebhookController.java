package com.example.payments.adapters.api.webhooks;

import com.example.payments.adapters.api.webhooks.dto.AuthorizeNetWebhookRequest;
import com.example.payments.application.services.WebhookService;
import com.example.payments.domain.shared.CorrelationId;
import com.example.payments.infra.gateway.AuthorizeNetProperties;
import com.example.payments.infra.web.CachedBodyHttpServletRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/authorize-net")
@Validated
public class AuthorizeNetWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeNetWebhookController.class);

    private final WebhookService webhookService;
    private final AuthorizeNetProperties properties;
    private final ObjectMapper objectMapper;

    public AuthorizeNetWebhookController(WebhookService webhookService, AuthorizeNetProperties properties,
            ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestHeader(name = "X-ANET-Signature", required = false) String signature,
            @RequestHeader(name = "X-Correlation-Id", required = false) String correlationIdHeader,
            @Valid @RequestBody AuthorizeNetWebhookRequest request,
            HttpServletRequest servletRequest) {

        String body = CachedBodyHttpServletRequest.extractBody(servletRequest)
                .orElseGet(() -> serializeRequest(request));

        validateSignature(signature, body);

        CorrelationId correlationId = StringUtils.hasText(correlationIdHeader) ? new CorrelationId(correlationIdHeader)
                : CorrelationId.newId();

        webhookService.recordEvent(request.eventId(), request.eventType(), body, signature);

        log.info("Webhook accepted eventId={} eventType={} correlationId={}",
                request.eventId(), request.eventType(), correlationId.value());
        return ResponseEntity.accepted().build();
    }

    private void validateSignature(String signature, String body) {
        if (!StringUtils.hasText(signature)) {
            throw new IllegalArgumentException("Missing signature header");
        }
        if (!signature.startsWith("sha512=")) {
            throw new IllegalArgumentException("Unsupported signature format");
        }
        String provided = signature.substring("sha512=".length());
        try {
            byte[] keyBytes = HexFormat.of().parseHex(properties.getWebhookSignatureKey());
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA512"));
            byte[] computed = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(computed);
            if (!expected.equalsIgnoreCase(provided)) {
                throw new IllegalArgumentException("Invalid webhook signature");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to validate webhook signature", ex);
        }
    }

    private String serializeRequest(AuthorizeNetWebhookRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize webhook request", e);
        }
    }
}
