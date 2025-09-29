package com.example.payments.application.services;

import com.example.payments.adapters.persistence.PaymentOrderRepository;
import com.example.payments.domain.payments.PaymentException;
import com.example.payments.domain.payments.PaymentOrder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentQueryService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final Counter paymentQueryCounter;

    public PaymentQueryService(PaymentOrderRepository paymentOrderRepository, MeterRegistry meterRegistry) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentQueryCounter = meterRegistry.counter("payments.query.count");
    }

    @Transactional(readOnly = true)
    public PaymentOrder getById(UUID orderId) {
        paymentQueryCounter.increment();
        return paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new PaymentException("ORDER_NOT_FOUND", "Payment order not found"));
    }
}
