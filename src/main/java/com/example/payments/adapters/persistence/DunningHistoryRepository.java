package com.example.payments.adapters.persistence;

import com.example.payments.domain.billing.DunningHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DunningHistoryRepository extends JpaRepository<DunningHistory, UUID> {

    List<DunningHistory> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);
}
