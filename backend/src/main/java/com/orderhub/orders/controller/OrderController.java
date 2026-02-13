package com.orderhub.orders.controller;

import com.orderhub.auth.security.JwtTokenProvider;
import com.orderhub.orders.dto.CreateOrderRequest;
import com.orderhub.orders.dto.OrderResponse;
import com.orderhub.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST controller for customer order operations.
 * <p>
 * Provides endpoints for creating, viewing, and cancelling orders.
 * All endpoints require authentication. Users can only access their own orders.
 * </p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Customer order management endpoints")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final JwtTokenProvider tokenProvider;

    public OrderController(OrderService orderService, JwtTokenProvider tokenProvider) {
        this.orderService = orderService;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Creates a new order for the authenticated user.
     * <p>
     * This is an atomic operation that validates products, decrements inventory,
     * and creates an order with status CONFIRMED.
     * </p>
     *
     * @param request        the order creation request
     * @param servletRequest the HTTP servlet request (for JWT extraction)
     * @return the created order
     */
    @Operation(summary = "Create order", description = "Creates a new order with the specified items. Inventory is decremented atomically.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock or product inactive", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest servletRequest) {

        UUID userId = extractUserIdFromRequest(servletRequest);

        logger.info("POST /api/v1/orders - Creating order for user {}", userId);

        OrderResponse response = orderService.createOrder(userId, request);

        logger.info("Order created successfully: orderId={}, totalAmount={}",
                response.getId(), response.getTotalAmount());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all orders for the authenticated user with optional filters.
     *
     * @param status         optional status filter (CONFIRMED, PAID, CANCELLED)
     * @param createdAfter   optional minimum created date
     * @param createdBefore  optional maximum created date
     * @param pageable       pagination parameters
     * @param servletRequest the HTTP servlet request (for JWT extraction)
     * @return paginated list of user's orders
     */
    @Operation(summary = "Get user orders", description = "Retrieves all orders for the authenticated user with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @Parameter(description = "Filter by order status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by minimum created date") @RequestParam(required = false) LocalDateTime createdAfter,
            @Parameter(description = "Filter by maximum created date") @RequestParam(required = false) LocalDateTime createdBefore,
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 10) Pageable pageable,
            HttpServletRequest servletRequest) {

        UUID userId = extractUserIdFromRequest(servletRequest);

        logger.debug("GET /api/v1/orders - Retrieving orders for user {} with filters: status={}, after={}, before={}",
                userId, status, createdAfter, createdBefore);

        Page<OrderResponse> orders = orderService.getUserOrders(
                userId, status, createdAfter, createdBefore, pageable);

        logger.info("Retrieved {} orders for user {}", orders.getTotalElements(), userId);

        return ResponseEntity.ok(orders);
    }

    /**
     * Retrieves a specific order by ID.
     * User can only view their own orders.
     *
     * @param id             the order ID
     * @param servletRequest the HTTP servlet request (for JWT extraction)
     * @return the order details
     */
    @Operation(summary = "Get order by ID", description = "Retrieves detailed information for a specific order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found or access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID id,
            HttpServletRequest servletRequest) {

        UUID userId = extractUserIdFromRequest(servletRequest);

        logger.debug("GET /api/v1/orders/{} - User {}", id, userId);

        OrderResponse response = orderService.getOrderById(id, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an order.
     * Only CONFIRMED orders can be cancelled. Inventory is restored.
     *
     * @param id             the order ID
     * @param servletRequest the HTTP servlet request (for JWT extraction)
     * @return the cancelled order
     */
    @Operation(summary = "Cancel order", description = "Cancels a CONFIRMED order and restores inventory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Order not found or access denied", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order cannot be cancelled (already PAID or CANCELLED)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID id,
            HttpServletRequest servletRequest) {

        UUID userId = extractUserIdFromRequest(servletRequest);

        logger.info("POST /api/v1/orders/{}/cancel - User {}", id, userId);

        OrderResponse response = orderService.cancelOrder(id, userId);

        logger.info("Order {} cancelled successfully by user {}", id, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Extracts the user ID from the JWT token in the request.
     *
     * @param request the HTTP servlet request
     * @return the user ID
     */
    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        String jwt = bearerToken.substring(7); // Remove "Bearer " prefix
        return tokenProvider.getUserIdFromToken(jwt);
    }
}
