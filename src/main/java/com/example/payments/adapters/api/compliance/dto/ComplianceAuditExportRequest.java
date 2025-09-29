package com.example.payments.adapters.api.compliance.dto;

import java.time.OffsetDateTime;

public record ComplianceAuditExportRequest(
        OffsetDateTime start,
        OffsetDateTime end,
        String actor,
        String operation,
        String resourceType,
        String resourceId) {
}
