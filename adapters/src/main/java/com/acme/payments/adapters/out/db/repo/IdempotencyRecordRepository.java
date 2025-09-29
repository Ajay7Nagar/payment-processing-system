package com.acme.payments.adapters.out.db.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.acme.payments.adapters.out.db.entity.IdempotencyRecordEntity;
import org.springframework.transaction.annotation.Transactional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, String> {
    @Query("select r.responseSnapshot from IdempotencyRecordEntity r where r.scopeKey = :k and r.requestHash = :h")
    String findSnapshot(@Param("k") String scopeKey, @Param("h") String requestHash);

    @Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from IdempotencyRecordEntity r where r.expiresAt is not null and r.expiresAt < :now")
    int deleteExpired(@Param("now") java.time.Instant now);
}
