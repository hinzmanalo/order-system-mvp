package com.orderhub.auth.controller;

import com.orderhub.auth.dto.*;
import com.orderhub.auth.security.JwtTokenProvider;
import com.orderhub.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for authentication endpoints.
 * <p>
 * Provides endpoints for user registration, login, token refresh,
 * and retrieving current user information. All endpoints are under
 * the /api/v1/auth path.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    /**
     * Constructs an AuthController with required dependencies.
     *
     * @param authService   the authentication service for business logic
     * @param tokenProvider the JWT token provider for token extraction
     */
    public AuthController(AuthService authService, JwtTokenProvider tokenProvider) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Registers a new user in the system.
     *
     * @param request the registration request containing user details
     * @return ResponseEntity with created user information and HTTP 201 status
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Received registration request for email: {}", request.getEmail());

        UserResponse response = authService.register(request);

        logger.info("Registration completed successfully for user ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns access and refresh tokens.
     *
     * @param request the login credentials
     * @return ResponseEntity with token information
     */
    @PostMapping("/login")
    @Operation(summary = "Login user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for email: {}", request.getEmail());

        TokenResponse response = authService.login(request);

        logger.info("Login successful for email: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request the refresh request containing the refresh token
     * @return ResponseEntity with new token information
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        logger.debug("Token refresh request received");

        TokenResponse response = authService.refresh(request);

        logger.debug("Token refresh successful");
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the current authenticated user's information.
     *
     * @return ResponseEntity with current user information
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User information retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Extract user ID from JWT token in request header
        org.springframework.web.context.request.RequestAttributes requestAttributes = org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes();
        jakarta.servlet.http.HttpServletRequest request = ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes)
                .getRequest();

        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for /me endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String jwt = bearerToken.substring(7);
        UUID userId = tokenProvider.getUserIdFromToken(jwt);

        logger.debug("Retrieving current user information for user ID: {}", userId);

        UserResponse response = authService.getCurrentUser(userId);

        logger.debug("Current user information retrieved for user ID: {}", userId);
        return ResponseEntity.ok(response);
    }
}
