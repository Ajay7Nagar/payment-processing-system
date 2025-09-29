package com.acme.payments.bootapi.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "ratelimit.backend", havingValue = "redis")
public class RedisRateLimiter implements RateLimiter {
    private final StringRedisTemplate redis;
    private final int limitPerSecond;

    public RedisRateLimiter(StringRedisTemplate redis,
                            @Value("${ratelimit.public.rps:20}") int limitPerSecond) {
        this.redis = redis;
        this.limitPerSecond = limitPerSecond;
    }

    @Override
    public boolean tryConsume(String key) {
        long second = Instant.now().getEpochSecond();
        String k = "rl:" + key + ":" + second;
        Long count = redis.opsForValue().increment(k);
        if (count != null && count == 1L) {
            redis.expire(k, 2, TimeUnit.SECONDS);
        }
        return count != null && count <= limitPerSecond;
    }
}


