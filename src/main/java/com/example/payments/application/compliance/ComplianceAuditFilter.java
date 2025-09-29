package com.example.payments.application.compliance;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public class ComplianceAuditFilter {

    private final OffsetDateTime start;
    private final OffsetDateTime end;
    private final String actor;
    private final String operation;
    private final String resourceType;
    private final UUID resourceId;

    public ComplianceAuditFilter(OffsetDateTime start, OffsetDateTime end, String actor, String operation,
            String resourceType, UUID resourceId) {
        this.start = start;
        this.end = end;
        this.actor = actor;
        this.operation = operation;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public Optional<OffsetDateTime> start() {
        return Optional.ofNullable(start);
    }

    public Optional<OffsetDateTime> end() {
        return Optional.ofNullable(end);
    }

    public Optional<String> actor() {
        return Optional.ofNullable(actor);
    }

    public Optional<String> operation() {
        return Optional.ofNullable(operation);
    }

    public Optional<String> resourceType() {
        return Optional.ofNullable(resourceType);
    }

    public Optional<UUID> resourceId() {
        return Optional.ofNullable(resourceId);
    }
}
