package com.example.payments.infra;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private static final String ROLES_CLAIM = "roles";

    private final JwtProperties properties;
    private final JwtDecoder jwtDecoder;
    private final JwtEncoder jwtEncoder;

    public JwtService(JwtProperties properties, JwtDecoder jwtDecoder, JwtEncoder jwtEncoder) {
        this.properties = properties;
        this.jwtDecoder = jwtDecoder;
        this.jwtEncoder = jwtEncoder;
    }

    public String createToken(String subject, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getExpirationSeconds());

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(subject)
                .claim(ROLES_CLAIM, authorities.stream().map(GrantedAuthority::getAuthority).toList());

        if (properties.getIssuer() != null) {
            claimsBuilder.issuer(properties.getIssuer());
        }

        if (properties.getAudience() != null) {
            claimsBuilder.audience(List.of(properties.getAudience()));
        }

        JwtClaimsSet claims = claimsBuilder.build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public Jwt decode(String token) {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtValidationException ex) {
            throw new BadCredentialsException(ex.getMessage(), ex);
        } catch (JwtException ex) {
            throw new BadJwtException(ex.getMessage(), ex);
        }
    }

    public Collection<? extends GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object rolesClaim = jwt.getClaim(ROLES_CLAIM);
        if (rolesClaim instanceof Collection<?> roles) {
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }
}

