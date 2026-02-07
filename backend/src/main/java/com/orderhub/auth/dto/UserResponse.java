package com.orderhub.auth.dto;

import com.orderhub.auth.entity.Role;
import com.orderhub.auth.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user information.
 * <p>
 * Used to transfer user data to clients without exposing sensitive
 * information like password hashes. Includes a factory method for
 * converting from the User entity.
 * </p>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private LocalDateTime createdAt;

    // Constructors
    public UserResponse() {
    }

    public UserResponse(UUID id, String email, String firstName, String lastName, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getCreatedAt());
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
