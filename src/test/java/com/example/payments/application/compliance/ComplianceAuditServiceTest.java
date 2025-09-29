package com.example.payments.application.compliance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.example.payments.adapters.persistence.AuditLogRepository;
import com.example.payments.domain.shared.AuditLog;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ComplianceAuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private ComplianceAuditService complianceAuditService;

    @BeforeEach
    void setUp() {
        complianceAuditService = new ComplianceAuditService(auditLogRepository);
    }

    @Test
    void query_shouldReturnPage() {
        AuditLog log = AuditLog.record("actor", "OPER", "resource", UUID.randomUUID(), "{}", OffsetDateTime.now());
        doReturn(new PageImpl<>(List.of(log))).when(auditLogRepository)
                .findAll((Specification<AuditLog>) org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(Pageable.class));

        Page<AuditLog> page = complianceAuditService.query(new ComplianceAuditFilter(null, null, null, null, null, null),
                PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
