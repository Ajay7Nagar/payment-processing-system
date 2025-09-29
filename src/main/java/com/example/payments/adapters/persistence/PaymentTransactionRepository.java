package com.example.payments.adapters.persistence;

import com.example.payments.domain.payments.PaymentTransaction;
import com.example.payments.domain.payments.PaymentTransactionType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByOrderId(UUID orderId);

    Optional<PaymentTransaction> findFirstByOrderIdAndTypeOrderByProcessedAtDesc(UUID orderId, PaymentTransactionType type);
}
