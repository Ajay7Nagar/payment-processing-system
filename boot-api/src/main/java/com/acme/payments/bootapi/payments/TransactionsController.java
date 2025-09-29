package com.acme.payments.bootapi.payments;

import com.acme.payments.adapters.out.db.entity.OrderEntity;
import com.acme.payments.adapters.out.db.repo.OrderRepository;
import com.acme.payments.bootapi.security.MerchantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TransactionsController {
    private final OrderRepository orderRepository;
    private final MerchantContext merchantContext;

    public TransactionsController(OrderRepository orderRepository, MerchantContext merchantContext) {
        this.orderRepository = orderRepository;
        this.merchantContext = merchantContext;
    }

    @GetMapping("/v1/transactions")
    public TransactionsDtos.ListTransactionsResponse list(@RequestParam(name = "limit", defaultValue = "50") int limit,
                                                          @RequestParam(name = "nextCursor", required = false) String nextCursor,
                                                          @RequestParam(name = "status", required = false) String status) {
        String merchantId = merchantContext.getMerchantId();
        int pageSize = Math.min(Math.max(limit, 1), 200);

        List<OrderEntity> orders;
        if (nextCursor != null && !nextCursor.isBlank()) {
            Cursor cursor = decode(nextCursor);
            orders = orderRepository.findSliceBefore(merchantId, status, cursor.createdAt, cursor.id, PageRequest.of(0, pageSize));
        } else if (status != null && !status.isBlank()) {
            orders = orderRepository.findByMerchantIdAndStatusOrderByCreatedAtDesc(merchantId, status, PageRequest.of(0, pageSize)).getContent();
        } else {
            orders = orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, PageRequest.of(0, pageSize)).getContent();
        }

        List<TransactionsDtos.Transaction> items = orders.stream().map(o -> new TransactionsDtos.Transaction(
                o.getId(),
                new PaymentDtos.Money(new BigDecimal(o.getAmountMinor()).movePointLeft(2).toPlainString(), o.getCurrency()),
                o.getStatus(),
                o.getCreatedAt().toString()
        )).collect(Collectors.toList());

        String newCursor = orders.size() == pageSize
                ? encode(orders.get(orders.size() - 1).getCreatedAt(), orders.get(orders.size() - 1).getId())
                : null;

        return new TransactionsDtos.ListTransactionsResponse(items, newCursor);
    }

    private static String encode(Instant createdAt, String id) {
        String raw = createdAt.toEpochMilli() + ":" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static Cursor decode(String cursor) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor), java.nio.charset.StandardCharsets.UTF_8);
        int idx = raw.lastIndexOf(":");
        if (idx <= 0) {
            return new Cursor(Instant.EPOCH, "");
        }
        long epoch = Long.parseLong(raw.substring(0, idx));
        String id = raw.substring(idx + 1);
        return new Cursor(Instant.ofEpochMilli(epoch), id);
    }

    private record Cursor(Instant createdAt, String id) {}
}

