package com.acme.payments.bootapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.acme.payments.bootapi.ratelimit.RateLimiter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = _TestPingController.class)
@AutoConfigureMockMvc(addFilters = false)
class SecurityDisabledTest {

	@Autowired MockMvc mvc;
	@MockBean RateLimiter rateLimiter;

	@Test
	void permits_when_security_disabled() throws Exception {
		mvc.perform(get("/ping"))
				.andExpect(status().isOk());
	}
}


