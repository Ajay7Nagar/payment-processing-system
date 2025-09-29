package com.example.payments.application.settlement;

import com.example.payments.adapters.persistence.PaymentOrderRepository;
import com.example.payments.adapters.persistence.PaymentTransactionRepository;
import com.example.payments.adapters.persistence.SettlementExportRepository;
import com.example.payments.domain.payments.PaymentOrder;
import com.example.payments.domain.payments.PaymentTransaction;
import com.example.payments.domain.payments.PaymentTransactionType;
import com.example.payments.domain.settlement.SettlementExport;
import com.example.payments.domain.settlement.SettlementExportFormat;
import com.example.payments.domain.settlement.SettlementExportStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementExportService {

    private static final Logger log = LoggerFactory.getLogger(SettlementExportService.class);

    private final SettlementExportRepository settlementExportRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SettlementExportWriterFactory writerFactory;

    public SettlementExportService(SettlementExportRepository settlementExportRepository,
            PaymentOrderRepository paymentOrderRepository, PaymentTransactionRepository paymentTransactionRepository,
            SettlementExportWriterFactory writerFactory) {
        this.settlementExportRepository = settlementExportRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.writerFactory = writerFactory;
    }

    @Transactional
    public SettlementExport requestExport(SettlementExportFormat format, OffsetDateTime start, OffsetDateTime end,
            Path outputPath) {
        SettlementExport export = SettlementExport.create(format, start, end, outputPath.toString());
        settlementExportRepository.save(export);
        log.info("Settlement export requested id={} format={} start={} end={}", export.getId(), format, start, end);
        return export;
    }

    @Transactional
    public void processPendingExports() {
        List<SettlementExport> pending = settlementExportRepository.findByStatus(SettlementExportStatus.PENDING);
        for (SettlementExport export : pending) {
            processExport(export);
        }
    }

    private void processExport(SettlementExport export) {
        try {
            Path path = Path.of(export.getFilePath());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            List<PaymentOrder> orders = paymentOrderRepository
                    .findByCreatedAtBetween(export.getDateRangeStart(), export.getDateRangeEnd()).stream()
                    .sorted(Comparator.comparing(PaymentOrder::getCreatedAt))
                    .collect(Collectors.toList());
            SettlementExportWriter writer = writerFactory.writerFor(export.getFormat());
            writer.write(path, orders, txnForOrders(orders));
            export.markCompleted(OffsetDateTime.now());
            settlementExportRepository.save(export);
            log.info("Settlement export completed id={} path={}", export.getId(), export.getFilePath());
        } catch (IOException | RuntimeException e) {
            log.error("Settlement export failed id={} error={}", export.getId(), e.getMessage(), e);
            export.markFailed();
            settlementExportRepository.save(export);
        }
    }

    private List<PaymentTransaction> txnForOrders(List<PaymentOrder> orders) {
        return orders.stream()
                .flatMap(order -> paymentTransactionRepository.findByOrderId(order.getId()).stream())
                .filter(tx -> tx.getType() == PaymentTransactionType.CAPTURE
                        || tx.getType() == PaymentTransactionType.PURCHASE)
                .collect(Collectors.toList());
    }
}
