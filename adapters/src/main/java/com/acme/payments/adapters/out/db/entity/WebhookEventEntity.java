package com.acme.payments.adapters.out.db.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "webhook_events")
public class WebhookEventEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "vendor_event_id", nullable = false, length = 128)
    private String vendorEventId;

    @Column(name = "signature_hash", nullable = false, length = 128)
    private String signatureHash;

    @Column(nullable = false, length = 64)
    private String type;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    public WebhookEventEntity() {}
}
