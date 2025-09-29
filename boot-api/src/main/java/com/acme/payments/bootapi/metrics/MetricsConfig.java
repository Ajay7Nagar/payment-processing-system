package com.acme.payments.bootapi.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter webhookEnqueuedCounter(MeterRegistry registry) {
        return Counter.builder("webhook.enqueued.total").description("Total webhooks enqueued").register(registry);
    }
}


