package com.orderhub.auth.repository;

import com.orderhub.auth.entity.RefreshToken;
import com.orderhub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Delete all refresh tokens for a user
     */
    void deleteByUser(User user);

    /**
     * Delete expired refresh tokens
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}
