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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service implementation for authentication and user management.
 * <p>
 * Handles user registration, login, token refresh, and administrative
 * user management operations. All write operations are transactional.
 * </p>
 * <p>
 * Thread-safety: This class is thread-safe and can be used as a singleton.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

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

    /**
     * {@inheritDoc}
     */
    @Override
    public UserResponse register(RegisterRequest request) {
        logger.info("Processing registration for email: {}", request.getEmail());

        // Validate early to fail fast - check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration rejected: email already exists - {}", request.getEmail());
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }

        // Create new user with default USER role
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully with ID: {}", savedUser.getId());

        return UserResponse.fromEntity(savedUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TokenResponse login(LoginRequest request) {
        logger.info("Login attempt for email: {}", request.getEmail());

        // Find user by email - using generic error message to prevent user enumeration
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Login failed: user not found for email - {}", request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        // Verify password - using generic error message for security
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Login failed: invalid password for user ID - {}", user.getId());
            throw new UnauthorizedException("Invalid email or password");
        }

        // Generate tokens after successful authentication
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshTokenString = tokenProvider.generateRefreshToken();

        // Persist refresh token for token rotation support
        RefreshToken refreshToken = new RefreshToken(
                user,
                refreshTokenString,
                LocalDateTime.now().plusHours(refreshTokenValidityHours));
        refreshTokenRepository.save(refreshToken);

        logger.info("Login successful for user ID: {}", user.getId());
        return new TokenResponse(accessToken, refreshTokenString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TokenResponse refresh(RefreshRequest request) {
        logger.debug("Processing token refresh request");

        // Find refresh token - token value not logged for security
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    logger.warn("Token refresh failed: invalid refresh token provided");
                    return new UnauthorizedException("Invalid refresh token");
                });

        // Check expiration before rotation
        if (refreshToken.isExpired()) {
            logger.warn("Token refresh failed: expired token for user ID - {}", refreshToken.getUser().getId());
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Implement token rotation - delete old token to prevent reuse
        refreshTokenRepository.delete(refreshToken);

        // Generate new token pair
        User user = refreshToken.getUser();
        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshTokenString = tokenProvider.generateRefreshToken();

        // Persist new refresh token
        RefreshToken newRefreshToken = new RefreshToken(
                user,
                newRefreshTokenString,
                LocalDateTime.now().plusHours(refreshTokenValidityHours));
        refreshTokenRepository.save(newRefreshToken);

        logger.info("Token refreshed successfully for user ID: {}", user.getId());
        return new TokenResponse(newAccessToken, newRefreshTokenString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        logger.debug("Retrieving current user information for ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Current user not found for ID: {}", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });

        return UserResponse.fromEntity(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        logger.debug("Retrieving all users with pagination - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return userRepository.findAll(pageable)
                .map(UserResponse::fromEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        logger.debug("Retrieving user by ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found for ID: {}", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });

        return UserResponse.fromEntity(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserResponse updateUserRole(UUID userId, UpdateRoleRequest request) {
        logger.info("Updating role for user ID: {} to role: {}", userId, request.getRole());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Role update failed: user not found for ID: {}", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });

        Role previousRole = user.getRole();

        try {
            Role newRole = Role.valueOf(request.getRole().toUpperCase());
            user.setRole(newRole);
            User updatedUser = userRepository.save(user);

            logger.info("Role updated successfully for user ID: {} from {} to {}",
                    userId, previousRole, newRole);

            return UserResponse.fromEntity(updatedUser);
        } catch (IllegalArgumentException e) {
            logger.error("Role update failed: invalid role value - {}", request.getRole());
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }
    }
}
