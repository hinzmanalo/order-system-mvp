package com.orderhub.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a product.
 * <p>
 * All fields are required except description. Price must be positive.
 * </p>
 *
 * @since 1.0.0
 */
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotBlank(message = "SKU is required")
    private String sku;

    /**
     * Default constructor.
     */
    public ProductRequest() {
    }

    /**
     * Creates a product request with the specified attributes.
     *
     * @param name        the product name
     * @param description the product description (optional)
     * @param price       the product price
     * @param sku         the stock keeping unit
     */
    public ProductRequest(String name, String description, BigDecimal price, String sku) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.sku = sku;
    }

    // Getters and Setters

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
}
