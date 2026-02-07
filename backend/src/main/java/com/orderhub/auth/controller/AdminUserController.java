package com.orderhub.auth.controller;

import com.orderhub.auth.dto.UpdateRoleRequest;
import com.orderhub.auth.dto.UserResponse;
import com.orderhub.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for administrative user management endpoints.
 * <p>
 * Provides endpoints for listing users, viewing user details, and updating
 * user roles. All endpoints require ADMIN role authorization.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin - Users", description = "Admin endpoints for user management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    private final AuthService authService;

    /**
     * Constructs an AdminUserController with required dependencies.
     *
     * @param authService the authentication service for user management operations
     */
    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Retrieves all users with pagination support.
     *
     * @param pageable pagination parameters (default page size: 20)
     * @return ResponseEntity with a page of user information
     */
    @GetMapping
    @Operation(summary = "Get all users (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        logger.info("Admin request: retrieving all users - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<UserResponse> users = authService.getAllUsers(pageable);

        logger.info("Retrieved {} users (total: {})", users.getNumberOfElements(), users.getTotalElements());
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves a specific user by their ID.
     *
     * @param id the unique identifier of the user
     * @return ResponseEntity with user information
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        logger.info("Admin request: retrieving user by ID: {}", id);

        UserResponse user = authService.getUserById(id);

        logger.info("User retrieved successfully: {} ({})", user.getEmail(), id);
        return ResponseEntity.ok(user);
    }

    /**
     * Updates a user's role.
     *
     * @param id      the unique identifier of the user to update
     * @param request the role update request containing the new role
     * @return ResponseEntity with updated user information
     */
    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User role updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        logger.info("Admin request: updating role for user ID: {} to role: {}", id, request.getRole());

        UserResponse user = authService.updateUserRole(id, request);

        logger.info("User role updated successfully for user ID: {} - new role: {}", id, user.getRole());
        return ResponseEntity.ok(user);
    }
}
