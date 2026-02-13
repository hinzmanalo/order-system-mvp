package com.orderhub.orders.controller;

import com.orderhub.orders.dto.OrderResponse;
import com.orderhub.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST controller for admin order operations.
 * <p>
 * Provides endpoints for viewing and managing orders across all users.
 * All endpoints require ADMIN role.
 * </p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Orders", description = "Admin order management endpoints")
public class AdminOrderController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Retrieves all orders across all users with optional filters.
     *
     * @param status        optional status filter (CONFIRMED, PAID, CANCELLED)
     * @param createdAfter  optional minimum created date
     * @param createdBefore optional maximum created date
     * @param pageable      pagination parameters
     * @return paginated list of all orders
     */
    @Operation(summary = "Get all orders (Admin)", description = "Retrieves all orders across all users with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @Parameter(description = "Filter by order status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by minimum created date") @RequestParam(required = false) LocalDateTime createdAfter,
            @Parameter(description = "Filter by maximum created date") @RequestParam(required = false) LocalDateTime createdBefore,
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 10) Pageable pageable) {

        logger.debug("GET /api/v1/admin/orders - Retrieving all orders with filters: status={}, after={}, before={}",
                status, createdAfter, createdBefore);

        Page<OrderResponse> orders = orderService.getAllOrders(
                status, createdAfter, createdBefore, pageable);

        logger.info("Admin retrieved {} total orders", orders.getTotalElements());

        return ResponseEntity.ok(orders);
    }

    /**
     * Retrieves any order by ID (admin operation).
     *
     * @param id the order ID
     * @return the order details
     */
    @Operation(summary = "Get any order by ID (Admin)", description = "Retrieves detailed information for any order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID id) {

        logger.debug("GET /api/v1/admin/orders/{}", id);

        OrderResponse response = orderService.getAnyOrderById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels any user's order (admin operation).
     * Only CONFIRMED orders can be cancelled. Inventory is restored.
     *
     * @param id the order ID
     * @return the cancelled order
     */
    @Operation(summary = "Cancel any order (Admin)", description = "Cancels any CONFIRMED order and restores inventory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order cannot be cancelled (already PAID or CANCELLED)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID id) {

        logger.info("POST /api/v1/admin/orders/{}/cancel", id);

        OrderResponse response = orderService.cancelAnyOrder(id);

        logger.info("Admin cancelled order {} successfully", id);

        return ResponseEntity.ok(response);
    }
}
