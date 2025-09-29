package com.example.payments.adapters.api.compliance;

import com.example.payments.adapters.api.compliance.dto.ComplianceAuditExportRequest;
import com.example.payments.adapters.api.compliance.dto.ComplianceAuditQueryRequest;
import com.example.payments.adapters.api.compliance.dto.ComplianceAuditResponse;
import com.example.payments.application.compliance.ComplianceAuditFilter;
import com.example.payments.application.compliance.ComplianceAuditService;
import com.example.payments.domain.shared.AuditLog;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance/audit-logs")
@Validated
public class ComplianceAuditController {

    private final ComplianceAuditService complianceAuditService;

    public ComplianceAuditController(ComplianceAuditService complianceAuditService) {
        this.complianceAuditService = complianceAuditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Page<ComplianceAuditResponse>> query(@Valid ComplianceAuditQueryRequest request) {
        ComplianceAuditFilter filter = toFilter(request.start(), request.end(), request.actor(), request.operation(),
                request.resourceType(), request.resourceId());
        Page<AuditLog> page = complianceAuditService.query(filter, PageRequest.of(request.page(), request.size()));
        return ResponseEntity.ok(page.map(this::toResponse));
    }

    @PostMapping("/export")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<ComplianceAuditResponse>> export(@Valid @RequestBody ComplianceAuditExportRequest request) {
        ComplianceAuditFilter filter = toFilter(request.start(), request.end(), request.actor(), request.operation(),
                request.resourceType(), request.resourceId());
        List<AuditLog> logs = complianceAuditService.export(filter);
        List<ComplianceAuditResponse> responses = logs.stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    private ComplianceAuditFilter toFilter(OffsetDateTime start, OffsetDateTime end, String actor, String operation,
            String resourceType, String resourceId) {
        UUID resourceUuid = null;
        if (resourceId != null && !resourceId.isBlank()) {
            resourceUuid = UUID.fromString(resourceId);
        }
        return new ComplianceAuditFilter(start, end, actor, operation, resourceType, resourceUuid);
    }

    private ComplianceAuditResponse toResponse(AuditLog log) {
        return new ComplianceAuditResponse(log.getId(), log.getActor(), log.getOperation(), log.getResourceType(),
                log.getResourceId(), log.getMetadata(), log.getCreatedAt());
    }
}
