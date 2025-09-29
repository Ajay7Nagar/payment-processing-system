package com.acme.payments.adapters.out.db.repo;

import com.acme.payments.adapters.out.db.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    Page<OrderEntity> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    @Query("select o from OrderEntity o where o.merchantId = :merchantId and (:status is null or o.status = :status) and (o.createdAt < :createdAt or (o.createdAt = :createdAt and o.id < :id)) order by o.createdAt desc, o.id desc")
    java.util.List<OrderEntity> findSliceBefore(
            @Param("merchantId") String merchantId,
            @Param("status") String status,
            @Param("createdAt") java.time.Instant createdAt,
            @Param("id") String id,
            Pageable pageable);

    Page<OrderEntity> findByMerchantIdAndStatusOrderByCreatedAtDesc(String merchantId, String status, Pageable pageable);
}
