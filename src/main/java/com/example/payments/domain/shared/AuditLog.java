package com.example.payments.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "actor", nullable = false)
    private String actor;

    @Column(name = "operation", nullable = false)
    private String operation;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AuditLog() {
    }

    public AuditLog(UUID id, String actor, String operation, String resourceType, UUID resourceId,
            String metadata, OffsetDateTime createdAt) {
        this.id = id;
        this.actor = actor;
        this.operation = operation;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public static AuditLog record(String actor, String operation, String resourceType, UUID resourceId,
            String metadata, OffsetDateTime createdAt) {
        return new AuditLog(UUID.randomUUID(), actor, operation, resourceType, resourceId, metadata, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getOperation() {
        return operation;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getMetadata() {
        return metadata;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
