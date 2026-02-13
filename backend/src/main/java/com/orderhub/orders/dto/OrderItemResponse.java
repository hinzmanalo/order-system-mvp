package com.orderhub.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for an order item.
 * <p>
 * Contains snapshot information about a product at the time of order,
 * including the price paid and quantity ordered.
 * </p>
 *
 * @since 1.0.0
 */
public class OrderItemResponse {

    private UUID productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    /**
     * Default constructor.
     */
    public OrderItemResponse() {
    }

    /**
     * Creates an order item response.
     *
     * @param productId   the product ID
     * @param productName the product name at time of order
     * @param quantity    the quantity ordered
     * @param unitPrice   the unit price at time of order
     * @param subtotal    the calculated subtotal
     */
    public OrderItemResponse(UUID productId, String productName, int quantity,
                             BigDecimal unitPrice, BigDecimal subtotal) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
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
    public String toString() {
        return "OrderItemResponse{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                '}';
    }
}
