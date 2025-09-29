package com.acme.payments.adapters.out.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoopEventQueue implements EventQueue {
    private static final Logger log = LoggerFactory.getLogger(NoopEventQueue.class);

    @Override
    public void enqueue(String topic, String payload) {
        log.info("enqueued topic={} bytes={}", topic, payload == null ? 0 : payload.length());
    }
}


