package com.example.payments.application.settlement;

import com.example.payments.domain.payments.PaymentOrder;
import com.example.payments.domain.payments.PaymentTransaction;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CsvSettlementExportWriter implements SettlementExportWriter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public void write(Path path, List<PaymentOrder> orders, List<PaymentTransaction> transactions) throws IOException {
        Map<PaymentOrder, List<PaymentTransaction>> byOrder = transactions.stream()
                .collect(Collectors.groupingBy(PaymentTransaction::getOrder));

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("order_id,customer_id,status,amount,currency,created_at,transaction_id,transaction_amount,transaction_status,processed_at");
            writer.newLine();
            for (PaymentOrder order : orders) {
                List<PaymentTransaction> txns = byOrder.getOrDefault(order, List.of());
                if (txns.isEmpty()) {
                    writer.write(String.join(",",
                            order.getId().toString(),
                            order.getCustomerId() != null ? order.getCustomerId().toString() : "",
                            order.getStatus().name(),
                            order.getMoney().amount().toPlainString(),
                            order.getMoney().currency(),
                            FORMATTER.format(order.getCreatedAt()),
                            "",
                            "",
                            "",
                            ""));
                    writer.newLine();
                } else {
                    for (PaymentTransaction txn : txns) {
                        writer.write(String.join(",",
                                order.getId().toString(),
                                order.getCustomerId() != null ? order.getCustomerId().toString() : "",
                                order.getStatus().name(),
                                order.getMoney().amount().toPlainString(),
                                order.getMoney().currency(),
                                FORMATTER.format(order.getCreatedAt()),
                                txn.getId().toString(),
                                txn.getMoney().amount().toPlainString(),
                                txn.getStatus(),
                                FORMATTER.format(txn.getProcessedAt())));
                        writer.newLine();
                    }
                }
            }
        }
    }
}
