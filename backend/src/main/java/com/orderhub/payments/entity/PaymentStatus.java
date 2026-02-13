package com.orderhub.payments.entity;

/**
 * Enumeration representing the possible states of a payment transaction.
 * <p>
 * Payment transactions are terminal - once a payment reaches SUCCESS or FAILED
 * status, it cannot be modified. Failed payments require a new idempotency key
 * to retry.
 * </p>
 *
 * @since 1.0.0
 */
public enum PaymentStatus {
    /**
     * Payment was processed successfully by the payment gateway.
     * Order status transitions to PAID when payment succeeds.
     */
    SUCCESS,

    /**
     * Payment processing failed (gateway rejection, network error, etc.).
     * Order remains in CONFIRMED status and customer can retry with a new
     * idempotency key.
     */
    FAILED
}
