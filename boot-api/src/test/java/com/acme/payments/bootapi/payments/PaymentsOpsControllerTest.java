package com.acme.payments.bootapi.payments;

import com.acme.payments.bootapi.config.CorrelationIdFilter;
import com.acme.payments.bootapi.error.GlobalExceptionHandler;
import com.acme.payments.bootapi.ratelimit.RateLimiter;
import com.acme.payments.bootapi.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentsController.class)
@Import({CorrelationIdFilter.class, GlobalExceptionHandler.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class PaymentsOpsControllerTest {

    @Autowired MockMvc mvc;

    @MockBean PaymentsService paymentsService;
    @MockBean PaymentsOpsService opsService;
    @MockBean RateLimiter rateLimiter;
    @org.springframework.boot.test.mock.mockito.MockBean com.acme.payments.bootapi.idempotency.IdempotencyService idempotencyService;

    @org.junit.jupiter.api.BeforeEach
    void allowRateLimiter() {
        when(rateLimiter.tryConsume(anyString())).thenReturn(true);
    }

    @Test
    void authorize_201() throws Exception {
        doNothing().when(opsService).authorize(org.mockito.ArgumentMatchers.any());
        String body = "{\"orderId\":\"o1\",\"amount\":{\"amount\":\"10.00\",\"currency\":\"INR\"},\"paymentToken\":\"tok\"}";
        mvc.perform(post("/v1/payments/authorize").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void capture_200() throws Exception {
        doNothing().when(opsService).capture(org.mockito.ArgumentMatchers.any());
        String body = "{\"authorizationId\":\"pi-o1\",\"amount\":{\"amount\":\"5.00\",\"currency\":\"INR\"}}";
        mvc.perform(post("/v1/payments/capture").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_200() throws Exception {
        doNothing().when(opsService).cancel(org.mockito.ArgumentMatchers.any());
        String body = "{\"authorizationId\":\"pi-o1\"}";
        mvc.perform(post("/v1/payments/cancel").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }
}
