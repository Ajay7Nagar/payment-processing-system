package com.acme.payments.bootworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.queue.type", havingValue = "redis")
public class RedisWebhookConsumer {
    private static final Logger log = LoggerFactory.getLogger(RedisWebhookConsumer.class);
    private final StringRedisTemplate redis;
    private final WebhookProcessor processor;

    public RedisWebhookConsumer(StringRedisTemplate redis, WebhookProcessor processor) {
        this.redis = redis;
        this.processor = processor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    String payload = redis.opsForList().rightPop("webhooks.authorize_net");
                    if (payload != null) {
                        processor.fromPayload(payload, "");
                    } else {
                        Thread.sleep(200);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    log.warn("webhook consumer error", e);
                }
            }
        }, "webhook-consumer").start();
    }
}


