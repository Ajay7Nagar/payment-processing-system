package com.acme.payments.adapters.out.db.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "charges")
public class ChargeEntity {
    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "intent_id")
    private PaymentIntentEntity intent;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "settled_at")
    private Instant settledAt;

    public ChargeEntity() {}

    public String getId() { return id; }
    public PaymentIntentEntity getIntent() { return intent; }
    public long getAmountMinor() { return amountMinor; }
    public String getStatus() { return status; }
    public Instant getSettledAt() { return settledAt; }
}
