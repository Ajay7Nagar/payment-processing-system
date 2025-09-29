package com.example.payments.adapters.api.settlement;

import com.example.payments.adapters.api.settlement.dto.SettlementExportRequest;
import com.example.payments.adapters.api.settlement.dto.SettlementExportResponse;
import com.example.payments.application.settlement.SettlementExportService;
import com.example.payments.domain.settlement.SettlementExport;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settlement/export")
public class SettlementExportController {

    private final SettlementExportService exportService;

    public SettlementExportController(SettlementExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SETTLEMENT_EXPORT')")
    public ResponseEntity<SettlementExportResponse> requestExport(@Valid @RequestBody SettlementExportRequest request) {
        OffsetDateTime start = request.start();
        OffsetDateTime end = request.end();
        Path path = request.filePath() != null ? Path.of(request.filePath())
                : Path.of("build/exports", "settlement-" + start.toLocalDate() + "." + request.format().name().toLowerCase());
        SettlementExport export = exportService.requestExport(request.format(), start, end, path);
        exportService.processPendingExports();
        SettlementExportResponse response = new SettlementExportResponse(export.getId(), export.getStatus(),
                export.getFilePath());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
