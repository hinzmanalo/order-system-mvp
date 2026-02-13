package com.orderhub.orders.service;

import com.orderhub.orders.dto.CreateOrderRequest;
import com.orderhub.orders.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service interface for order management operations.
 * <p>
 * Handles order creation, retrieval, cancellation, and status updates.
 * Supports both customer-facing and admin operations.
 * Order creation is atomic with inventory decrementation.
 * </p>
 *
 * @since 1.0.0
 */
public interface OrderService {

    /**
     * Creates a new order for the specified user.
     * <p>
     * This is an atomic operation that:
     * </p>
     * <ol>
     *     <li>Validates all products exist and are active</li>
     *     <li>Decrements inventory for each item</li>
     *     <li>Creates the order with status CONFIRMED</li>
     * </ol>
     * <p>
     * If any step fails, the entire transaction rolls back.
     * </p>
     *
     * @param userId  the user ID placing the order
     * @param request the order request with items
     * @return the created order
     * @throws com.orderhub.common.exception.ResourceNotFoundException  if user or
     *                                                                  product not
     *                                                                  found
     * @throws com.orderhub.common.exception.InvalidOrderStateException if product
     *                                                                  is inactive
     * @throws com.orderhub.common.exception.InsufficientStockException if
     *                                                                  insufficient
     *                                                                  stock for
     *                                                                  any item
     */
    OrderResponse createOrder(UUID userId, CreateOrderRequest request);

    /**
     * Retrieves a specific order for a user.
     * Users can only view their own orders.
     *
     * @param orderId the order ID
     * @param userId  the user ID
     * @return the order
     * @throws com.orderhub.common.exception.ResourceNotFoundException if order not
     *                                                                 found or
     *                                                                 doesn't
     *                                                                 belong to
     *                                                                 user
     */
    OrderResponse getOrderById(UUID orderId, UUID userId);

    /**
     * Retrieves all orders for a specific user with optional filters.
     *
     * @param userId   the user ID
     * @param status   optional status filter (CONFIRMED, PAID, CANCELLED)
     * @param after    optional minimum created date (inclusive)
     * @param before   optional maximum created date (inclusive)
     * @param pageable pagination parameters
     * @return paginated list of orders
     */
    Page<OrderResponse> getUserOrders(UUID userId, String status,
                                      LocalDateTime after, LocalDateTime before,
                                      Pageable pageable);

    /**
     * Cancels a user's order.
     * <p>
     * Only CONFIRMED orders can be cancelled. PAID orders cannot be cancelled.
     * Cancellation restores inventory for all items.
     * </p>
     *
     * @param orderId the order ID
     * @param userId  the user ID
     * @return the cancelled order
     * @throws com.orderhub.common.exception.ResourceNotFoundException  if order not
     *                                                                  found or
     *                                                                  doesn't
     *                                                                  belong to
     *                                                                  user
     * @throws com.orderhub.common.exception.InvalidOrderStateException if order is
     *                                                                  not in
     *                                                                  CONFIRMED
     *                                                                  status
     */
    OrderResponse cancelOrder(UUID orderId, UUID userId);

    /**
     * Retrieves all orders across all users (admin operation).
     *
     * @param status   optional status filter
     * @param after    optional minimum created date (inclusive)
     * @param before   optional maximum created date (inclusive)
     * @param pageable pagination parameters
     * @return paginated list of all orders
     */
    Page<OrderResponse> getAllOrders(String status, LocalDateTime after,
                                     LocalDateTime before, Pageable pageable);

    /**
     * Retrieves any order by ID (admin operation).
     *
     * @param orderId the order ID
     * @return the order
     * @throws com.orderhub.common.exception.ResourceNotFoundException if order not
     *                                                                 found
     */
    OrderResponse getAnyOrderById(UUID orderId);

    /**
     * Cancels any order (admin operation).
     *
     * @param orderId the order ID
     * @return the cancelled order
     * @throws com.orderhub.common.exception.ResourceNotFoundException  if order not
     *                                                                  found
     * @throws com.orderhub.common.exception.InvalidOrderStateException if order is
     *                                                                  not in
     *                                                                  CONFIRMED
     *                                                                  status
     */
    OrderResponse cancelAnyOrder(UUID orderId);

    /**
     * Updates an order status to PAID.
     * <p>
     * Called by the PaymentService after successful payment processing.
     * Only CONFIRMED orders can be marked as PAID.
     * </p>
     *
     * @param orderId the order ID
     * @throws com.orderhub.common.exception.ResourceNotFoundException  if order not
     *                                                                  found
     * @throws com.orderhub.common.exception.InvalidOrderStateException if order is
     *                                                                  not in
     *                                                                  CONFIRMED
     *                                                                  status
     */
    void updateOrderStatusToPaid(UUID orderId);
}
