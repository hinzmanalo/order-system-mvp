package com.orderhub.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing access tokens.
 * <p>
 * Contains the refresh token that will be exchanged for a new
 * access token and refresh token pair (token rotation).
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    // Constructors
    public RefreshRequest() {
    }

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
