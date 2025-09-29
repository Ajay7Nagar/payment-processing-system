package com.example.payments.adapters.persistence;

import com.example.payments.domain.payments.PaymentOrder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    Optional<PaymentOrder> findByRequestId(String requestId);

    List<PaymentOrder> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}
