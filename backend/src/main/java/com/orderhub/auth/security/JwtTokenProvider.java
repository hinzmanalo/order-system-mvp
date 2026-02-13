package com.orderhub.auth.security;

import com.orderhub.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token provider for generating and validating JWT tokens.
 * <p>
 * Handles the creation of access tokens and refresh tokens, as well as
 * token validation and claim extraction. Uses HMAC-SHA256 for token signing.
 * </p>
 * <p>
 * Thread-safety: This class is thread-safe due to the immutable secret key
 * and thread-safe SecureRandom instance.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final SecureRandom secureRandom;

    /**
     * Constructs a JwtTokenProvider with the specified configuration.
     *
     * @param secret                the secret key for signing JWT tokens, must be
     *                              at least 256 bits
     * @param accessTokenValidityMs the access token validity duration in
     *                              milliseconds (default: 15 minutes)
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity-ms:900000}") long accessTokenValidityMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.secureRandom = new SecureRandom();

        logger.info("JwtTokenProvider initialized with access token validity: {} ms", accessTokenValidityMs);
    }

    /**
     * Generates an access token for the specified user.
     * <p>
     * The token contains the user's ID as subject, and email and role as claims.
     * </p>
     *
     * @param user the user for whom to generate the token, must not be null
     * @return the generated JWT access token string
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityMs);

        logger.debug("Generating access token for user ID: {}", user.getId());

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a cryptographically secure random refresh token.
     * <p>
     * The token is 48 bytes of random data encoded in URL-safe Base64.
     * </p>
     *
     * @return the generated refresh token string
     */
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);

        logger.debug("Generated new refresh token");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Validates the specified JWT token.
     * <p>
     * Checks the token signature, format, and expiration.
     * </p>
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            logger.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.warn("Invalid JWT token format: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.debug("JWT token has expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts the user ID from the specified JWT token.
     *
     * @param token the JWT token to extract the user ID from
     * @return the user ID as a UUID
     * @throws JwtException if the token is invalid
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        logger.debug("Extracted user ID from token");
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the email address from the specified JWT token.
     *
     * @param token the JWT token to extract the email from
     * @return the email address string
     * @throws JwtException if the token is invalid
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        logger.debug("Extracted email from token");
        return claims.get("email", String.class);
    }
}
