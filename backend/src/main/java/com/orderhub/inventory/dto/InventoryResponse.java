package com.orderhub.inventory.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for inventory information.
 * Includes product details for simplified frontend consumption.
 *
 * @since 1.0.0
 */
public class InventoryResponse {

    private UUID productId;
    private String productName;
    private String productSku;
    private int quantity;
    private LocalDateTime updatedAt;

    /**
     * Default constructor.
     */
    public InventoryResponse() {
    }

    /**
     * Creates a new inventory response.
     *
     * @param productId   the product ID
     * @param productName the product name
     * @param productSku  the product SKU
     * @param quantity    the available quantity
     * @param updatedAt   the last update timestamp
     */
    public InventoryResponse(UUID productId, String productName, String productSku,
            int quantity, LocalDateTime updatedAt) {
        this.productId = productId;
        this.productName = productName;
        this.productSku = productSku;
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
