package com.example.payments.application.auth;

import com.example.payments.application.auth.dto.AuthResponse;
import com.example.payments.application.auth.dto.LoginRequest;
import com.example.payments.application.auth.dto.RefreshTokenRequest;
import com.example.payments.application.auth.dto.RegisterRequest;
import com.example.payments.application.auth.event.UserRegisteredEvent;
import com.example.payments.domain.user.Role;
import com.example.payments.domain.user.RoleRepository;
import com.example.payments.domain.user.User;
import com.example.payments.domain.user.UserRepository;
import com.example.payments.infra.JwtService;
import com.example.payments.infra.auth.RefreshToken;
import com.example.payments.infra.auth.RefreshTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName());

        if (request.roleCodes().isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }

        List<Role> roles = request.roleCodes().stream()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + code)))
                .toList();

        roles.forEach(user::addRole);

        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId(), saved.getEmail()));
        return issueTokens(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        return issueTokens(refreshToken.getUser());
    }

    public void revokeRefreshTokens(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private AuthResponse issueTokens(User user) {
        Collection<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(Role::getCode)
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .toList();

        String accessToken = jwtService.createToken(user.getId().toString(), authorities);

        String refreshTokenValue = UUID.randomUUID().toString();
        Instant refreshExpiry = Instant.now().plus(30, ChronoUnit.DAYS);

        RefreshToken refreshToken = new RefreshToken(user, refreshTokenValue, refreshExpiry);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshTokenValue, refreshExpiry);
    }
}
