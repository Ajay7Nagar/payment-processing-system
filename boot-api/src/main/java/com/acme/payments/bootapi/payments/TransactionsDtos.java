package com.acme.payments.bootapi.payments;

import java.util.List;

public class TransactionsDtos {
    public record Transaction(
            String id,
            PaymentDtos.Money amount,
            String status,
            String createdAt
    ) {}

    public record ListTransactionsResponse(
            List<Transaction> items,
            String nextCursor
    ) {}
}


