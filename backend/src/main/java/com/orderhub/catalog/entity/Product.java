package com.orderhub.catalog.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a product in the catalog.
 * <p>
 * Products have SKU-based uniqueness enforcement and use optimistic locking
 * to prevent concurrent modification conflicts. Each product tracks active
 * status
 * to support soft deletion without removing historical order data.
 * </p>
 * <p>
 * Thread-safety: Entity state should be managed within transaction boundaries.
 * </p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(unique = true, nullable = false, length = 50)
    private String sku;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private int version = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Default constructor for JPA.
     */
    public Product() {
    }

    /**
     * Creates a new product with the specified attributes.
     *
     * @param name        the product name, must not be null
     * @param description optional product description
     * @param price       the product price, must be positive
     * @param sku         the stock keeping unit, must be unique
     */
    public Product(String name, String description, BigDecimal price, String sku) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.sku = sku;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
