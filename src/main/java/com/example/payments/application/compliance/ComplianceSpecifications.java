package com.example.payments.application.compliance;

import com.example.payments.domain.shared.AuditLog;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ComplianceSpecifications {

    private ComplianceSpecifications() {
    }

    public static Specification<AuditLog> withinDateRange(Optional<OffsetDateTime> start, Optional<OffsetDateTime> end) {
        return (root, query, builder) -> {
            if (start.isEmpty() && end.isEmpty()) {
                return builder.conjunction();
            }
            if (start.isPresent() && end.isPresent()) {
                return builder.between(root.get("createdAt"), start.get(), end.get());
            }
            if (start.isPresent()) {
                return builder.greaterThanOrEqualTo(root.get("createdAt"), start.get());
            }
            return builder.lessThanOrEqualTo(root.get("createdAt"), end.get());
        };
    }

    public static Specification<AuditLog> hasActor(Optional<String> actor) {
        return (root, query, builder) -> actor.map(value -> builder.equal(root.get("actor"), value))
                .orElseGet(builder::conjunction);
    }

    public static Specification<AuditLog> hasOperation(Optional<String> operation) {
        return (root, query, builder) -> operation.map(value -> builder.equal(root.get("operation"), value))
                .orElseGet(builder::conjunction);
    }

    public static Specification<AuditLog> hasResourceType(Optional<String> resourceType) {
        return (root, query, builder) -> resourceType.map(value -> builder.equal(root.get("resourceType"), value))
                .orElseGet(builder::conjunction);
    }

    public static Specification<AuditLog> hasResourceId(Optional<UUID> resourceId) {
        return (root, query, builder) -> resourceId.map(value -> builder.equal(root.get("resourceId"), value))
                .orElseGet(builder::conjunction);
    }
}
