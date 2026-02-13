package com.orderhub.orders.entity;

/**
 * Enumeration representing the possible states of an order.
 * <p>
 * Order lifecycle transitions:
 * </p>
 * <ul>
 * <li>CONFIRMED → PAID (payment successful)</li>
 * <li>CONFIRMED → CANCELLED (order cancelled before payment)</li>
 * </ul>
 * <p>
 * Once an order reaches PAID or CANCELLED state, it cannot be modified.
 * </p>
 *
 * @since 1.0.0
 */
public enum OrderStatus {
    /**
     * Order has been created and inventory has been reserved.
     * Awaiting payment.
     */
    CONFIRMED,

    /**
     * Payment has been successfully processed.
     * Order is complete and cannot be cancelled.
     */
    PAID,

    /**
     * Order was cancelled before payment.
     * Inventory has been restored.
     */
    CANCELLED
}
