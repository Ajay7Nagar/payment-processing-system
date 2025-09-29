package com.acme.payments.adapters.out.queue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.queue.type", havingValue = "redis")
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


