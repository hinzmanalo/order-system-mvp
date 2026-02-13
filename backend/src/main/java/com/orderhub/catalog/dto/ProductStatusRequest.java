package com.orderhub.catalog.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a product's active status.
 * <p>
 * Used to activate or deactivate products without modifying other attributes.
 * </p>
 *
 * @since 1.0.0
 */
public class ProductStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    /**
     * Default constructor.
     */
    public ProductStatusRequest() {
    }

    /**
     * Creates a status request with the specified active flag.
     *
     * @param active whether the product should be active
     */
    public ProductStatusRequest(Boolean active) {
        this.active = active;
    }

    // Getters and Setters

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
