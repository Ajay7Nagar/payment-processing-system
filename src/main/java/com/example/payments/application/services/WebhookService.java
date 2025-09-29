package com.example.payments.application.services;

import com.example.payments.adapters.messaging.WebhookEventPublisher;
import com.example.payments.adapters.persistence.WebhookEventRepository;
import com.example.payments.domain.webhook.WebhookEvent;
import com.example.payments.domain.webhook.WebhookEvent.ProcessedStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookEventRepository repository;
    private final WebhookEventPublisher publisher;
    private final Clock clock;
    private final Counter webhookReceivedCounter;
    private final Counter webhookDuplicateCounter;
    private final Counter webhookProcessedCounter;
    private final Counter webhookFailedCounter;
    private final ObservationRegistry observationRegistry;

    public WebhookService(WebhookEventRepository repository, WebhookEventPublisher publisher, Clock clock,
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
        this.webhookReceivedCounter = Counter.builder("webhooks.received.count")
                .description("Authorize.Net webhook payloads received")
                .register(meterRegistry);
        this.webhookDuplicateCounter = Counter.builder("webhooks.duplicate.count")
                .description("Webhook events ignored due to duplicate eventId")
                .register(meterRegistry);
        this.webhookProcessedCounter = Counter.builder("webhooks.processed.count")
                .description("Webhook events processed successfully")
                .register(meterRegistry);
        this.webhookFailedCounter = Counter.builder("webhooks.failed.count")
                .description("Webhook events that failed processing")
                .register(meterRegistry);
        this.observationRegistry = observationRegistry;
    }

    @Transactional
    public WebhookEvent recordEvent(String eventId, String eventType, String payload, String signature) {
        webhookReceivedCounter.increment();
        Optional<WebhookEvent> existing = repository.findByEventId(eventId);
        if (existing.isPresent()) {
            webhookDuplicateCounter.increment();
            log.info("Webhook event already exists eventId={}", eventId);
            return existing.get();
        }
        String dedupeHash = java.util.Base64.getEncoder().encodeToString(payload.getBytes());
        OffsetDateTime receivedAt = OffsetDateTime.now(clock);
        return Observation.createNotStarted("webhooks.persist", observationRegistry)
                .lowCardinalityKeyValue("webhook.event.type", eventType)
                .observe(() -> {
                    WebhookEvent event = WebhookEvent.create(eventId, eventType, payload, signature, dedupeHash,
                            receivedAt);
                    WebhookEvent saved = repository.save(event);
                    publisher.publish(saved);
                    log.info("Webhook event recorded and enqueued eventId={}", eventId);
                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public Optional<WebhookEvent> fetchNextPending() {
        return repository.findFirstByProcessedStatusOrderByReceivedAtAsc(ProcessedStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countPending(OffsetDateTime threshold) {
        return repository.countByProcessedStatusAndReceivedAtBefore(ProcessedStatus.PENDING, threshold);
    }

    @Transactional(readOnly = true)
    public Optional<WebhookEvent> getEvent(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public void markProcessing(WebhookEvent event) {
        event.markProcessing();
        repository.save(event);
    }

    @Transactional
    public void markCompleted(WebhookEvent event) {
        event.markCompleted();
        repository.save(event);
        webhookProcessedCounter.increment();
    }

    @Transactional
    public void markFailed(WebhookEvent event, String reason) {
        event.markFailed(reason);
        repository.save(event);
        webhookFailedCounter.increment();
    }
}
