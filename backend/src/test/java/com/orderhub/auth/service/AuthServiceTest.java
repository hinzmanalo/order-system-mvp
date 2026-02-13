package com.orderhub.auth.service;

import com.orderhub.auth.dto.*;
import com.orderhub.auth.entity.RefreshToken;
import com.orderhub.auth.entity.Role;
import com.orderhub.auth.entity.User;
import com.orderhub.auth.repository.RefreshTokenRepository;
import com.orderhub.auth.repository.UserRepository;
import com.orderhub.auth.security.JwtTokenProvider;
import com.orderhub.common.exception.ConflictException;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenValidityHours", 168);
    }

    @Test
    @DisplayName("Register - success - creates user with hashed password")
    void register_success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail(request.getEmail());
        savedUser.setPasswordHash("hashedPassword");
        savedUser.setFirstName(request.getFirstName());
        savedUser.setLastName(request.getLastName());
        savedUser.setRole(Role.USER);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        UserResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(response.getLastName()).isEqualTo(request.getLastName());
        assertThat(response.getRole()).isEqualTo(Role.USER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getPasswordHash()).isEqualTo("hashedPassword");
        assertThat(capturedUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Register - duplicate email - throws ConflictException")
    void register_duplicateEmail_throws() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login - valid credentials - returns tokens")
    void login_validCredentials_returnsTokens() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.getEmail());
        user.setPasswordHash("hashedPassword");
        user.setRole(Role.USER);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(tokenProvider.generateAccessToken(user)).thenReturn("accessToken123");
        when(tokenProvider.generateRefreshToken()).thenReturn("refreshToken123");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // When
        TokenResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken123");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken123");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Login - invalid password - throws UnauthorizedException")
    void login_invalidPassword_throws() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.getEmail());
        user.setPasswordHash("hashedPassword");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");

        verify(tokenProvider, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login - nonexistent email - throws UnauthorizedException")
    void login_nonexistentEmail_throws() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenProvider, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Refresh - valid token - returns new tokens")
    void refresh_validToken_returnsNewTokens() {
        // Given
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("oldRefreshToken");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        RefreshToken refreshToken = new RefreshToken(
                user,
                "oldRefreshToken",
                LocalDateTime.now().plusDays(7));

        when(refreshTokenRepository.findByToken(request.getRefreshToken()))
                .thenReturn(Optional.of(refreshToken));
        when(tokenProvider.generateAccessToken(user)).thenReturn("newAccessToken");
        when(tokenProvider.generateRefreshToken()).thenReturn("newRefreshToken");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // When
        TokenResponse response = authService.refresh(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");

        verify(refreshTokenRepository).delete(refreshToken); // Old token deleted
        verify(refreshTokenRepository).save(any(RefreshToken.class)); // New token saved
    }

    @Test
    @DisplayName("Refresh - expired token - throws UnauthorizedException")
    void refresh_expiredToken_throws() {
        // Given
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("expiredToken");

        User user = new User();
        user.setId(UUID.randomUUID());

        RefreshToken refreshToken = new RefreshToken(
                user,
                "expiredToken",
                LocalDateTime.now().minusDays(1) // Expired
        );

        when(refreshTokenRepository.findByToken(request.getRefreshToken()))
                .thenReturn(Optional.of(refreshToken));

        // When & Then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(refreshToken); // Expired token should be deleted
        verify(tokenProvider, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Refresh - invalid token - throws UnauthorizedException")
    void refresh_invalidToken_throws() {
        // Given
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("invalidToken");

        when(refreshTokenRepository.findByToken(request.getRefreshToken()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(tokenProvider, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("UpdateRole - user to admin - success")
    void updateRole_userToAdmin_success() {
        // Given
        UUID userId = UUID.randomUUID();
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole("ADMIN");

        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UserResponse response = authService.updateUserRole(userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("UpdateRole - already admin - idempotent")
    void updateRole_alreadyAdmin_idempotent() {
        // Given
        UUID userId = UUID.randomUUID();
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole("ADMIN");

        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setRole(Role.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // When
        UserResponse response = authService.updateUserRole(userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);

        verify(userRepository).save(user);
    }
}
