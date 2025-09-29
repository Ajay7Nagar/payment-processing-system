package com.acme.payments.adapters.out.db.repo;

import com.acme.payments.adapters.out.db.entity.RefundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<RefundEntity, String> {
    @Query("select coalesce(sum(r.amountMinor),0) from RefundEntity r where r.charge.id = :chargeId")
    long sumAmountMinorByChargeId(@Param("chargeId") String chargeId);

    RefundEntity findFirstByGatewayRef(String gatewayRef);
}
