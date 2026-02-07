package com.orderhub.auth.repository;

import com.orderhub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for User entities.
 * <p>
 * Provides CRUD operations and custom query methods for user management.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the user if found, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user exists with the specified email address.
     *
     * @param email the email address to check
     * @return true if a user exists with this email, false otherwise
     */
    boolean existsByEmail(String email);
}
