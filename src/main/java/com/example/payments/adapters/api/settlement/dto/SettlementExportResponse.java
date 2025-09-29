package com.example.payments.adapters.api.settlement.dto;

import com.example.payments.domain.settlement.SettlementExportStatus;
import java.util.UUID;

public record SettlementExportResponse(UUID exportId, SettlementExportStatus status, String filePath) {
}
