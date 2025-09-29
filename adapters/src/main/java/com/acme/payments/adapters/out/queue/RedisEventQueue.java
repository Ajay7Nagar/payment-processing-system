package com.acme.payments.adapters.out.queue;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisEventQueue implements EventQueue {
    private final StringRedisTemplate redis;

    public RedisEventQueue(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void enqueue(String topic, String payload) {
        redis.opsForList().leftPush(topic, payload == null ? "" : payload);
    }
}


