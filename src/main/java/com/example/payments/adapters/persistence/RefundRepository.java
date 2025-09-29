package com.example.payments.adapters.persistence;

import com.example.payments.domain.payments.Refund;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByTransactionId(UUID transactionId);
}
