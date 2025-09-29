package com.example.payments.adapters.api.subscriptions.dto;

import com.example.payments.domain.billing.SubscriptionSchedule;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionScheduleResponse(UUID id, int attemptNumber, SubscriptionSchedule.ScheduleStatus status,
        OffsetDateTime scheduledAt, OffsetDateTime updatedAt, String failureReason) {

    public static SubscriptionScheduleResponse fromDomain(SubscriptionSchedule schedule) {
        return new SubscriptionScheduleResponse(schedule.getId(), schedule.getAttemptNumber(), schedule.getStatus(),
                schedule.getScheduledAt(), schedule.getUpdatedAt(), schedule.getFailureReason());
    }
}
