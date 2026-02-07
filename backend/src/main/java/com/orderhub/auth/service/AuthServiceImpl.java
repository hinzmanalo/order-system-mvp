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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service implementation for authentication and user management
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Value("${app.jwt.refresh-token-validity-hours:168}") // 7 days default
    private int refreshTokenValidityHours;

    public AuthServiceImpl(UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshTokenString = tokenProvider.generateRefreshToken();

        // Save refresh token
        RefreshToken refreshToken = new RefreshToken(
                user,
                refreshTokenString,
                LocalDateTime.now().plusHours(refreshTokenValidityHours));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenString);
    }

    @Override
    public TokenResponse refresh(RefreshRequest request) {
        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Check if expired
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Delete old refresh token (rotation)
        refreshTokenRepository.delete(refreshToken);

        // Generate new tokens
        User user = refreshToken.getUser();
        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshTokenString = tokenProvider.generateRefreshToken();

        // Save new refresh token
        RefreshToken newRefreshToken = new RefreshToken(
                user,
                newRefreshTokenString,
                LocalDateTime.now().plusHours(refreshTokenValidityHours));
        refreshTokenRepository.save(newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshTokenString);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse updateUserRole(UUID userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        try {
            Role newRole = Role.valueOf(request.getRole().toUpperCase());
            user.setRole(newRole);
            User updatedUser = userRepository.save(user);
            return UserResponse.fromEntity(updatedUser);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }
    }
}
