package com.orderhub.payments.controller;

import com.orderhub.auth.security.JwtTokenProvider;
import com.orderhub.payments.dto.PaymentRequest;
import com.orderhub.payments.dto.PaymentResponse;
import com.orderhub.payments.service.PaymentService;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for payment processing operations.
 * <p>
 * Provides endpoints for processing payments and viewing payment history.
 * All endpoints require authentication and the Idempotency-Key header
 * for payment processing.
 * </p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/orders/{orderId}/payments")
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final JwtTokenProvider tokenProvider;

    public PaymentController(PaymentService paymentService, JwtTokenProvider tokenProvider) {
        this.paymentService = paymentService;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Processes a payment for an order.
     * <p>
     * Requires the Idempotency-Key header to prevent duplicate processing.
     * The payment amount must match the order total exactly.
     * </p>
     *
     * @param orderId        the order ID to pay for
     * @param idempotencyKey unique key to prevent duplicate processing
     * @param request        the payment request with amount
     * @param servletRequest the HTTP servlet request (for JWT extraction)
     * @return the payment result
     */
    @Operation(summary = "Process payment", description = "Processes a payment for an order. Requires Idempotency-Key header. Payment amount must match order total.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or missing Idempotency-Key header", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Order not in CONFIRMED status", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Payment amount does not match order total", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId,

            @Parameter(description = "Idempotency key to prevent duplicate processing", required = true) @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,

            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest servletRequest) {

        UUID userId = extractUserIdFromRequest(servletRequest);

        logger.info("POST /api/v1/orders/{}/payments - Processing payment for user {} with idempotency key {}",
                orderId, userId, idempotencyKey);

        // Validate Idempotency-Key header is present (required = true handles this)
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            logger.warn("Missing or empty Idempotency-Key header for payment request");
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        PaymentResponse response = paymentService.processPayment(orderId, userId, idempotencyKey, request);

        logger.info("Payment processed: paymentId={}, status={}, orderId={}",
                response.getId(), response.getStatus(), orderId);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all payments for a specific order.
     * <p>
     * Returns payment history including both successful and failed attempts.
     * Users can only view payments for their own orders.
     * </p>
     *
     * @param orderId        the order ID
     * @param servletRequest the HTTP servlet request (for JWT extraction)
     * @return list of payments for the order
     */
    @Operation(summary = "Get order payments", description = "Retrieves all payment attempts for an order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payments retrieved successfully", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getOrderPayments(
            @Parameter(description = "Order ID", required = true) @PathVariable UUID orderId,
            HttpServletRequest servletRequest) {

        UUID userId = extractUserIdFromRequest(servletRequest);

        logger.info("GET /api/v1/orders/{}/payments - Retrieving payments for user {}", orderId, userId);

        List<PaymentResponse> payments = paymentService.getPaymentsByOrderId(orderId, userId);

        logger.info("Retrieved {} payment(s) for order {}", payments.size(), orderId);

        return ResponseEntity.ok(payments);
    }

    /**
     * Extracts the user ID from the JWT token in the request.
     *
     * @param request the HTTP servlet request
     * @return the user ID from the JWT token
     */
    private UUID extractUserIdFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        String jwt = bearerToken.substring(7); // Remove "Bearer " prefix
        return tokenProvider.getUserIdFromToken(jwt);
    }
}
