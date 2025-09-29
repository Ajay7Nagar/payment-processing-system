package com.example.payments.adapters.persistence;

import com.example.payments.domain.billing.SubscriptionSchedule;
import com.example.payments.domain.billing.SubscriptionSchedule.ScheduleStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionScheduleRepository extends JpaRepository<SubscriptionSchedule, UUID> {

    List<SubscriptionSchedule> findBySubscriptionIdAndStatus(UUID subscriptionId, ScheduleStatus status);

    List<SubscriptionSchedule> findByStatusAndScheduledAtLessThanEqual(ScheduleStatus status, OffsetDateTime threshold);

    Optional<SubscriptionSchedule> findFirstBySubscriptionIdAndStatusOrderByScheduledAtAsc(UUID subscriptionId,
            ScheduleStatus status);

    List<SubscriptionSchedule> findBySubscriptionIdOrderByScheduledAtAsc(UUID subscriptionId);
}
