package com.orderhub.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for product data.
 * <p>
 * Contains all product information exposed to clients.
 * </p>
 *
 * @since 1.0.0
 */
public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String sku;
    private boolean active;
    private LocalDateTime createdAt;

    /**
     * Default constructor.
     */
    public ProductResponse() {
    }

    /**
     * Creates a product response with all attributes.
     *
     * @param id          the product ID
     * @param name        the product name
     * @param description the product description
     * @param price       the product price
     * @param sku         the stock keeping unit
     * @param active      whether the product is active
     * @param createdAt   when the product was created
     */
    public ProductResponse(UUID id, String name, String description, BigDecimal price,
            String sku, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.sku = sku;
        this.active = active;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
