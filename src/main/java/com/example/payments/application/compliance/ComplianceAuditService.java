package com.example.payments.application.compliance;

import com.example.payments.adapters.persistence.AuditLogRepository;
import com.example.payments.domain.shared.AuditLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ComplianceAuditService {

    private final AuditLogRepository auditLogRepository;

    public ComplianceAuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Page<AuditLog> query(ComplianceAuditFilter filter, Pageable pageable) {
        Specification<AuditLog> spec = Specification.where(ComplianceSpecifications.withinDateRange(filter.start(), filter.end()))
                .and(ComplianceSpecifications.hasActor(filter.actor()))
                .and(ComplianceSpecifications.hasOperation(filter.operation()))
                .and(ComplianceSpecifications.hasResourceType(filter.resourceType()))
                .and(ComplianceSpecifications.hasResourceId(filter.resourceId()));
        Pageable page = pageable.getPageSize() == 0 ? PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt")) : pageable;
        return auditLogRepository.findAll(spec, page);
    }

    public List<AuditLog> export(ComplianceAuditFilter filter) {
        Specification<AuditLog> spec = Specification.where(ComplianceSpecifications.withinDateRange(filter.start(), filter.end()))
                .and(ComplianceSpecifications.hasActor(filter.actor()))
                .and(ComplianceSpecifications.hasOperation(filter.operation()))
                .and(ComplianceSpecifications.hasResourceType(filter.resourceType()))
                .and(ComplianceSpecifications.hasResourceId(filter.resourceId()));
        return auditLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
