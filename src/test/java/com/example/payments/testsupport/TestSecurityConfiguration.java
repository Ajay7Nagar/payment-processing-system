package com.example.payments.testsupport;

import com.example.payments.infra.JwtProperties;
import com.example.payments.infra.JwtService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfiguration {

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    @Bean
    JwtProperties testJwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("ZHNsa2YzNzlLOHJ1ZzQySWFiNkZsU2UzTnhpU0J6VTRqZkpnd0t4VzFRTlJ3Ukh5aXAzTHN4RDJneHhQSlR1MDk3bUYyV3lyQTM4UWRhUw==");
        properties.setExpirationSeconds(3600);
        properties.setIssuer("payments-system");
        properties.setAudience("payments-clients");
        return properties;
    }

    @Bean
    JwtService testJwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}