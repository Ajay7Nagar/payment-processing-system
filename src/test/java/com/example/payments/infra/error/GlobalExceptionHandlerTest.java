package com.example.payments.infra.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.payments.domain.payments.PaymentException;
import com.example.payments.domain.payments.RefundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handlePaymentException_shouldMapToConflict() {
        PaymentException exception = new PaymentException("INVALID_STATE", "Invalid state transition");

        ResponseEntity<ErrorResponse> response = handler.handlePaymentException(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_STATE");
    }

    @Test
    void handleRefundException_shouldReturnConflict() {
        RefundException exception = new RefundException("INVALID_AMOUNT", "Too much");

        ResponseEntity<ErrorResponse> response = handler.handleRefundException(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().errorCode()).isEqualTo("INVALID_AMOUNT");
    }

    @Test
    void handleOther_shouldReturnInternalServerError() {
        Exception exception = new Exception("boom");

        ResponseEntity<ErrorResponse> response = handler.handleOther(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
    }
}
