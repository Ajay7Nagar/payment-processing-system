package com.acme.payments.bootapi.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimiter implements RateLimiter {
    private final int limitPerSecond;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(@Value("${ratelimit.public.rps:20}") int limitPerSecond) {
        this.limitPerSecond = limitPerSecond;
    }

    @Override
    public boolean tryConsume(String key) {
        long second = Instant.now().getEpochSecond();
        Counter c = counters.computeIfAbsent(key, k -> new Counter(second, 0));
        synchronized (c) {
            if (c.epochSecond != second) {
                c.epochSecond = second;
                c.count = 0;
            }
            if (c.count >= limitPerSecond) {
                return false;
            }
            c.count++;
            return true;
        }
    }

    private static final class Counter {
        long epochSecond;
        int count;
        Counter(long s, int c) { this.epochSecond = s; this.count = c; }
    }
}
