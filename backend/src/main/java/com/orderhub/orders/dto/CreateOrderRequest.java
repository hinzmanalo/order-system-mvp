package com.orderhub.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for creating a new order.
 * <p>
 * Contains a list of order items. The order must contain at least one item.
 * Each item is validated according to {@link OrderItemRequest} constraints.
 * </p>
 *
 * @since 1.0.0
 */
public class CreateOrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items = new ArrayList<>();

    /**
     * Default constructor.
     */
    public CreateOrderRequest() {
    }

    /**
     * Creates an order request with the specified items.
     *
     * @param items the list of items to order
     */
    public CreateOrderRequest(List<OrderItemRequest> items) {
        this.items = items;
    }

    // Getters and Setters

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "CreateOrderRequest{" +
                "items=" + items +
                '}';
    }
}
