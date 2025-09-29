package com.example.payments.domain.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    public enum ProcessedStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "signature", nullable = false)
    private String signature;

    @Enumerated(EnumType.STRING)
    @Column(name = "processed_status", nullable = false)
    private ProcessedStatus processedStatus = ProcessedStatus.PENDING;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "dedupe_hash", nullable = false)
    private String dedupeHash;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private long version;

    protected WebhookEvent() {
    }

    private WebhookEvent(UUID id, String eventId, String eventType, String payload, String signature,
            String dedupeHash, OffsetDateTime receivedAt, ProcessedStatus processedStatus, OffsetDateTime processedAt,
            String failureReason, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.signature = signature;
        this.dedupeHash = dedupeHash;
        this.receivedAt = receivedAt;
        this.processedStatus = processedStatus;
        this.processedAt = processedAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WebhookEvent create(String eventId, String eventType, String payload, String signature,
            String dedupeHash, OffsetDateTime receivedAt) {
        OffsetDateTime now = OffsetDateTime.now();
        return new WebhookEvent(UUID.randomUUID(), eventId, eventType, payload, signature, dedupeHash,
                receivedAt, ProcessedStatus.PENDING, null, null, now, now);
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getSignature() {
        return signature;
    }

    public ProcessedStatus getProcessedStatus() {
        return processedStatus;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getDedupeHash() {
        return dedupeHash;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public void markProcessing() {
        processedStatus = ProcessedStatus.PROCESSING;
        processedAt = OffsetDateTime.now();
    }

    public void markCompleted() {
        processedStatus = ProcessedStatus.COMPLETED;
        processedAt = OffsetDateTime.now();
        failureReason = null;
    }

    public void markFailed(String reason) {
        processedStatus = ProcessedStatus.FAILED;
        processedAt = OffsetDateTime.now();
        failureReason = reason;
    }
}
