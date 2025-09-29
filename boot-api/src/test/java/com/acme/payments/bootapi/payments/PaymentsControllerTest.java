package com.acme.payments.bootapi.payments;

import com.acme.payments.bootapi.idempotency.IdempotencyService;
import com.acme.payments.bootapi.security.MerchantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentsControllerTest {

    private PaymentsService paymentsService;
    private IdempotencyService idempotencyService;
    private MerchantContext merchantContext;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        paymentsService = mock(PaymentsService.class);
        idempotencyService = mock(IdempotencyService.class);
        merchantContext = mock(MerchantContext.class);
        when(merchantContext.getMerchantId()).thenReturn("merchant-1");

        PaymentsController controller = new PaymentsController(
                paymentsService,
                idempotencyService,
                new ObjectMapper(),
                java.util.Optional.of(merchantContext),
                java.util.Optional.of(new SimpleMeterRegistry())
        );

        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void purchase_without_idempotency_creates_payment() throws Exception {
        String body = "{" +
                "\"orderId\":\"order-1\"," +
                "\"amount\":{\"amount\":\"10.00\",\"currency\":\"INR\"}," +
                "\"paymentToken\":\"tok\"}";

        mvc.perform(post("/v1/payments/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order-1"))
                .andExpect(jsonPath("$.status").value("CAPTURED"));

        verify(paymentsService).validatePurchase(any());
        verify(paymentsService).createOrderIfAbsent(any());
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void purchase_with_idempotency_returns_cached_response() throws Exception {
        when(idempotencyService.scopeKey(eq("merchant-1"), eq("/v1/payments/purchase"), eq("idem")))
                .thenReturn("scope");
        when(idempotencyService.hashPayload(any())).thenReturn("hash");
        when(idempotencyService.findResponse("scope", "hash"))
                .thenReturn("{\"id\":\"cached\"}");

        mvc.perform(post("/v1/payments/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", "idem")
                        .content("{" +
                                "\"orderId\":\"order-1\"," +
                                "\"amount\":{\"amount\":\"10.00\",\"currency\":\"INR\"}," +
                                "\"paymentToken\":\"tok\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("cached"));

        verify(idempotencyService, never()).storeResponse(any(), any(), any());
        verifyNoInteractions(paymentsService);
    }

    @Test
    void purchase_with_idempotency_persists_when_not_cached() throws Exception {
        when(idempotencyService.scopeKey(any(), any(), any())).thenReturn("scope");
        when(idempotencyService.hashPayload(any())).thenReturn("hash");
        when(idempotencyService.findResponse("scope", "hash")).thenReturn(null);

        mvc.perform(post("/v1/payments/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", "key")
                        .content("{" +
                                "\"orderId\":\"order-2\"," +
                                "\"amount\":{\"amount\":\"12.00\",\"currency\":\"INR\"}," +
                                "\"paymentToken\":\"tok\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("order-2"));

        verify(idempotencyService).storeResponse(eq("scope"), eq("hash"), any());
        verify(paymentsService).validatePurchase(any());
        verify(paymentsService).createOrderIfAbsent(any());
    }
}


