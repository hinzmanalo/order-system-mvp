package com.orderhub.inventory.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adjusting stock by a relative amount.
 * Adjustment can be positive (adding stock) or negative (removing stock).
 * The resulting quantity must not go below zero.
 *
 * @since 1.0.0
 */
public class AdjustStockRequest {

    @NotNull(message = "Adjustment value is required")
    private Integer adjustment;

    /**
     * Default constructor.
     */
    public AdjustStockRequest() {
    }

    /**
     * Creates a new adjust stock request.
     *
     * @param adjustment the quantity to add (positive) or remove (negative)
     */
    public AdjustStockRequest(Integer adjustment) {
        this.adjustment = adjustment;
    }

    // Getters and Setters

    public Integer getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(Integer adjustment) {
        this.adjustment = adjustment;
    }
}
