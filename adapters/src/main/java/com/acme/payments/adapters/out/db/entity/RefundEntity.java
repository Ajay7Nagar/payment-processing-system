package com.acme.payments.adapters.out.db.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refunds")
public class RefundEntity {
    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "charge_id")
    private ChargeEntity charge;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "merchant_id", nullable = false, length = 64)
    private String merchantId = "unknown";

    @Column(name = "gateway_ref", length = 128)
    private String gatewayRef;

    public RefundEntity() {}
}
