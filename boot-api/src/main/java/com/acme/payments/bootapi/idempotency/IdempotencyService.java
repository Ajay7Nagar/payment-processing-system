package com.acme.payments.bootapi.idempotency;

public interface IdempotencyService {
    String scopeKey(String merchantId, String endpoint, String key);
    String hashPayload(String payload);
    String findResponse(String scopeKey, String payloadHash);
    void storeResponse(String scopeKey, String payloadHash, String responseJson);
}


