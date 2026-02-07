package com.orderhub.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login authentication.
 * <p>
 * Contains the credentials required for user authentication.
 * Both email and password are required fields.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class LoginRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // Constructors
    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
