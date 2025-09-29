package com.acme.payments.bootapi.refunds;

import com.acme.payments.bootapi.config.CorrelationIdFilter;
import com.acme.payments.bootapi.error.GlobalExceptionHandler;
import com.acme.payments.bootapi.ratelimit.RateLimiter;
import com.acme.payments.bootapi.security.SecurityConfig;
import com.acme.payments.bootapi.payments.DefaultRefundsService;
import com.acme.payments.bootapi.payments.RefundsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RefundsController.class)
@Import({CorrelationIdFilter.class, GlobalExceptionHandler.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class RefundsControllerTest {

    @Autowired MockMvc mvc;
    @org.springframework.boot.test.mock.mockito.MockBean RateLimiter rateLimiter;
    @org.springframework.boot.test.mock.mockito.MockBean com.acme.payments.bootapi.payments.RefundsService refundsService;
    @org.springframework.boot.test.mock.mockito.MockBean com.acme.payments.bootapi.idempotency.IdempotencyService idempotencyService;

    @org.junit.jupiter.api.BeforeEach
    void allowRateLimiter() {
        when(rateLimiter.tryConsume(anyString())).thenReturn(true);
    }

    @Test
    void refund_full_refund_created() throws Exception {
        String body = "{\"chargeId\":\"c1\"}";
        org.mockito.Mockito.doNothing().when(refundsService).validateRefundRequest(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doNothing().when(refundsService).createRefund(org.mockito.ArgumentMatchers.any());
        mvc.perform(post("/v1/payments/refund").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void refund_rejects_non_inr() throws Exception {
        org.mockito.Mockito.doThrow(new com.acme.payments.bootapi.error.ApiException("CURRENCY_NOT_SUPPORTED","Only INR", org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY))
                .when(refundsService).validateRefundRequest(org.mockito.ArgumentMatchers.any());
        String body = "{\"chargeId\":\"c1\",\"amount\":{\"amount\":\"10.00\",\"currency\":\"USD\"}}";
        mvc.perform(post("/v1/payments/refund").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }
}
