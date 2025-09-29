package com.acme.payments.bootapi.ratelimit;

import com.acme.payments.bootapi.config.CorrelationIdFilter;
import com.acme.payments.bootapi.error.GlobalExceptionHandler;
import com.acme.payments.bootapi.payments.PaymentsController;
import com.acme.payments.bootapi.payments.PaymentsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentsController.class)
@Import({RateLimitingFilter.class, CorrelationIdFilter.class, GlobalExceptionHandler.class, com.acme.payments.bootapi.security.SecurityConfig.class})
@org.springframework.test.context.TestPropertySource(properties = {"app.security.enabled=false"})
class RateLimitingFilterTest {

	@Autowired MockMvc mvc;

	@MockBean PaymentsService paymentsService;
	@MockBean RateLimiter rateLimiter;
	@org.springframework.boot.test.mock.mockito.MockBean com.acme.payments.bootapi.idempotency.IdempotencyService idempotencyService;

	@Test
	void returns_429_when_rate_limited() throws Exception {
		when(rateLimiter.tryConsume("127.0.0.1")).thenReturn(false);
		String body = "{\"orderId\":\"o1\",\"amount\":{\"amount\":\"10.00\",\"currency\":\"INR\"},\"paymentToken\":\"tok\"}";
		mvc.perform(post("/v1/payments/purchase").contentType(MediaType.APPLICATION_JSON).content(body)
					.with(req -> { req.setRemoteAddr("127.0.0.1"); return req; }))
				.andExpect(status().isTooManyRequests());
	}
}
