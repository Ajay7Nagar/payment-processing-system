package com.example.payments.adapters.api.compliance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ComplianceAuditQueryRequest(
        OffsetDateTime start,
        OffsetDateTime end,
        String actor,
        String operation,
        String resourceType,
        String resourceId,
        @NotNull @Min(0) Integer page,
        @NotNull @Min(1) @Max(200) Integer size) {
}
