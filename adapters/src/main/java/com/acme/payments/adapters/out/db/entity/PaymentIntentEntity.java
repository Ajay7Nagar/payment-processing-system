package com.acme.payments.adapters.out.db.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_intents")
public class PaymentIntentEntity {
    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "gateway_ref", length = 128)
    private String gatewayRef;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public PaymentIntentEntity() {}
}
