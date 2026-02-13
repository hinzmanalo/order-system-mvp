package com.orderhub.orders.repository;

import com.orderhub.orders.entity.Order;
import com.orderhub.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Order entity.
 * <p>
 * Provides data access methods for orders with filtering by user, status,
 * and date range. Supports both customer-specific and admin-wide queries.
 * </p>
 *
 * @since 1.0.0
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Finds all orders for a specific user, paginated.
     *
     * @param userId   the user ID
     * @param pageable pagination parameters
     * @return paginated list of orders
     */
    Page<Order> findByUserId(UUID userId, Pageable pageable);

    /**
     * Finds a specific order by ID for a specific user.
     * Used to ensure users can only access their own orders.
     *
     * @param id     the order ID
     * @param userId the user ID
     * @return optional containing the order if found
     */
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Finds orders for a specific user with optional filters.
     *
     * @param userId the user ID
     * @param status optional order status filter
     * @param after  optional minimum date (inclusive)
     * @param before optional maximum date (inclusive)
     * @param pageable pagination parameters
     * @return paginated list of orders matching the criteria
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:after IS NULL OR o.createdAt >= :after) " +
            "AND (:before IS NULL OR o.createdAt <= :before)")
    Page<Order> findByUserIdWithFilters(
            @Param("userId") UUID userId,
            @Param("status") OrderStatus status,
            @Param("after") LocalDateTime after,
            @Param("before") LocalDateTime before,
            Pageable pageable
    );

    /**
     * Finds all orders across all users with optional filters.
     * Used for admin operations.
     *
     * @param status   optional order status filter
     * @param after    optional minimum date (inclusive)
     * @param before   optional maximum date (inclusive)
     * @param pageable pagination parameters
     * @return paginated list of orders matching the criteria
     */
    @Query("SELECT o FROM Order o WHERE " +
            "(:status IS NULL OR o.status = :status) " +
            "AND (:after IS NULL OR o.createdAt >= :after) " +
            "AND (:before IS NULL OR o.createdAt <= :before)")
    Page<Order> findAllWithFilters(
            @Param("status") OrderStatus status,
            @Param("after") LocalDateTime after,
            @Param("before") LocalDateTime before,
            Pageable pageable
    );
}
