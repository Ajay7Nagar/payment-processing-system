package com.acme.payments.adapters.out.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.queue.type", havingValue = "noop", matchIfMissing = true)
public class NoopEventQueue implements EventQueue {
    private static final Logger log = LoggerFactory.getLogger(NoopEventQueue.class);

    @Override
    public void enqueue(String topic, String payload) {
        log.info("enqueued topic={} bytes={}", topic, payload == null ? 0 : payload.length());
    }
}


