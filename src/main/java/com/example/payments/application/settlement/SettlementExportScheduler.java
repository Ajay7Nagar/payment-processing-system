package com.example.payments.application.settlement;

import com.example.payments.domain.settlement.SettlementExportFormat;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SettlementExportScheduler {

    private final SettlementExportService exportService;
    private final Clock clock;

    public SettlementExportScheduler(SettlementExportService exportService, Clock clock) {
        this.exportService = exportService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void scheduleDailyExport() {
        OffsetDateTime yesterdayStart = OffsetDateTime.now(clock).minusDays(1).withHour(0).withMinute(0).withSecond(0)
                .withNano(0);
        OffsetDateTime yesterdayEnd = yesterdayStart.plusDays(1).minusSeconds(1);
        Path output = Path.of("build/exports", "settlement-" + yesterdayStart.toLocalDate() + ".csv");
        exportService.requestExport(SettlementExportFormat.CSV, yesterdayStart, yesterdayEnd, output);
        exportService.processPendingExports();
    }
}
