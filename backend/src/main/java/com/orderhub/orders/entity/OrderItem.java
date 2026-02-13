package com.orderhub.orders.entity;

import com.orderhub.catalog.entity.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing a single line item within an order.
 * <p>
 * OrderItems capture a snapshot of product details at the time of purchase,
 * including the price and quantity ordered. This ensures historical order data
 * remains accurate even if product prices change later.
 * </p>
 * <p>
 * Subtotal is calculated as unitPrice × quantity.
 * </p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    /**
     * Default constructor for JPA.
     */
    public OrderItem() {
    }

    /**
     * Creates a new order item.
     *
     * @param product   the product being ordered
     * @param quantity  the quantity ordered
     * @param unitPrice the unit price at time of order
     * @param subtotal  the calculated subtotal (unitPrice × quantity)
     */
    public OrderItem(Product product, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
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

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OrderItem))
            return false;
        OrderItem orderItem = (OrderItem) o;
        return id != null && id.equals(orderItem.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                '}';
    }
}
