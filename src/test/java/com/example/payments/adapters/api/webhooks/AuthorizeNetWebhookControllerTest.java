package com.example.payments.adapters.api.webhooks;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.payments.adapters.api.webhooks.dto.AuthorizeNetWebhookRequest;
import com.example.payments.application.services.WebhookService;
import com.example.payments.domain.webhook.WebhookEvent;
import com.example.payments.infra.gateway.AuthorizeNetProperties;
import com.example.payments.test.support.SignatureHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

@WebMvcTest(controllers = AuthorizeNetWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthorizeNetWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookService webhookService;

    @MockBean
    private AuthorizeNetProperties properties;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String SIGNATURE_KEY = "00112233445566778899AABBCCDDEEFF";

    @BeforeEach
    void setUp() {
        Mockito.when(properties.getWebhookSignatureKey()).thenReturn(SIGNATURE_KEY);
    }

    @Test
    void receiveWebhook_shouldAcceptValidSignature() throws Exception {
        AuthorizeNetWebhookRequest request = new AuthorizeNetWebhookRequest(
                "evt-1",
                "PAYMENT_AUTHORIZED",
                OffsetDateTime.now(),
                Map.of("amount", 100));

        String body = objectMapper.writeValueAsString(request);
        String signature = "sha512=" + SignatureHelper.hmacSha512Hex(SIGNATURE_KEY, body);

        Mockito.when(webhookService.recordEvent(Mockito.eq("evt-1"), Mockito.eq("PAYMENT_AUTHORIZED"), Mockito.anyString(), Mockito.eq(signature)))
                .thenReturn(WebhookEvent.create("evt-1", "PAYMENT_AUTHORIZED", body, signature, "hash",
                        OffsetDateTime.now()));
        Mockito.clearInvocations(webhookService);

        mockMvc.perform(post("/api/v1/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpRequest -> {
                    httpRequest.setAttribute(com.example.payments.infra.web.CachedBodyHttpServletRequest.ATTRIBUTE_CACHED_BODY, body);
                    return httpRequest;
                })
                .header("X-ANET-Signature", signature))
                .andExpect(status().isAccepted());

        verify(webhookService).recordEvent(Mockito.eq("evt-1"), Mockito.eq("PAYMENT_AUTHORIZED"), Mockito.anyString(), Mockito.eq(signature));
    }

    @Test
    void receiveWebhook_shouldRejectMissingSignature() throws Exception {
        AuthorizeNetWebhookRequest request = new AuthorizeNetWebhookRequest(
                "evt-2",
                "PAYMENT_AUTHORIZED",
                OffsetDateTime.now(),
                Map.of());

        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(webhookService);
    }

    @Test
    void receiveWebhook_shouldRejectInvalidSignature() throws Exception {
        AuthorizeNetWebhookRequest request = new AuthorizeNetWebhookRequest(
                "evt-3",
                "PAYMENT_AUTHORIZED",
                OffsetDateTime.now(),
                Map.of());

        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/webhooks/authorize-net")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(httpRequest -> {
                    httpRequest.setAttribute(com.example.payments.infra.web.CachedBodyHttpServletRequest.ATTRIBUTE_CACHED_BODY, body);
                    return httpRequest;
                })
                .header("X-ANET-Signature", "sha512=deadbeef"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(webhookService);
    }
}
