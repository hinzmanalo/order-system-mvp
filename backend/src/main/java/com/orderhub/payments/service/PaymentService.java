package com.orderhub.payments.service;

import com.orderhub.payments.dto.PaymentRequest;
import com.orderhub.payments.dto.PaymentResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for payment processing operations.
 * <p>
 * Handles payment processing with idempotency guarantees, gateway integration,
 * and order status transitions. Ensures payments are processed exactly once
 * per idempotency key.
 * </p>
 *
 * @since 1.0.0
 */
public interface PaymentService {

    /**
     * Processes a payment for an order.
     * <p>
     * This is an idempotent operation - using the same idempotency key will
     * return the original result without reprocessing. The payment amount
     * must exactly match the order total.
     * </p>
     * <p>
     * Processing steps:
     * </p>
     * <ol>
     * <li>Check idempotency key for existing payment</li>
     * <li>Validate order ownership and status (must be CONFIRMED)</li>
     * <li>Validate payment amount matches order total</li>
     * <li>Process payment through gateway</li>
     * <li>Save payment record</li>
     * <li>Update order status to PAID if successful</li>
     * </ol>
     *
     * @param orderId        the order ID to pay for
     * @param userId         the user ID making the payment
     * @param idempotencyKey unique key to prevent duplicate processing
     * @param request        the payment request with amount
     * @return the payment result
     * @throws com.orderhub.common.exception.ResourceNotFoundException      if
     *                                                                      order
     *                                                                      not
     *                                                                      found
     *                                                                      or
     *                                                                      doesn't
     *                                                                      belong
     *                                                                      to
     *                                                                      user
     * @throws com.orderhub.common.exception.InvalidOrderStateException     if
     *                                                                      order
     *                                                                      is
     *                                                                      not
     *                                                                      in
     *                                                                      CONFIRMED
     *                                                                      status
     * @throws com.orderhub.common.exception.PaymentAmountMismatchException if
     *                                                                      payment
     *                                                                      amount
     *                                                                      doesn't
     *                                                                      match
     *                                                                      order
     *                                                                      total
     */
    PaymentResponse processPayment(UUID orderId, UUID userId, String idempotencyKey, PaymentRequest request);

    /**
     * Retrieves all payments for a specific order.
     * <p>
     * Users can only view payments for their own orders.
     * Returns both successful and failed payment attempts.
     * </p>
     *
     * @param orderId the order ID
     * @param userId  the user ID
     * @return list of payments for the order
     * @throws com.orderhub.common.exception.ResourceNotFoundException if order not
     *                                                                 found or
     *                                                                 doesn't
     *                                                                 belong to
     *                                                                 user
     */
    List<PaymentResponse> getPaymentsByOrderId(UUID orderId, UUID userId);
}
