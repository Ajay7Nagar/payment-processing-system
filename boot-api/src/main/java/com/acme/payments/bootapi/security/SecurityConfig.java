package com.acme.payments.bootapi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http,
									@Value("${app.security.enabled:true}") boolean securityEnabled) throws Exception {
		if (!securityEnabled) {
			http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
			return http.build();
		}
		http.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/**").permitAll()
				.requestMatchers("/v1/webhooks/**").permitAll()
				.requestMatchers("/v1/transactions/**").hasAnyRole("ADMIN","OPS","READ_ONLY")
				.requestMatchers("/v1/payments/**", "/v1/subscriptions/**").hasAnyRole("ADMIN","OPS")
				.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}
}


