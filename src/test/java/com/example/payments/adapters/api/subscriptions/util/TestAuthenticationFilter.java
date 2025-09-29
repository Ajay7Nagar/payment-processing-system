package com.example.payments.adapters.api.subscriptions.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class TestAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String role = header.substring("Bearer ".length());
            AbstractAuthenticationToken token = new AbstractAuthenticationToken(
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))) {
                @Override
                public Object getCredentials() {
                    return "";
                }

                @Override
                public Object getPrincipal() {
                    return "tester";
                }
            };
            token.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(token);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
