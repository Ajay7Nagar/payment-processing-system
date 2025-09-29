package com.acme.payments.bootapi.webhooks;

public interface WebhookService {
    void verify(String payload, String signatureHex, long epochSeconds);
    boolean isDuplicate(String vendorEventId, String signatureHash);
}
