package com.acme.payments.bootworker;

import com.acme.payments.adapters.out.db.entity.WebhookEventEntity;
import com.acme.payments.adapters.out.db.repo.WebhookEventRepository;
import com.acme.payments.adapters.out.db.repo.RefundRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class WebhookProcessor {
    private static final Logger log = LoggerFactory.getLogger(WebhookProcessor.class);
    private final WebhookEventRepository repo;
    private final RefundRepository refundRepository;
    private final MeterRegistry meterRegistry;
    private final com.acme.payments.adapters.out.db.repo.PaymentIntentRepository paymentIntentRepository;

    public WebhookProcessor(WebhookEventRepository repo, RefundRepository refundRepository, MeterRegistry meterRegistry,
                            com.acme.payments.adapters.out.db.repo.PaymentIntentRepository paymentIntentRepository) {
        this.repo = repo;
        this.refundRepository = refundRepository;
        this.meterRegistry = meterRegistry;
        this.paymentIntentRepository = paymentIntentRepository;
    }

    public boolean fromPayload(String payload, String signatureHash) {
        // very simple extraction for demo: look for id and type fields if present
        String vendorEventId = extract(payload, "id");
        String type = extract(payload, "event");
        if (vendorEventId == null) {
            vendorEventId = java.util.UUID.randomUUID().toString();
        }
        boolean duplicate = dedupeAndPersist(vendorEventId, signatureHash, type == null ? "unknown" : type, vendorEventId);
        if (!duplicate) {
            // If refund completion, update refund status by gateway reference (transaction id)
            if (type != null && (type.contains("refund") || type.contains("REFUND")) && (type.contains("completed") || type.contains("succeeded") || type.contains("COMPLETED"))) {
                String transId = extract(payload, "transId");
                if (transId == null) transId = extract(payload, "refTransId");
                if (transId != null) {
                    var refund = refundRepository.findFirstByGatewayRef(transId);
                    if (refund != null) {
                        try {
                            java.lang.reflect.Field f = com.acme.payments.adapters.out.db.entity.RefundEntity.class.getDeclaredField("status");
                            f.setAccessible(true);
                            f.set(refund, "COMPLETED");
                            refundRepository.save(refund);
                            meterRegistry.counter("refund.completed.total").increment();
                            log.info("refund marked COMPLETED for gatewayRef={}", transId);
                        } catch (Exception e) {
                            log.warn("failed to update refund status for transId={}", transId, e);
                        }
                    }
                }
            }
            // If payment captured/settled, update payment intent status
            if (type != null && (type.contains("captured") || type.contains("settled") || type.contains("CAPTURED") || type.contains("SETTLED"))) {
                String transId = extract(payload, "transId");
                if (transId != null) {
                    paymentIntentRepository.findFirstByGatewayRef(transId).ifPresent(intent -> {
                        try {
                            java.lang.reflect.Field f = com.acme.payments.adapters.out.db.entity.PaymentIntentEntity.class.getDeclaredField("status");
                            f.setAccessible(true);
                            f.set(intent, "CAPTURED");
                            paymentIntentRepository.save(intent);
                            log.info("payment intent marked CAPTURED for gatewayRef={}", transId);
                        } catch (Exception e) {
                            log.warn("failed to update payment intent for transId={}", transId, e);
                        }
                    });
                }
            }
        }
        return duplicate;
    }

    public boolean dedupeAndPersist(String vendorEventId, String signatureHash, String type, String id) {
        if (repo.existsByVendorEventIdAndSignatureHash(vendorEventId, signatureHash)) {
            log.info("duplicate webhook suppressed vendorEventId={} sig={} ", vendorEventId, signatureHash);
            return true;
        }
        WebhookEventEntity e = new WebhookEventEntity();
        try {
            java.lang.reflect.Field f1 = WebhookEventEntity.class.getDeclaredField("id");
            f1.setAccessible(true);
            f1.set(e, id);
            java.lang.reflect.Field f2 = WebhookEventEntity.class.getDeclaredField("vendorEventId");
            f2.setAccessible(true);
            f2.set(e, vendorEventId);
            java.lang.reflect.Field f3 = WebhookEventEntity.class.getDeclaredField("signatureHash");
            f3.setAccessible(true);
            f3.set(e, signatureHash);
            java.lang.reflect.Field f4 = WebhookEventEntity.class.getDeclaredField("type");
            f4.setAccessible(true);
            f4.set(e, type);
            java.lang.reflect.Field f5 = WebhookEventEntity.class.getDeclaredField("receivedAt");
            f5.setAccessible(true);
            f5.set(e, Instant.now());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        repo.save(e);
        return false;
    }

    private static String extract(String json, String field) {
        // naive extractor for {"field":"value"}
        String needle = "\"" + field + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int start = json.indexOf('"', i + needle.length());
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }
}


