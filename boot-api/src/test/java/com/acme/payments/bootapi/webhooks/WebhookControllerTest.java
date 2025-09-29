package com.acme.payments.bootapi.webhooks;

import com.acme.payments.bootapi.config.CorrelationIdFilter;
import com.acme.payments.bootapi.error.GlobalExceptionHandler;
import com.acme.payments.bootapi.ratelimit.RateLimiter;
import com.acme.payments.bootapi.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebhookController.class)
@Import({DefaultWebhookService.class, CorrelationIdFilter.class, GlobalExceptionHandler.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "webhook.secret.current=secret",
        "webhook.clockSkewMaxSeconds=300"
})
class WebhookControllerTest {

    @Autowired MockMvc mvc;
    @org.springframework.boot.test.mock.mockito.MockBean RateLimiter rateLimiter;
    @org.springframework.boot.test.mock.mockito.MockBean com.acme.payments.adapters.out.queue.EventQueue queue;
    @org.springframework.boot.test.mock.mockito.MockBean io.micrometer.core.instrument.Counter webhookEnqueuedCounter;

    @org.junit.jupiter.api.BeforeEach
    void allowRateLimiter() {
        when(rateLimiter.tryConsume(anyString())).thenReturn(true);
    }

    @Test
    void ok_when_signature_valid_and_skew_within_limit() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";
        long now = System.currentTimeMillis() / 1000L;
        String sig = hmac(payload, "secret");
        mvc.perform(post("/v1/webhooks/authorize-net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("X-Signature", sig)
                        .header("X-Timestamp", now))
                .andExpect(status().isOk());
    }

    @Test
    void unauthorized_on_bad_signature() throws Exception {
        String payload = "{}";
        long now = System.currentTimeMillis() / 1000L;
        mvc.perform(post("/v1/webhooks/authorize-net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("X-Signature", "bad")
                        .header("X-Timestamp", now))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bad_request_on_large_skew() throws Exception {
        String payload = "{}";
        long oldTs = (System.currentTimeMillis() / 1000L) - 10000L;
        String sig = hmac(payload, "secret");
        mvc.perform(post("/v1/webhooks/authorize-net")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("X-Signature", sig)
                        .header("X-Timestamp", oldTs))
                .andExpect(status().isBadRequest());
    }

    private static String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
