package com.acme.payments.bootapi.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    @Test
    void allows_up_to_limit_per_second() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(2);

        assertThat(limiter.tryConsume("ip"))
                .as("first request should pass")
                .isTrue();
        assertThat(limiter.tryConsume("ip"))
                .as("second request should pass")
                .isTrue();
        assertThat(limiter.tryConsume("ip")).isFalse();
    }
}


