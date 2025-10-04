package com.example.payments.domain.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "external_ref", nullable = false, unique = true, length = 255)
    private String externalRef;

    @Column(name = "pii_hash", length = 256)
    private String piiHash;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Customer() {
        // for JPA
    }

    private Customer(String externalRef) {
        this.externalRef = Objects.requireNonNull(externalRef, "externalRef");
    }

    public static Customer forExternalRef(String externalRef) {
        return new Customer(externalRef);
    }

    public UUID getId() {
        return id;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public String getPiiHash() {
        return piiHash;
    }

    public void setPiiHash(String piiHash) {
        this.piiHash = piiHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}

