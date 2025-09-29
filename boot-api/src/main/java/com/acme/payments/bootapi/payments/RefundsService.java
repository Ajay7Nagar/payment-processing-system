package com.acme.payments.bootapi.payments;

public interface RefundsService {
    void validateRefundRequest(RefundDtos.RefundRequest req);
    void createRefund(RefundDtos.RefundRequest req);
}
