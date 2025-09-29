package com.example.payments.domain.billing;

public class SubscriptionException extends RuntimeException {

    private final String errorCode;

    public SubscriptionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SubscriptionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
