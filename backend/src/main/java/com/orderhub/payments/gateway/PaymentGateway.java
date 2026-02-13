package com.orderhub.payments.gateway;

import java.math.BigDecimal;

/**
 * Strategy interface for payment gateway implementations.
 * <p>
 * This abstraction allows swapping between different payment providers
 * (Stripe, PayPal, etc.) without changing business logic. Implementations
 * should handle communication with external payment systems.
 * </p>
 * <p>
 * Thread-safety: Implementations should be thread-safe as they will be
 * used as Spring beans.
 * </p>
 *
 * @since 1.0.0
 */
public interface PaymentGateway {

    /**
     * Processes a payment transaction through the gateway.
     * <p>
     * This method communicates with the external payment provider to
     * charge the specified amount. The referenceId can be used for
     * tracking and reconciliation.
     * </p>
     *
     * @param amount      the amount to charge, must be positive
     * @param referenceId a reference identifier for tracking (e.g., order ID)
     * @return the result of the payment processing attempt
     * @throws IllegalArgumentException if amount is null or not positive
     */
    PaymentGatewayResult processPayment(BigDecimal amount, String referenceId);
}
