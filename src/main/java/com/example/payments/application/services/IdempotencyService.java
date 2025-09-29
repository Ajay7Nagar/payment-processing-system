package com.example.payments.application.services;

import com.example.payments.adapters.persistence.IdempotencyRecordRepository;
import com.example.payments.domain.shared.IdempotencyKey;
import com.example.payments.domain.shared.IdempotencyRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findExisting(IdempotencyKey key) {
        return repository.findByIdempotencyKey(key.value());
    }

    @Transactional
    public IdempotencyRecord persist(IdempotencyKey key, String requestPayload, String responsePayload,
            int statusCode) {
        String hash = hashPayload(requestPayload);
        IdempotencyRecord record = IdempotencyRecord.create(key.value(), hash, responsePayload, statusCode,
                java.time.OffsetDateTime.now());
        return repository.save(record);
    }

    private String hashPayload(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
