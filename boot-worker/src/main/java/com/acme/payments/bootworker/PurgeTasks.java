package com.acme.payments.bootworker;

import com.acme.payments.adapters.out.db.repo.IdempotencyRecordRepository;
import com.acme.payments.adapters.out.db.repo.OutboxEventRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class PurgeTasks {
    private final IdempotencyRecordRepository idempotencyRepo;
    private final OutboxEventRepository outboxRepo;

    public PurgeTasks(IdempotencyRecordRepository idempotencyRepo, OutboxEventRepository outboxRepo) {
        this.idempotencyRepo = idempotencyRepo;
        this.outboxRepo = outboxRepo;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeOldIdempotency() {
        idempotencyRepo.deleteExpired(java.time.Instant.now());
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void purgeProcessedOutbox() {
        outboxRepo.deleteProcessedBefore(java.time.Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS));
    }
}


