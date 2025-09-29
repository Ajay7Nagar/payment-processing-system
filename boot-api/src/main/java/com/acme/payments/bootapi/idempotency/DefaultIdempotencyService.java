package com.acme.payments.bootapi.idempotency;

import com.acme.payments.adapters.out.db.entity.IdempotencyRecordEntity;
import com.acme.payments.adapters.out.db.repo.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class DefaultIdempotencyService implements IdempotencyService {
    private final IdempotencyRecordRepository repo;

    public DefaultIdempotencyService(IdempotencyRecordRepository repo) {
        this.repo = repo;
    }

    @Override
    public String scopeKey(String merchantId, String endpoint, String key) {
        return merchantId + ":" + endpoint + ":" + key;
    }

    @Override
    public String hashPayload(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String findResponse(String scopeKey, String payloadHash) {
        return repo.findSnapshot(scopeKey, payloadHash);
    }

    @Override
    public void storeResponse(String scopeKey, String payloadHash, String responseJson) {
        IdempotencyRecordEntity e = new IdempotencyRecordEntity();
        try {
            java.lang.reflect.Field f1 = IdempotencyRecordEntity.class.getDeclaredField("scopeKey");
            f1.setAccessible(true);
            f1.set(e, scopeKey);
            java.lang.reflect.Field f2 = IdempotencyRecordEntity.class.getDeclaredField("requestHash");
            f2.setAccessible(true);
            f2.set(e, payloadHash);
            java.lang.reflect.Field f3 = IdempotencyRecordEntity.class.getDeclaredField("responseSnapshot");
            f3.setAccessible(true);
            f3.set(e, responseJson);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        repo.save(e);
    }
}


