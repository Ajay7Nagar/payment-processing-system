package com.example.payments.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.payments.adapters.persistence.AuditLogRepository;
import com.example.payments.domain.payments.Money;
import com.example.payments.domain.payments.PaymentOrder;
import com.example.payments.domain.payments.PaymentTransaction;
import com.example.payments.domain.payments.PaymentTransactionType;
import com.example.payments.domain.shared.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentAuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentAuditService paymentAuditService;

    @Captor
    private ArgumentCaptor<com.example.payments.domain.shared.AuditLog> auditLogCaptor;

    private PaymentOrder order;
    private PaymentTransaction transaction;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        paymentAuditService = new PaymentAuditService(auditLogRepository, objectMapper);
        order = PaymentOrder.create(UUID.randomUUID(), new Money(new BigDecimal("100.00"), "USD"),
                CorrelationId.newId(), "req123", "idem-key", OffsetDateTime.now());
        transaction = PaymentTransaction.record(order, PaymentTransactionType.PURCHASE,
                new Money(new BigDecimal("100.00"), "USD"), "tx123", "SETTLED", OffsetDateTime.now(), "1",
                "Approved");
    }

    @Test
    void recordPurchase_shouldPersistAuditLog() {
        paymentAuditService.recordPurchase(order, transaction);

        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getOperation()).isEqualTo("PURCHASE");
        assertThat(auditLogCaptor.getValue().getMetadata()).contains("orderId").doesNotContain("ipAddress");
    }
}
