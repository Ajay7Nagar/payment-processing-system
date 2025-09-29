package com.acme.payments.adapters.out.db.repo;

import com.acme.payments.adapters.out.db.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
    @Modifying
    @Transactional
    @Query("delete from OutboxEventEntity e where e.processedAt is not null and e.processedAt < :cutoff")
    void deleteProcessedBefore(@Param("cutoff") Instant cutoff);
}


