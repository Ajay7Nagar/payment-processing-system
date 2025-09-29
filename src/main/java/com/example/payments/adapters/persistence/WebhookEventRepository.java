package com.example.payments.adapters.persistence;

import com.example.payments.domain.webhook.WebhookEvent;
import com.example.payments.domain.webhook.WebhookEvent.ProcessedStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Optional<WebhookEvent> findByEventId(String eventId);

    Optional<WebhookEvent> findFirstByProcessedStatusOrderByReceivedAtAsc(ProcessedStatus status);

    long countByProcessedStatusAndReceivedAtBefore(ProcessedStatus status, OffsetDateTime threshold);

    List<WebhookEvent> findByProcessedStatusAndProcessedAtBefore(ProcessedStatus status, OffsetDateTime threshold);
}
