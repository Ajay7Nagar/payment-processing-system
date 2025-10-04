package com.example.payments.infra;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private static final String CLAIMS_CLAIM = "claims";
    private final Key signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String createToken(String subject, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getExpirationSeconds());
        
        List<String> claims = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();

        return Jwts.builder()
                .setSubject(subject)
                .claim(CLAIMS_CLAIM, claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .setIssuer(properties.getIssuer())
                .setAudience(properties.getAudience())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractSubject(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractClaims(String token) {
        Claims claims = parseToken(token);
        Object claimObj = claims.get(CLAIMS_CLAIM);
        if (claimObj instanceof List<?>) {
            return ((List<?>) claimObj).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}