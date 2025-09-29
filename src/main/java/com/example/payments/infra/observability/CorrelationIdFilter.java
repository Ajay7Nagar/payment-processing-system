package com.example.payments.infra.observability;

import com.example.payments.domain.shared.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader(CORRELATION_ID_HEADER);
        String correlationId = Optional.ofNullable(incoming)
                .filter(value -> !value.isBlank())
                .orElse(UUID.randomUUID().toString());

        MDC.put(MDC_KEY, correlationId);
        CorrelationIdContext.set(new CorrelationId(correlationId));
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationIdContext.clear();
            MDC.remove(MDC_KEY);
        }
    }
}

