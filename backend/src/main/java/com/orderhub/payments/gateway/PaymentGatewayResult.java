package com.orderhub.payments.gateway;

/**
 * Result object returned by payment gateway implementations.
 * <p>
 * Encapsulates the outcome of a payment processing attempt, including
 * success status and optional gateway reference number for successful
 * transactions.
 * </p>
 *
 * @param success          true if payment was processed successfully
 * @param gatewayReference unique reference from the payment gateway (null on
 *                         failure)
 * @since 1.0.0
 */
public record PaymentGatewayResult(boolean success, String gatewayReference) {

    /**
     * Creates a result for a successful payment.
     *
     * @param gatewayReference the gateway transaction reference
     * @return a successful payment result
     */
    public static PaymentGatewayResult success(String gatewayReference) {
        return new PaymentGatewayResult(true, gatewayReference);
    }

    /**
     * Creates a result for a failed payment.
     *
     * @return a failed payment result with no gateway reference
     */
    public static PaymentGatewayResult failure() {
        return new PaymentGatewayResult(false, null);
    }
}
