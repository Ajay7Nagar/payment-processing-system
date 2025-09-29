package com.acme.payments.bootapi.error;

import com.acme.payments.bootapi.config.CorrelationIdFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(@org.springframework.beans.factory.annotation.Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletResponse resp) {
        String correlation = resp.getHeader(CorrelationIdFilter.CORRELATION_ID);
        increment("INVALID_REQUEST", HttpStatus.UNPROCESSABLE_ENTITY);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("INVALID_REQUEST", ex.getMessage(), correlation));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletResponse resp) {
        String correlation = resp.getHeader(CorrelationIdFilter.CORRELATION_ID);
        increment("INVALID_REQUEST", HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", ex.getMessage(), correlation));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletResponse resp) {
        String correlation = resp.getHeader(CorrelationIdFilter.CORRELATION_ID);
        increment(ex.getCode(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), correlation));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletResponse resp) {
        String correlation = resp.getHeader(CorrelationIdFilter.CORRELATION_ID);
        increment("GATEWAY_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("GATEWAY_ERROR", "Unexpected error", correlation));
    }

    private void increment(String code, HttpStatus status) {
        if (meterRegistry != null) {
            meterRegistry.counter("http.error.count", "code", code, "status", String.valueOf(status.value())).increment();
        }
    }
}
