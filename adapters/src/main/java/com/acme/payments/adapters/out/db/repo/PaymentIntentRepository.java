package com.acme.payments.adapters.out.db.repo;

import com.acme.payments.adapters.out.db.entity.PaymentIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntentEntity, String> {
    Optional<PaymentIntentEntity> findFirstByGatewayRef(String gatewayRef);
}
