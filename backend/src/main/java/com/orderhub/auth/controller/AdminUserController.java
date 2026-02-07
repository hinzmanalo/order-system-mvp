package com.orderhub.auth.controller;

import com.orderhub.auth.dto.UpdateRoleRequest;
import com.orderhub.auth.dto.UserResponse;
import com.orderhub.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for admin user management endpoints
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin - Users", description = "Admin endpoints for user management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Get all users (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserResponse> users = authService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse user = authService.getUserById(id);
        return ResponseEntity.ok(user);
    }

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
        UserResponse user = authService.updateUserRole(id, request);
        return ResponseEntity.ok(user);
    }
}
