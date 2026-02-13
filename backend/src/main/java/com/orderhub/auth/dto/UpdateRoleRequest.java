package com.orderhub.auth.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a user's role.
 * <p>
 * Used by administrators to change user roles. The role field should
 * contain a valid role name (e.g., "USER" or "ADMIN").
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class UpdateRoleRequest {

    @NotNull(message = "Role is required")
    private String role;

    // Constructors
    public UpdateRoleRequest() {
    }

    public UpdateRoleRequest(String role) {
        this.role = role;
    }

    // Getters and Setters
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
