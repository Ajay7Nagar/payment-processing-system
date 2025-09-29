package com.example.payments.application.settlement;

import com.example.payments.domain.payments.PaymentOrder;
import com.example.payments.domain.payments.PaymentTransaction;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SettlementExportWriter {

    void write(Path path, List<PaymentOrder> orders, List<PaymentTransaction> transactions) throws IOException;
}
