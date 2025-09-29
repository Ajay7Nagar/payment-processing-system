package com.acme.payments.bootapi.webhooks;

import com.acme.payments.bootapi.error.ApiException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultWebhookServiceTest {

    private final DefaultWebhookService service = new DefaultWebhookService("secret", "previous", 300);

    @Test
    void verify_accepts_valid_signature_current_secret() {
        String payload = "{}";
        String signature = hmac("secret", payload);
        long now = System.currentTimeMillis() / 1000L;

        service.verify(payload, signature, now);
    }

    @Test
    void verify_accepts_previous_secret() {
        DefaultWebhookService svc = new DefaultWebhookService("", "prev", 300);
        String payload = "{}";
        String signature = hmac("prev", payload);
        long now = System.currentTimeMillis() / 1000L;

        svc.verify(payload, signature, now);
    }

    @Test
    void verify_rejects_missing_signature() {
        long now = System.currentTimeMillis() / 1000L;
        assertThatThrownBy(() -> service.verify("{}", null, now))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("WEBHOOK_SIGNATURE_INVALID");
    }

    @Test
    void verify_rejects_clock_skew() {
        long farPast = (System.currentTimeMillis() / 1000L) - 9999;
        assertThatThrownBy(() -> service.verify("{}", "sig", farPast))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_REQUEST");
    }

    @Test
    void verify_rejects_mismatch() {
        long now = System.currentTimeMillis() / 1000L;
        assertThatThrownBy(() -> service.verify("{}", "bad", now))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("WEBHOOK_SIGNATURE_INVALID");
    }

    @Test
    void isDuplicate_returns_false_for_placeholder() {
        assertThat(service.isDuplicate("id", "sig")).isFalse();
    }
    private String hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


