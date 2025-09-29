package com.example.payments.application.settlement;

import com.example.payments.domain.payments.PaymentOrder;
import com.example.payments.domain.payments.PaymentTransaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class JsonSettlementExportWriter implements SettlementExportWriter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void write(Path path, List<PaymentOrder> orders, List<PaymentTransaction> transactions) throws IOException {
        Map<PaymentOrder, List<PaymentTransaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(PaymentTransaction::getOrder));

        List<Map<String, Object>> payload = orders.stream().map(order -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("orderId", order.getId());
            entry.put("customerId", order.getCustomerId());
            entry.put("status", order.getStatus());
            entry.put("amount", order.getMoney().amount());
            entry.put("currency", order.getMoney().currency());
            entry.put("createdAt", order.getCreatedAt());
            entry.put("transactions", grouped.getOrDefault(order, List.of()));
            return entry;
        }).collect(Collectors.toList());

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), payload);
    }
}
