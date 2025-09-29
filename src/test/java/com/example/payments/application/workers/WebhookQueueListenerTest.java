package com.example.payments.application.workers;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.example.payments.application.services.WebhookService;
import com.example.payments.domain.webhook.WebhookEvent;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WebhookQueueListenerTest {

    @Mock
    private WebhookService webhookService;

    private WebhookQueueListener listener;

    private WebhookEvent event;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new WebhookQueueListener(webhookService);
        event = WebhookEvent.create("evt", "PAYMENT_SETTLED", "{}", "sig", "hash", OffsetDateTime.now());
    }

    @Test
    void onMessage_shouldProcessEvent() {
        UUID id = event.getId();
        doReturn(Optional.of(event)).when(webhookService).getEvent(id);
        doNothing().when(webhookService).markProcessing(event);
        doNothing().when(webhookService).markCompleted(event);

        listener.onMessage(id.toString());

        verify(webhookService).markProcessing(event);
        verify(webhookService).markCompleted(event);
    }
}
