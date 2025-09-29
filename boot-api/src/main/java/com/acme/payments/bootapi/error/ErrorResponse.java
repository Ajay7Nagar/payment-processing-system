package com.acme.payments.bootapi.error;

public record ErrorResponse(String code, String message, String correlation_id) {}
