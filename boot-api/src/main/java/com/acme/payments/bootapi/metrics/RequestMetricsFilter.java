package com.acme.payments.bootapi.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnBean(MeterRegistry.class)
public class RequestMetricsFilter extends OncePerRequestFilter {

	private final MeterRegistry registry;

	public RequestMetricsFilter(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		long start = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = (System.nanoTime() - start) / 1_000_000L;
			String method = request.getMethod();
			String path = request.getRequestURI();
			int status = response.getStatus();
			Tags tags = Tags.of(Tag.of("method", method), Tag.of("path", path), Tag.of("status", String.valueOf(status)));
			registry.counter("http.requests.total", tags).increment();
			registry.timer("http.requests.latency", tags).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
		}
	}
}
