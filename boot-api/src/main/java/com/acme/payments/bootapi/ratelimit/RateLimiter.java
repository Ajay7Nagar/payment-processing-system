package com.acme.payments.bootapi.ratelimit;

public interface RateLimiter {
    boolean tryConsume(String key);
}
