package com.orderhub.common.exception;

/**
 * Exception thrown when there is insufficient stock to fulfill an order.
 */
public class InsufficientStockException extends RuntimeException {

    private final String productName;
    private final int available;
    private final int requested;

    public InsufficientStockException(String productName, int available, int requested) {
        super(String.format("Insufficient stock for %s: requested %d, available %d",
                productName, requested, available));
        this.productName = productName;
        this.available = available;
        this.requested = requested;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
