package com.acme.payments.adapters.out.db.repo;

import com.acme.payments.adapters.out.db.entity.WebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEventEntity, String> {
    boolean existsByVendorEventIdAndSignatureHash(String vendorEventId, String signatureHash);
}
