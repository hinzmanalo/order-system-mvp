package com.orderhub.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for setting absolute stock quantity.
 * Sets the inventory to an exact value, regardless of current stock.
 *
 * @since 1.0.0
 */
public class SetStockRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer quantity;

    /**
     * Default constructor.
     */
    public SetStockRequest() {
    }

    /**
     * Creates a new set stock request.
     *
     * @param quantity the absolute quantity to set, must be non-negative
     */
    public SetStockRequest(Integer quantity) {
        this.quantity = quantity;
    }

    // Getters and Setters

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
