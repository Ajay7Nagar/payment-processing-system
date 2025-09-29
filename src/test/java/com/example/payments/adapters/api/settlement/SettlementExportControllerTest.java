package com.example.payments.adapters.api.settlement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.payments.application.settlement.SettlementExportService;
import com.example.payments.domain.settlement.SettlementExport;
import com.example.payments.domain.settlement.SettlementExportFormat;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SettlementExportController.class)
class SettlementExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SettlementExportService exportService;

    @MockBean
    private org.springframework.security.core.Authentication authentication;

    @Test
    void requestExport_shouldReturnAccepted() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2025-09-28T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2025-09-29T00:00:00Z");
        SettlementExport export = SettlementExport.create(SettlementExportFormat.CSV, start, end, "build/exports/test.csv");
        doReturn(export).when(exportService).requestExport(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/settlement/export")
                .with(jwt().authorities(() -> "ROLE_SETTLEMENT_EXPORT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"format\":\"CSV\"," +
                        "\"start\":\"2025-09-28T00:00:00Z\"," +
                        "\"end\":\"2025-09-29T00:00:00Z\"" +
                        "}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void requestExport_shouldAcceptDefaultPath() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2025-09-28T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2025-09-29T00:00:00Z");
        SettlementExport export = SettlementExport.create(SettlementExportFormat.CSV, start, end,
                "build/exports/settlement-2025-09-28.csv");
        doReturn(export).when(exportService).requestExport(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/settlement/export")
                .with(jwt().authorities(() -> "ROLE_SETTLEMENT_EXPORT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"format\":\"CSV\"," +
                        "\"start\":\"2025-09-28T00:00:00Z\"," +
                        "\"end\":\"2025-09-29T00:00:00Z\"" +
                        "}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.filePath").value("build/exports/settlement-2025-09-28.csv"));
    }

    @Test
    void requestExport_shouldUseProvidedPath() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2025-09-28T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2025-09-29T00:00:00Z");
        SettlementExport export = SettlementExport.create(SettlementExportFormat.JSON, start, end,
                "custom/path/settlement.json");
        doReturn(export).when(exportService).requestExport(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/settlement/export")
                .with(jwt().authorities(() -> "ROLE_SETTLEMENT_EXPORT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"format\":\"JSON\"," +
                        "\"start\":\"2025-09-28T00:00:00Z\"," +
                        "\"end\":\"2025-09-29T00:00:00Z\"," +
                        "\"filePath\":\"custom/path/settlement.json\"" +
                        "}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.filePath").value("custom/path/settlement.json"));
    }

    @Test
    void requestExport_shouldForbidWithoutRole() throws Exception {
        mockMvc.perform(post("/api/v1/settlement/export")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"format\":\"CSV\"," +
                        "\"start\":\"2025-09-28T00:00:00Z\"," +
                        "\"end\":\"2025-09-29T00:00:00Z\"" +
                        "}"))
                .andExpect(status().isForbidden());
    }
}
