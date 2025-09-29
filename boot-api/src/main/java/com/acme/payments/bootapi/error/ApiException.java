package com.acme.payments.bootapi.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public ApiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public HttpStatus getStatus() { return status; }

    public static ApiException unprocessable(String code, String message) {
        return new ApiException(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ApiException gateway(String code, String message) {
        return new ApiException(code, message, HttpStatus.BAD_GATEWAY);
    }
}
