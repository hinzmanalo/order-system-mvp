package com.orderhub.auth.service;

import com.orderhub.auth.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for authentication and user management
 */
public interface AuthService {

    /**
     * Register a new user
     */
    UserResponse register(RegisterRequest request);

    /**
     * Login user and generate tokens
     */
    TokenResponse login(LoginRequest request);

    /**
     * Refresh access token using refresh token
     */
    TokenResponse refresh(RefreshRequest request);

    /**
     * Get current user information
     */
    UserResponse getCurrentUser(UUID userId);

    /**
     * Get all users (admin only)
     */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Get user by ID (admin only)
     */
    UserResponse getUserById(UUID userId);

    /**
     * Update user role (admin only)
     */
    UserResponse updateUserRole(UUID userId, UpdateRoleRequest request);
}
