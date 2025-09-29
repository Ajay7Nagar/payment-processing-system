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

    public String getId() {
        return id;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getGatewayRef() {
        return gatewayRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setGatewayRef(String gatewayRef) {
        this.gatewayRef = gatewayRef;
    }
}
