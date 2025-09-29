package com.example.payments.application.workers;

import com.example.payments.application.services.WebhookService;
import com.example.payments.domain.webhook.WebhookEvent;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(WebhookProcessor.class);

    private final WebhookService webhookService;
    private final Clock clock;

    public WebhookProcessor(WebhookService webhookService, Clock clock) {
        this.webhookService = webhookService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${webhooks.processing.fixed-delay-millis:1000}")
    public void processPending() {
        Optional<WebhookEvent> next = webhookService.fetchNextPending();
        if (next.isEmpty()) {
            return;
        }
        WebhookEvent event = next.get();
        try {
            log.info("Processing webhook event id={} type={} dedupeHash={}", event.getId(),
                    event.getEventType(), event.getDedupeHash());
            // TODO: dispatch to domain-specific handlers (payments, subscriptions, settlements)
            webhookService.markCompleted(event);
        } catch (Exception ex) {
            log.error("Failed processing webhook event id={} error={}", event.getId(), ex.getMessage(), ex);
            webhookService.markFailed(event, ex.getMessage());
        }
    }
}
