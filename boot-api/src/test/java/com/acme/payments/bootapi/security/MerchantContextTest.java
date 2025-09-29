package com.acme.payments.bootapi.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantContextTest {

    private final MerchantContext context = new MerchantContext();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returns_merchant_id_from_jwt() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "none"), Map.of("merchant_id", "merchant-9"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(context.getMerchantId()).isEqualTo("merchant-9");
    }

    @Test
    void falls_back_when_no_authentication() {
        SecurityContextHolder.clearContext();
        assertThat(context.getMerchantId()).isEqualTo("test-merchant");
    }

    @Test
    void falls_back_for_non_jwt_authentication() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "pass"));
        assertThat(context.getMerchantId()).isEqualTo("test-merchant");
    }
}


