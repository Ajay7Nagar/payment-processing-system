package com.example.payments.application.workers;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.example.payments.adapters.messaging.WebhookEventPublisher;
import com.example.payments.adapters.persistence.WebhookEventRepository;
import com.example.payments.domain.webhook.WebhookEvent;
import com.example.payments.domain.webhook.WebhookEvent.ProcessedStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

class WebhookQueueSchedulerTest {

    @Mock
    private WebhookEventRepository repository;

    @Mock
    private WebhookEventPublisher publisher;

    private Clock clock;

    private WebhookQueueScheduler scheduler;

    private WebhookEvent staleEvent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clock = Clock.fixed(Instant.parse("2025-09-29T15:00:00Z"), ZoneOffset.UTC);
        scheduler = new WebhookQueueScheduler(repository, publisher, clock);
        staleEvent = WebhookEvent.create("evt", "PAYMENT_SETTLED", "{}", "sig", "hash",
                OffsetDateTime.now(clock).minusMinutes(10));
        staleEvent.markProcessing();
    }

    @Test
    void requeueStaleEvents_shouldPublish() {
        OffsetDateTime threshold = OffsetDateTime.now(clock).minusMinutes(5);
        ReflectionTestUtils.setField(staleEvent, "processedAt", OffsetDateTime.now(clock).minusMinutes(6));
        doReturn(List.of(staleEvent)).when(repository)
                .findByProcessedStatusAndProcessedAtBefore(ProcessedStatus.PROCESSING, threshold);

        scheduler.requeueStaleEvents();

        verify(publisher).publish(staleEvent);
    }
}
