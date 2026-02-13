package com.orderhub.inventory.entity;

import com.orderhub.catalog.entity.Product;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing inventory stock levels for products.
 * <p>
 * Uses optimistic locking via @Version to prevent overselling in concurrent
 * scenarios.
 * Each product has exactly one inventory record tracking its available
 * quantity.
 * </p>
 * <p>
 * Thread-safety: Optimistic locking prevents concurrent modification conflicts.
 * The version field is automatically incremented on each update.
 * </p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", unique = true, nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity = 0;

    @Version
    @Column(nullable = false)
    private int version = 0;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Default constructor for JPA.
     */
    public Inventory() {
    }

    /**
     * Creates a new inventory record for a product.
     *
     * @param product  the product this inventory tracks, must not be null
     * @param quantity the initial quantity, must be non-negative
     */
    public Inventory(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "id=" + id +
                ", productId=" + (product != null ? product.getId() : null) +
                ", quantity=" + quantity +
                ", version=" + version +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
