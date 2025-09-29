package com.example.payments.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "response_payload", nullable = false)
    private String responsePayload;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(UUID id, String idempotencyKey, String requestHash, String responsePayload,
            int statusCode, OffsetDateTime createdAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.responsePayload = responsePayload;
        this.statusCode = statusCode;
        this.createdAt = createdAt;
    }

    public static IdempotencyRecord create(String idempotencyKey, String requestHash,
            String responsePayload, int statusCode, OffsetDateTime createdAt) {
        return new IdempotencyRecord(UUID.randomUUID(), idempotencyKey, requestHash, responsePayload,
                statusCode, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
