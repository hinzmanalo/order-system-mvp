package com.orderhub.auth.repository;

import com.orderhub.auth.entity.RefreshToken;
import com.orderhub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for RefreshToken entities.
 * <p>
 * Provides CRUD operations and custom query methods for refresh token
 * management,
 * including token lookup, user-based deletion, and expired token cleanup.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Finds a refresh token by its token string value.
     *
     * @param token the refresh token string to search for
     * @return an Optional containing the RefreshToken if found, or empty if not
     *         found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes all refresh tokens associated with a specific user.
     * <p>
     * Useful for logout-all-devices functionality or user account deletion.
     * </p>
     *
     * @param user the user whose refresh tokens should be deleted
     */
    void deleteByUser(User user);

    /**
     * Deletes all refresh tokens that have expired before the specified time.
     * <p>
     * Used for periodic cleanup of expired tokens to prevent database bloat.
     * </p>
     *
     * @param now the reference time to compare against token expiration times
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}
