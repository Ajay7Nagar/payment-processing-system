package com.acme.payments.adapters.out.db.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecordEntity {
    @Id
    @Column(name = "scope_key", length = 200)
    private String scopeKey;

    @Column(name = "request_hash", nullable = false, length = 128)
    private String requestHash;

    @Lob
    @Column(name = "response_snapshot")
    private String responseSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    public IdempotencyRecordEntity() {}
}
