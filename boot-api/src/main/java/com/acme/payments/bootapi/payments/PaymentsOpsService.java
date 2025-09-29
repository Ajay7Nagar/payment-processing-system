package com.acme.payments.bootapi.payments;

public interface PaymentsOpsService {
    void authorize(PaymentDtos.AuthorizeRequest req);
    void capture(PaymentDtos.CaptureRequest req);
    void cancel(PaymentDtos.CancelRequest req);
}
