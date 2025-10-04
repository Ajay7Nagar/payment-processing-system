package com.example.payments.application.auth;

import com.example.payments.application.auth.dto.AuthResponse;
import com.example.payments.application.auth.dto.LoginRequest;
import com.example.payments.application.auth.dto.RefreshTokenRequest;
import com.example.payments.application.auth.dto.RegisterRequest;
import com.example.payments.domain.customer.Customer;
import com.example.payments.domain.customer.CustomerRepository;
import com.example.payments.domain.user.Claim;
import com.example.payments.domain.user.Role;
import com.example.payments.domain.user.RoleRepository;
import com.example.payments.domain.user.User;
import com.example.payments.domain.user.UserRepository;
import com.example.payments.infra.JwtService;
import com.example.payments.infra.auth.RefreshToken;
import com.example.payments.infra.auth.RefreshTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AuthServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private CustomerRepository customerRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private ApplicationEventPublisher eventPublisher;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        customerRepository = mock(CustomerRepository.class);

        authService = new AuthService(userRepository, roleRepository, refreshTokenRepository, customerRepository,
                passwordEncoder, jwtService, eventPublisher);
    }

    @Test
    void register_createsUserAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("user@example.com", "secret", "User", List.of("ADMIN"));
        Role adminRole = new Role("ADMIN", "Administrator");
        adminRole.addClaim(new Claim("COMPLIANCE_AUDIT_VIEW", "Audit"));

        given(userRepository.existsByEmailIgnoreCase("user@example.com")).willReturn(false);
        given(roleRepository.findByCode("ADMIN")).willReturn(Optional.of(adminRole));
        given(passwordEncoder.encode("secret")).willReturn("hashed");

        UUID userId = UUID.randomUUID();
        User saved = new User("user@example.com", "hashed", "User");
        saved.addRole(adminRole);
        ReflectionTestUtils.setField(saved, "id", userId);

        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.addRole(adminRole);
            ReflectionTestUtils.setField(toSave, "id", userId);
            return toSave;
        });
        given(customerRepository.findByExternalRef(userId.toString())).willReturn(Optional.empty());
        UUID customerId = UUID.randomUUID();
        given(customerRepository.save(any(Customer.class))).willAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", customerId);
            return c;
        });
        given(jwtService.createToken(any(), any())).willReturn("access-token");
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.accessToken()).isEmpty();
        assertThat(response.refreshToken()).isEmpty();
        assertThat(response.refreshTokenExpiresAt()).isEmpty();
        verify(passwordEncoder).encode("secret");
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void register_throwsWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("dup@example.com", "secret", "Dup", List.of("ADMIN"));
        given(userRepository.existsByEmailIgnoreCase("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentialsIssuesTokens() {
        LoginRequest request = new LoginRequest("user@example.com", "secret");
        User user = new User("user@example.com", "hashed", "User");
        Role adminRole = new Role("ADMIN", "admin");
        adminRole.addClaim(new Claim("COMPLIANCE_AUDIT_VIEW", "Audit"));
        user.addRole(adminRole);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        given(userRepository.findByEmailIgnoreCase("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("secret", "hashed")).willReturn(true);
        Customer customer = Customer.forExternalRef("ref");
        ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
        given(customerRepository.findByExternalRef(any())).willReturn(Optional.of(customer));
        given(jwtService.createToken(any(), any())).willReturn("access-token");
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).contains("access-token");
        assertThat(response.refreshToken()).isPresent();
        assertThat(response.refreshTokenExpiresAt()).isPresent();
        assertThat(response.customerId()).isEqualTo(customer.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_invalidPasswordThrows() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong");
        User user = new User("user@example.com", "hashed", "User");

        given(userRepository.findByEmailIgnoreCase("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_validTokenIssuesNewTokens() {
        User user = new User("user@example.com", "hashed", "User");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        RefreshToken refreshToken = new RefreshToken(user, UUID.randomUUID().toString(), Instant.now().plusSeconds(60));

        given(refreshTokenRepository.findByToken(refreshToken.getToken())).willReturn(Optional.of(refreshToken));
        Customer customer = Customer.forExternalRef("ref");
        ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
        given(customerRepository.findByExternalRef(any())).willReturn(Optional.of(customer));
        given(jwtService.createToken(any(), any())).willReturn("new-access");
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refresh(new RefreshTokenRequest(refreshToken.getToken()));

        assertThat(response.accessToken()).contains("new-access");
        assertThat(response.refreshToken()).isPresent();
        assertThat(response.refreshTokenExpiresAt()).isPresent();
        assertThat(response.customerId()).isEqualTo(customer.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refresh_expiredTokenThrows() {
        RefreshToken refreshToken = new RefreshToken(new User("user@example.com", "hashed", "User"),
                UUID.randomUUID().toString(), Instant.now().minusSeconds(1));

        given(refreshTokenRepository.findByToken(refreshToken.getToken())).willReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(refreshToken.getToken())))
                .isInstanceOf(BadCredentialsException.class);
    }
}
