package com.example.payments.application.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.payments.adapters.persistence.PaymentOrderRepository;
import com.example.payments.adapters.persistence.PaymentTransactionRepository;
import com.example.payments.adapters.persistence.SettlementExportRepository;
import com.example.payments.domain.payments.PaymentOrder;
import com.example.payments.domain.payments.PaymentTransaction;
import com.example.payments.domain.payments.PaymentTransactionType;
import com.example.payments.domain.settlement.SettlementExport;
import com.example.payments.domain.settlement.SettlementExportFormat;
import com.example.payments.domain.settlement.SettlementExportStatus;
import com.example.payments.domain.shared.CorrelationId;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementExportServiceTest {

    @Mock
    private SettlementExportRepository settlementExportRepository;
    @Mock
    private PaymentOrderRepository paymentOrderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private SettlementExportWriterFactory writerFactory;
    @Mock
    private SettlementExportWriter writer;

    @InjectMocks
    private SettlementExportService settlementExportService;

    private OffsetDateTime start;
    private OffsetDateTime end;

    @Captor
    private ArgumentCaptor<SettlementExport> exportCaptor;

    @BeforeEach
    void setUp() {
        start = OffsetDateTime.parse("2025-09-28T00:00:00Z");
        end = OffsetDateTime.parse("2025-09-29T00:00:00Z");
    }

    @Test
    void requestExport_shouldPersistExport() {
        SettlementExport export = settlementExportService.requestExport(SettlementExportFormat.CSV, start, end,
                Path.of("build/exports/test.csv"));

        verify(settlementExportRepository).save(export);
        assertThat(export.getStatus()).isEqualTo(SettlementExportStatus.PENDING);
        assertThat(export.getFormat()).isEqualTo(SettlementExportFormat.CSV);
    }

    @Test
    void processPendingExports_shouldWriteFileAndMarkCompleted() throws Exception {
        SettlementExport export = SettlementExport.create(SettlementExportFormat.CSV, start, end, "build/exports/test.csv");
        PaymentOrder order = PaymentOrder.create(UUID.randomUUID(), new com.example.payments.domain.payments.Money(BigDecimal.TEN, "USD"),
                CorrelationId.newId(), "req", "idem", start.plusHours(1));
        PaymentTransaction txn = PaymentTransaction.record(order, PaymentTransactionType.CAPTURE,
                new com.example.payments.domain.payments.Money(BigDecimal.TEN, "USD"), "txn123", "CAPTURED",
                start.plusHours(2), "1", "Approved");

        doReturn(List.of(export)).when(settlementExportRepository).findByStatus(SettlementExportStatus.PENDING);
        doReturn(List.of(order)).when(paymentOrderRepository).findByCreatedAtBetween(start, end);
        doReturn(List.of(txn)).when(paymentTransactionRepository).findByOrderId(order.getId());
        doReturn(writer).when(writerFactory).writerFor(SettlementExportFormat.CSV);

        settlementExportService.processPendingExports();

        verify(writer).write(any(Path.class), any(List.class), any(List.class));
        verify(settlementExportRepository, times(1)).save(exportCaptor.capture());
        assertThat(exportCaptor.getValue().getStatus()).isEqualTo(SettlementExportStatus.COMPLETED);
    }

    @Test
    void processPendingExports_shouldMarkFailedOnWriterError() throws Exception {
        SettlementExport export = SettlementExport.create(SettlementExportFormat.CSV, start, end, "build/exports/test.csv");
        doReturn(List.of(export)).when(settlementExportRepository).findByStatus(SettlementExportStatus.PENDING);
        doReturn(writer).when(writerFactory).writerFor(SettlementExportFormat.CSV);
        doThrow(new RuntimeException("IO")).when(writer).write(any(Path.class), any(List.class), any(List.class));

        settlementExportService.processPendingExports();

        verify(settlementExportRepository, times(1)).save(exportCaptor.capture());
        assertThat(exportCaptor.getValue().getStatus()).isEqualTo(SettlementExportStatus.FAILED);
    }
}
