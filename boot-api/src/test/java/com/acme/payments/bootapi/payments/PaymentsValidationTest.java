package com.acme.payments.bootapi.payments;

import com.acme.payments.bootapi.config.CorrelationIdFilter;
import com.acme.payments.bootapi.error.ApiException;
import com.acme.payments.bootapi.error.GlobalExceptionHandler;
import com.acme.payments.bootapi.ratelimit.RateLimiter;
import com.acme.payments.bootapi.ratelimit.RateLimitingFilter;
import com.acme.payments.bootapi.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentsController.class)
@Import({CorrelationIdFilter.class, GlobalExceptionHandler.class, RateLimitingFilter.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class PaymentsValidationTest {

    @Autowired MockMvc mvc;
    @org.springframework.boot.test.mock.mockito.MockBean RateLimiter rateLimiter;
    @org.springframework.boot.test.mock.mockito.MockBean PaymentsService paymentsService;
    @org.springframework.boot.test.mock.mockito.MockBean com.acme.payments.bootapi.idempotency.IdempotencyService idempotencyService;

    @org.junit.jupiter.api.BeforeEach
    void allowRateLimiter() {
        org.mockito.Mockito.when(rateLimiter.tryConsume(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
    }

    @Test
    void rejects_non_inr_currency() throws Exception {
        org.mockito.Mockito.doThrow(ApiException.unprocessable("CURRENCY_NOT_SUPPORTED", "Only INR"))
                .when(paymentsService).validatePurchase(org.mockito.ArgumentMatchers.any());
        String body = "{\"orderId\":\"o1\",\"amount\":{\"amount\":\"10.00\",\"currency\":\"USD\"},\"paymentToken\":\"tok\"}";
        mvc.perform(post("/v1/payments/purchase").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejects_amount_below_min() throws Exception {
        org.mockito.Mockito.doThrow(ApiException.unprocessable("AMOUNT_OUT_OF_RANGE", "low"))
                .when(paymentsService).validatePurchase(org.mockito.ArgumentMatchers.any());
        String body = "{\"orderId\":\"o1\",\"amount\":{\"amount\":\"0.49\",\"currency\":\"INR\"},\"paymentToken\":\"tok\"}";
        mvc.perform(post("/v1/payments/purchase").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejects_amount_above_max() throws Exception {
        org.mockito.Mockito.doThrow(ApiException.unprocessable("AMOUNT_OUT_OF_RANGE", "high"))
                .when(paymentsService).validatePurchase(org.mockito.ArgumentMatchers.any());
        String body = "{\"orderId\":\"o1\",\"amount\":{\"amount\":\"1000000.01\",\"currency\":\"INR\"},\"paymentToken\":\"tok\"}";
        mvc.perform(post("/v1/payments/purchase").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }
}
