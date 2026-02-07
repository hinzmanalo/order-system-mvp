package com.orderhub.auth.entity;

/**
 * Enumeration of user roles for role-based access control (RBAC).
 * <p>
 * Defines the available roles in the system:
 * </p>
 * <ul>
 * <li>{@link #USER} - Regular user with standard permissions</li>
 * <li>{@link #ADMIN} - Administrator with full system access</li>
 * </ul>
 *
 * @author OrderHub Team
 * @version 1.0.0
 * @since 1.0.0
 */
public enum Role {
    /**
     * Regular user role with standard permissions for browsing products,
     * placing orders, and viewing own order history.
     */
    USER,

    /**
     * Administrator role with full system access including user management,
     * inventory management, and access to all orders.
     */
    ADMIN
}
