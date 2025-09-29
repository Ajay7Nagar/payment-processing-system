package com.example.payments.adapters.api.compliance.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ComplianceAuditResponse(
        UUID id,
        String actor,
        String operation,
        String resourceType,
        UUID resourceId,
        String metadata,
        OffsetDateTime createdAt) {
}
