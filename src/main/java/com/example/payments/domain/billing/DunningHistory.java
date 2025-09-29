package com.example.payments.domain.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dunning_attempts")
public class DunningHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_message")
    private String failureMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected DunningHistory() {
    }

    public DunningHistory(UUID id, Subscription subscription, OffsetDateTime scheduledAt, String status,
            String failureCode, String failureMessage, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.subscription = subscription;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DunningHistory record(Subscription subscription, OffsetDateTime scheduledAt, String status,
            String failureCode, String failureMessage, OffsetDateTime now) {
        return new DunningHistory(UUID.randomUUID(), subscription, scheduledAt, status, failureCode, failureMessage,
                now, now);
    }

    public UUID getId() {
        return id;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public String getStatus() {
        return status;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markCompleted(OffsetDateTime now) {
        status = "COMPLETED";
        updatedAt = now;
    }

    public void markFailed(String code, String message, OffsetDateTime now) {
        status = "FAILED";
        failureCode = code;
        failureMessage = message;
        updatedAt = now;
    }
}
