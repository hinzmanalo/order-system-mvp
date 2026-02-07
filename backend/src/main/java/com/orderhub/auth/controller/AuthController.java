package com.orderhub.auth.controller;

import com.orderhub.auth.dto.*;
import com.orderhub.auth.security.JwtTokenProvider;
import com.orderhub.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider tokenProvider) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User information retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        // Extract token from request to get user ID
        org.springframework.web.context.request.RequestAttributes requestAttributes = org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes();
        jakarta.servlet.http.HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes)
                .getRequest();

        String bearerToken = request.getHeader("Authorization");
        String jwt = bearerToken.substring(7);
        UUID userId = tokenProvider.getUserIdFromToken(jwt);

        UserResponse response = authService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }
}
