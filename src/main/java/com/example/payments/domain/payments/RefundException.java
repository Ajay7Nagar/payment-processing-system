package com.example.payments.domain.payments;

public class RefundException extends RuntimeException {

    private final String errorCode;

    public RefundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RefundException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
