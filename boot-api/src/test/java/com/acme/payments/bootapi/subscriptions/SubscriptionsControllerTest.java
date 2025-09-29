package com.acme.payments.bootapi.subscriptions;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubscriptionsController.class)
@Import({CorrelationIdFilter.class, GlobalExceptionHandler.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class SubscriptionsControllerTest {

    @Autowired MockMvc mvc;
    @MockBean SubscriptionsService service;
    @MockBean RateLimiter rateLimiter;

    @org.junit.jupiter.api.BeforeEach
    void allowRateLimiter() {
        when(rateLimiter.tryConsume(anyString())).thenReturn(true);
    }

    @Test
    void create_201() throws Exception {
        String body = "{\"customerId\":\"c1\",\"amount\":{\"amount\":\"10.00\",\"currency\":\"INR\"},\"schedule\":{\"type\":\"MONTHLY\",\"billingDay\":15}}";
        mvc.perform(post("/v1/subscriptions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void cancel_200() throws Exception {
        mvc.perform(post("/v1/subscriptions/s1/cancel"))
                .andExpect(status().isOk());
    }
}


