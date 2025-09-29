package com.example.payments.adapters.api.settlement.dto;

import com.example.payments.domain.settlement.SettlementExportFormat;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record SettlementExportRequest(
        @NotNull SettlementExportFormat format,
        @NotNull OffsetDateTime start,
        @NotNull OffsetDateTime end,
        String filePath) {
}
