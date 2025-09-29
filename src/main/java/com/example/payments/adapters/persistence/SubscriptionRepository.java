package com.example.payments.adapters.persistence;

import com.example.payments.domain.billing.Subscription;
import com.example.payments.domain.billing.SubscriptionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByClientReference(String clientReference);

    List<Subscription> findByStatusInAndNextBillingAtLessThanEqual(List<SubscriptionStatus> statuses,
            OffsetDateTime threshold);
}
