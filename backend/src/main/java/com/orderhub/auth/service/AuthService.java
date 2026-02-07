package com.orderhub.auth.service;

import com.orderhub.auth.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for authentication and user management.
 * <p>
 * Provides operations for user registration, authentication, token management,
 * and administrative user management functions.
 * </p>
 * <p>
 * Thread-safety: Implementations of this interface should be thread-safe.
 * </p>
 *
 * @see AuthServiceImpl
 * @since 1.0.0
 */
public interface AuthService {

    /**
     * Registers a new user in the system.
     *
     * @param request the registration request containing user details, must not be null
     * @return the created user information as a response DTO
     * @throws com.orderhub.common.exception.ConflictException if a user with the same email already exists
     * @throws IllegalArgumentException if request is null or contains invalid data
     */
    UserResponse register(RegisterRequest request);

    /**
     * Authenticates a user and generates access and refresh tokens.
     *
     * @param request the login credentials, must not be null
     * @return token response containing access and refresh tokens
     * @throws com.orderhub.common.exception.UnauthorizedException if credentials are invalid
     */
    TokenResponse login(LoginRequest request);

    /**
     * Refreshes the access token using a valid refresh token.
     * <p>
     * Implements token rotation: the old refresh token is invalidated
     * and a new one is issued along with the new access token.
     * </p>
     *
     * @param request the refresh request containing the refresh token, must not be null
     * @return new token response with fresh access and refresh tokens
     * @throws com.orderhub.common.exception.UnauthorizedException if refresh token is invalid or expired
     */
    TokenResponse refresh(RefreshRequest request);

    /**
     * Retrieves the current user's information by their ID.
     *
     * @param userId the unique identifier of the user, must not be null
     * @return the user information as a response DTO
     * @throws com.orderhub.common.exception.ResourceNotFoundException if user is not found
     */
    UserResponse getCurrentUser(UUID userId);

    /**
     * Retrieves all users with pagination support (admin only).
     *
     * @param pageable pagination parameters
     * @return a page of user response DTOs
     */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Retrieves a specific user by their ID (admin only).
     *
     * @param userId the unique identifier of the user, must not be null
     * @return the user information as a response DTO
     * @throws com.orderhub.common.exception.ResourceNotFoundException if user is not found
     */
    UserResponse getUserById(UUID userId);

    /**
     * Updates a user's role (admin only).
     *
     * @param userId the unique identifier of the user to update, must not be null
     * @param request the role update request containing the new role, must not be null
     * @return the updated user information as a response DTO
     * @throws com.orderhub.common.exception.ResourceNotFoundException if user is not found
     * @throws IllegalArgumentException if the specified role is invalid
     */
    UserResponse updateUserRole(UUID userId, UpdateRoleRequest request);
}
