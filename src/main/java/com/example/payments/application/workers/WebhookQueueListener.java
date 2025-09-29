package com.example.payments.application.workers;

import com.example.payments.application.services.WebhookService;
import com.example.payments.domain.webhook.WebhookEvent;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class WebhookQueueListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookQueueListener.class);

    private final WebhookService webhookService;

    public WebhookQueueListener(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @RabbitListener(queues = "webhook.events.queue")
    public void onMessage(String eventId) {
        try {
            Optional<WebhookEvent> eventOpt = webhookService.getEvent(UUID.fromString(eventId));
            if (eventOpt.isEmpty()) {
                log.warn("Received queue message for event {} but no pending record found", eventId);
                return;
            }

            WebhookEvent event = eventOpt.get();
            webhookService.markProcessing(event);
            // TODO: dispatch to handlers
            webhookService.markCompleted(event);
        } catch (Exception ex) {
            log.error("Error handling webhook message {}", eventId, ex);
        }
    }
}
