package com.acme.payments.bootapi.webhooks;

import com.acme.payments.bootapi.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class DefaultWebhookService implements WebhookService {
    private final String currentSecret;
    private final String previousSecret;
    private final long maxSkewSeconds;

    public DefaultWebhookService(
            @Value("${webhook.secret.current:}") String currentSecret,
            @Value("${webhook.secret.previous:}") String previousSecret,
            @Value("${webhook.clockSkewMaxSeconds:300}") long maxSkewSeconds) {
        this.currentSecret = currentSecret;
        this.previousSecret = previousSecret;
        this.maxSkewSeconds = maxSkewSeconds;
    }

    @Override
    public void verify(String payload, String signatureHex, long epochSeconds) {
        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - epochSeconds) > maxSkewSeconds) {
            throw new ApiException("INVALID_REQUEST", "Clock skew too large", HttpStatus.BAD_REQUEST);
        }
        if (signatureHex == null || signatureHex.isBlank()) {
            throw new ApiException("WEBHOOK_SIGNATURE_INVALID", "Missing signature", HttpStatus.UNAUTHORIZED);
        }
        String s1 = hmacHex(payload, currentSecret);
        String s2 = previousSecret == null ? null : hmacHex(payload, previousSecret);
        if (!signatureHex.equalsIgnoreCase(s1) && (s2 == null || !signatureHex.equalsIgnoreCase(s2))) {
            throw new ApiException("WEBHOOK_SIGNATURE_INVALID", "Signature mismatch", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public boolean isDuplicate(String vendorEventId, String signatureHash) {
        // placeholder for repo-backed check; return false for now (will be wired in worker processing)
        return false;
    }

    // Update refund status to COMPLETED based on gateway_ref from webhook
    public void handleRefundCompleted(String gatewayRef) {
        try {
            var ctx = org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
            if (ctx == null) return;
            var repo = ctx.getBean(com.acme.payments.adapters.out.db.repo.RefundRepository.class);
            var refund = repo.findFirstByGatewayRef(gatewayRef);
            if (refund != null) {
                java.lang.reflect.Field f = com.acme.payments.adapters.out.db.entity.RefundEntity.class.getDeclaredField("status");
                f.setAccessible(true);
                f.set(refund, "COMPLETED");
                repo.save(refund);
            }
        } catch (Exception ignored) {}
    }

    private static String hmacHex(String payload, String secret) {
        if (secret == null || secret.isBlank()) return "";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
