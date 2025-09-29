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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentsController.class)
@Import({CorrelationIdFilter.class, GlobalExceptionHandler.class, com.acme.payments.bootapi.security.SecurityConfig.class})
@org.springframework.test.context.TestPropertySource(properties = {"app.security.enabled=false"})
class PaymentsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PaymentsService paymentsService;
    @MockBean
    RateLimiter rateLimiter;
    @MockBean
    PaymentsOpsService opsService;
    @MockBean
    com.acme.payments.bootapi.idempotency.IdempotencyService idempotencyService;

    @org.junit.jupiter.api.BeforeEach
    void allowRateLimiter() {
        when(rateLimiter.tryConsume(anyString())).thenReturn(true);
    }

    @Test
    void purchase_returns201_and_sets_correlation_header() throws Exception {
        String body = "{\"orderId\":\"o1\",\"amount\":{\"amount\":\"10.00\",\"currency\":\"INR\"},\"paymentToken\":\"tok\"}";
        mvc.perform(post("/v1/payments/purchase").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void purchase_validation_error_returns_422() throws Exception {
        String body = "{\"orderId\":\"\",\"amount\":{\"amount\":\"10\",\"currency\":\"INR\"},\"paymentToken\":\"\"}";
        mvc.perform(post("/v1/payments/purchase").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }
}
