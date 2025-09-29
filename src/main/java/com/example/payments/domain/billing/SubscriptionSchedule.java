package com.example.payments.domain.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "subscription_schedules")
public class SubscriptionSchedule {

    public enum ScheduleStatus {
        PENDING,
        SUCCESS,
        FAILED,
        SKIPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScheduleStatus status;

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected SubscriptionSchedule() {
    }

    public SubscriptionSchedule(UUID id, Subscription subscription, int attemptNumber, ScheduleStatus status,
            OffsetDateTime scheduledAt, String failureReason, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.subscription = subscription;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SubscriptionSchedule pending(Subscription subscription, int attemptNumber,
            OffsetDateTime scheduledAt, OffsetDateTime now) {
        return new SubscriptionSchedule(UUID.randomUUID(), subscription, attemptNumber, ScheduleStatus.PENDING,
                scheduledAt, null, now, now);
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

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markSuccess(OffsetDateTime now) {
        status = ScheduleStatus.SUCCESS;
        updatedAt = now;
    }

    public void markFailure(String reason, OffsetDateTime now) {
        status = ScheduleStatus.FAILED;
        failureReason = reason;
        updatedAt = now;
    }

    public void markSkipped(OffsetDateTime now) {
        status = ScheduleStatus.SKIPPED;
        updatedAt = now;
    }
}
