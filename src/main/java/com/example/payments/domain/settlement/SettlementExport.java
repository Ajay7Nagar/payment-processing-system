package com.example.payments.domain.settlement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_exports")
public class SettlementExport {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false)
    private SettlementExportFormat format;

    @Column(name = "date_range_start", nullable = false)
    private OffsetDateTime dateRangeStart;

    @Column(name = "date_range_end", nullable = false)
    private OffsetDateTime dateRangeEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SettlementExportStatus status;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected SettlementExport() {
    }

    private SettlementExport(UUID id, SettlementExportFormat format, OffsetDateTime dateRangeStart,
            OffsetDateTime dateRangeEnd, SettlementExportStatus status, String filePath, OffsetDateTime createdAt) {
        this.id = id;
        this.format = format;
        this.dateRangeStart = dateRangeStart;
        this.dateRangeEnd = dateRangeEnd;
        this.status = status;
        this.filePath = filePath;
        this.createdAt = createdAt;
    }

    public static SettlementExport create(SettlementExportFormat format, OffsetDateTime start, OffsetDateTime end,
            String filePath) {
        return new SettlementExport(UUID.randomUUID(), format, start, end, SettlementExportStatus.PENDING, filePath,
                OffsetDateTime.now());
    }

    public UUID getId() {
        return id;
    }

    public SettlementExportFormat getFormat() {
        return format;
    }

    public OffsetDateTime getDateRangeStart() {
        return dateRangeStart;
    }

    public OffsetDateTime getDateRangeEnd() {
        return dateRangeEnd;
    }

    public SettlementExportStatus getStatus() {
        return status;
    }

    public String getFilePath() {
        return filePath;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void markCompleted(OffsetDateTime completedAt) {
        this.status = SettlementExportStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public void markFailed() {
        this.status = SettlementExportStatus.FAILED;
        this.completedAt = OffsetDateTime.now();
    }
}
