package com.acme.payments.bootapi.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    void tryConsume_returns_true_when_under_limit() {
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(1L);

        RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate, 2);

        assertThat(limiter.tryConsume("client"))
                .isTrue();
        verify(redisTemplate).expire(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.eq(java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void tryConsume_returns_false_when_over_limit() {
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.increment(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(3L);

        RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate, 2);

        assertThat(limiter.tryConsume("client"))
                .isFalse();
    }
}


