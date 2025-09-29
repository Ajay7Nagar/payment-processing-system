package com.example.payments.application.workers;

import com.example.payments.adapters.messaging.WebhookEventPublisher;
import com.example.payments.adapters.persistence.WebhookEventRepository;
import com.example.payments.domain.webhook.WebhookEvent.ProcessedStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebhookQueueScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookQueueScheduler.class);

    private final WebhookEventRepository repository;
    private final WebhookEventPublisher publisher;
    private final Clock clock;

    public WebhookQueueScheduler(WebhookEventRepository repository, WebhookEventPublisher publisher, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${webhooks.queue.requeue-delay-millis:10000}")
    public void requeueStaleEvents() {
        OffsetDateTime threshold = OffsetDateTime.now(clock).minusMinutes(5);
        repository.findByProcessedStatusAndProcessedAtBefore(ProcessedStatus.PROCESSING, threshold)
                .forEach(event -> {
                    log.warn("Re-queuing stale webhook event {}", event.getId());
                    publisher.publish(event);
                });
    }
}
