package com.orderhub.orders.dto;

import com.orderhub.orders.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for an order.
 * <p>
 * Contains complete order information including items, status, and totals.
 * Used for both customer and admin endpoints.
 * </p>
 *
 * @since 1.0.0
 */
public class OrderResponse {

    private UUID id;
    private UUID userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Default constructor.
     */
    public OrderResponse() {
    }

    /**
     * Creates an order response.
     *
     * @param id          the order ID
     * @param userId      the user ID who placed the order
     * @param status      the current order status
     * @param totalAmount the total order amount
     * @param items       the list of order items
     * @param createdAt   when the order was created
     * @param updatedAt   when the order was last updated
     */
    public OrderResponse(UUID id, UUID userId, OrderStatus status, BigDecimal totalAmount,
            List<OrderItemResponse> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
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

    @Override
    public String toString() {
        return "OrderResponse{" +
                "id=" + id +
                ", userId=" + userId +
                ", status=" + status +
                ", totalAmount=" + totalAmount +
                ", items=" + items.size() +
                ", createdAt=" + createdAt +
                '}';
    }
}
