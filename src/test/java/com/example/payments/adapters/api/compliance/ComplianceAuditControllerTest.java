package com.example.payments.adapters.api.compliance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.payments.application.compliance.ComplianceAuditFilter;
import com.example.payments.application.compliance.ComplianceAuditService;
import com.example.payments.domain.shared.AuditLog;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ComplianceAuditController.class)
class ComplianceAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplianceAuditService complianceAuditService;

    @Test
    void query_shouldReturnPageForOfficer() throws Exception {
        AuditLog log = AuditLog.record("actor", "OPER", "payment_order", UUID.randomUUID(), "{}", OffsetDateTime.now());
        doReturn(new PageImpl<>(List.of(log))).when(complianceAuditService)
                .query(any(ComplianceAuditFilter.class), any());

        mockMvc.perform(get("/api/v1/compliance/audit-logs")
                .with(jwt().authorities(() -> "ROLE_COMPLIANCE_OFFICER"))
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actor").value("actor"));
    }

    @Test
    void export_shouldReturnListForOfficer() throws Exception {
        AuditLog log = AuditLog.record("actor", "OPER", "payment_order", UUID.randomUUID(), "{}", OffsetDateTime.now());
        doReturn(List.of(log)).when(complianceAuditService).export(any(ComplianceAuditFilter.class));

        mockMvc.perform(post("/api/v1/compliance/audit-logs/export")
                .with(jwt().authorities(() -> "ROLE_COMPLIANCE_OFFICER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].operation").value("OPER"));
    }

    @Test
    void query_shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/compliance/audit-logs").param("page", "0").param("size", "10"))
                .andExpect(status().isUnauthorized());
    }
}
