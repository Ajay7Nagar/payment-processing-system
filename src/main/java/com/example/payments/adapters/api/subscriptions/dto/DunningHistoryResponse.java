package com.example.payments.adapters.api.subscriptions.dto;

import com.example.payments.domain.billing.DunningHistory;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DunningHistoryResponse(UUID id, OffsetDateTime scheduledAt, String status, String failureCode,
        String failureMessage, OffsetDateTime updatedAt) {

    public static DunningHistoryResponse fromDomain(DunningHistory history) {
        return new DunningHistoryResponse(history.getId(), history.getScheduledAt(), history.getStatus(),
                history.getFailureCode(), history.getFailureMessage(), history.getUpdatedAt());
    }
}
