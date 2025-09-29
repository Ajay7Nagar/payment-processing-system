package com.example.payments.application.settlement;

import com.example.payments.domain.settlement.SettlementExportFormat;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SettlementExportWriterFactory {

    private final Map<SettlementExportFormat, SettlementExportWriter> writers = new EnumMap<>(SettlementExportFormat.class);

    public SettlementExportWriterFactory(CsvSettlementExportWriter csvWriter, JsonSettlementExportWriter jsonWriter) {
        writers.put(SettlementExportFormat.CSV, csvWriter);
        writers.put(SettlementExportFormat.JSON, jsonWriter);
    }

    public SettlementExportWriter writerFor(SettlementExportFormat format) {
        return writers.getOrDefault(format, writers.get(SettlementExportFormat.CSV));
    }
}
