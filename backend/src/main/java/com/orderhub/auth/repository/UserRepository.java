package com.orderhub.auth.repository;

import com.orderhub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists with given email
     */
    boolean existsByEmail(String email);
}
