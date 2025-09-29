package com.example.payments.application.services;

import com.example.payments.adapters.persistence.AuditLogRepository;
import com.example.payments.domain.payments.PaymentOrder;
import com.example.payments.domain.payments.PaymentTransaction;
import com.example.payments.domain.shared.AuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PaymentAuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public PaymentAuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public void recordPurchase(PaymentOrder order, PaymentTransaction transaction) {
        record("system", "PURCHASE", "payment_order", order.getId(), auditMap(order, transaction));
    }

    public void recordAuthorization(PaymentOrder order, PaymentTransaction transaction) {
        record("system", "AUTHORIZE", "payment_order", order.getId(), auditMap(order, transaction));
    }

    public void recordCapture(PaymentOrder order, PaymentTransaction transaction, UUID actorId) {
        record(actorId.toString(), "CAPTURE", "payment_order", order.getId(), auditMap(order, transaction));
    }

    public void recordCancel(PaymentOrder order, PaymentTransaction transaction, UUID actorId) {
        record(actorId.toString(), "CANCEL", "payment_order", order.getId(), auditMap(order, transaction));
    }

    public void recordRefund(PaymentOrder order, PaymentTransaction transaction, UUID actorId) {
        record(actorId.toString(), "REFUND", "payment_order", order.getId(), auditMap(order, transaction));
    }

    private void record(String actor, String operation, String resourceType, UUID resourceId,
            Map<String, Object> metadata) {
        try {
            AuditLog log = AuditLog.record(actor, operation, resourceType, resourceId,
                    objectMapper.writeValueAsString(metadata), java.time.OffsetDateTime.now());
            auditLogRepository.save(log);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit metadata", e);
        }
    }

    private Map<String, Object> auditMap(PaymentOrder order, PaymentTransaction transaction) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", order.getId());
        map.put("status", order.getStatus());
        map.put("transactionId", transaction.getAuthorizeNetTransactionId());
        map.put("type", transaction.getType());
        map.put("amount", transaction.getMoney().amount());
        map.put("currency", transaction.getMoney().currency());
        return map;
    }
}
