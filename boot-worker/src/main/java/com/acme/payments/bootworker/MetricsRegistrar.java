package com.acme.payments.bootworker;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class MetricsRegistrar {
    public MetricsRegistrar(MeterRegistry registry, StringRedisTemplate redis) {
        Gauge.builder("webhook.queue.depth", () -> redis.opsForList().size("webhooks.authorize_net")).register(registry);
    }
}


