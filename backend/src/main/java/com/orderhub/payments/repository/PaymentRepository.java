package com.orderhub.payments.repository;

import com.orderhub.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Payment entity data access.
 * <p>
 * Provides methods for querying payments by idempotency key and order ID.
 * The idempotency key lookup enables duplicate payment prevention.
 * </p>
 *
 * @since 1.0.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Finds a payment by its unique idempotency key.
     * <p>
     * Used to check if a payment has already been processed with the same
     * idempotency key, preventing duplicate transactions.
     * </p>
     *
     * @param idempotencyKey the unique idempotency key
     * @return an Optional containing the payment if found
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * Retrieves all payments associated with a specific order.
     * <p>
     * Returns payment history for an order, including both successful
     * and failed payment attempts.
     * </p>
     *
     * @param orderId the order UUID
     * @return list of payments for the order, empty if none found
     */
    List<Payment> findByOrderId(UUID orderId);
}
