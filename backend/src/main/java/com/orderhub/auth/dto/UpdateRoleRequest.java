package com.orderhub.auth.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating user role
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
