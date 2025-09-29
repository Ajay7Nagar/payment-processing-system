package com.acme.payments.bootapi.payments;

public interface PaymentsService {
    void validatePurchase(PaymentDtos.PurchaseRequest req);
    void createOrderIfAbsent(PaymentDtos.PurchaseRequest req);
}
