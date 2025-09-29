package com.acme.payments.adapters.out.db.repo;

import com.acme.payments.adapters.out.db.entity.ChargeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeRepository extends JpaRepository<ChargeEntity, String> {}
