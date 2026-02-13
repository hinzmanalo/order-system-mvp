package com.orderhub.payments.service;

import com.orderhub.common.exception.InvalidOrderStateException;
import com.orderhub.common.exception.PaymentAmountMismatchException;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.orders.entity.Order;
import com.orderhub.orders.entity.OrderStatus;
import com.orderhub.orders.repository.OrderRepository;
import com.orderhub.orders.service.OrderService;
import com.orderhub.payments.dto.PaymentRequest;
import com.orderhub.payments.dto.PaymentResponse;
import com.orderhub.payments.entity.Payment;
import com.orderhub.payments.entity.PaymentStatus;
import com.orderhub.payments.gateway.PaymentGateway;
import com.orderhub.payments.gateway.PaymentGatewayResult;
import com.orderhub.payments.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of PaymentService.
 * <p>
 * Handles payment processing with idempotency guarantees, ensuring each
 * payment is processed exactly once. Integrates with payment gateway and
 * manages order status transitions.
 * </p>
 * <p>
 * Uses BigDecimal.compareTo() for amount comparison to handle scale differences
 * correctly (e.g., 10.00 vs 10.0).
 * </p>
 *
 * @since 1.0.0
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentGateway paymentGateway;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            OrderService orderService,
            PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.paymentGateway = paymentGateway;
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(UUID orderId, UUID userId,
            String idempotencyKey, PaymentRequest request) {
        logger.info("Processing payment for order {} with idempotency key {}", orderId, idempotencyKey);

        // Step 1: Check idempotency - if payment already exists with this key, return
        // it
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            logger.info("Idempotency key {} already used - returning existing payment {} with status {}",
                    idempotencyKey, payment.getId(), payment.getStatus());
            return PaymentResponse.fromEntity(payment);
        }

        // Step 2: Load and validate order
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> {
                    logger.error("Order {} not found for user {}", orderId, userId);
                    return new ResourceNotFoundException("Order", "id", orderId);
                });

        // Step 3: Verify order status is CONFIRMED
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            logger.error("Order {} has status {} - expected CONFIRMED", orderId, order.getStatus());
            throw new InvalidOrderStateException(
                    order.getStatus().toString(),
                    "process payment - order must be CONFIRMED");
        }

        // Step 4: Validate payment amount matches order total
        // Using compareTo() to handle BigDecimal scale differences correctly
        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            logger.error("Payment amount {} does not match order total {} for order {}",
                    request.getAmount(), order.getTotalAmount(), orderId);
            throw new PaymentAmountMismatchException(
                    request.getAmount(), order.getTotalAmount());
        }

        // Step 5: Process payment through gateway
        logger.info("Calling payment gateway for order {}: amount={}", orderId, request.getAmount());
        PaymentGatewayResult gatewayResult = paymentGateway.processPayment(
                request.getAmount(),
                orderId.toString());

        // Step 6: Create payment record
        PaymentStatus paymentStatus = gatewayResult.success()
                ? PaymentStatus.SUCCESS
                : PaymentStatus.FAILED;

        Payment payment = new Payment(
                order,
                request.getAmount(),
                paymentStatus,
                idempotencyKey,
                gatewayResult.gatewayReference());

        Payment savedPayment = paymentRepository.save(payment);

        logger.info("Payment {} saved with status {} for order {}",
                savedPayment.getId(), paymentStatus, orderId);

        // Step 7: If payment successful, update order status to PAID
        if (gatewayResult.success()) {
            logger.info("Payment successful - updating order {} to PAID status", orderId);
            orderService.updateOrderStatusToPaid(orderId);
        } else {
            logger.warn("Payment failed for order {} - order remains CONFIRMED", orderId);
        }

        return PaymentResponse.fromEntity(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(UUID orderId, UUID userId) {
        logger.debug("Retrieving payments for order {} and user {}", orderId, userId);

        // Verify order exists and belongs to user
        orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> {
                    logger.error("Order {} not found for user {}", orderId, userId);
                    return new ResourceNotFoundException("Order", "id", orderId);
                });

        List<Payment> payments = paymentRepository.findByOrderId(orderId);

        logger.info("Found {} payment(s) for order {}", payments.size(), orderId);

        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
