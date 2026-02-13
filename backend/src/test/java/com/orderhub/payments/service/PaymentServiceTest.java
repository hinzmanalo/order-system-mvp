package com.orderhub.payments.service;

import com.orderhub.auth.entity.User;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        return user;
    }

    private Order createTestOrder(UUID userId, BigDecimal amount, OrderStatus status) {
        User user = createTestUser();
        user.setId(userId);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        order.setTotalAmount(amount);
        order.setStatus(status);
        return order;
    }

    @Test
    @DisplayName("ProcessPayment - success - payment SUCCESS and order PAID")
    void processPayment_success() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "payment-key-123";
        BigDecimal amount = new BigDecimal("100.00");

        PaymentRequest request = new PaymentRequest();
        request.setAmount(amount);

        Order order = createTestOrder(userId, amount, OrderStatus.CONFIRMED);
        order.setId(orderId);

        PaymentGatewayResult gatewayResult = new PaymentGatewayResult(true, "gateway-ref-123");

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentGateway.processPayment(amount, orderId.toString())).thenReturn(gatewayResult);

        Payment savedPayment = new Payment(order, amount, PaymentStatus.SUCCESS, idempotencyKey, "gateway-ref-123");
        savedPayment.setId(UUID.randomUUID());

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        doNothing().when(orderService).updateOrderStatusToPaid(orderId);

        // When
        PaymentResponse response = paymentService.processPayment(orderId, userId, idempotencyKey, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getAmount()).isEqualByComparingTo(amount);

        verify(paymentGateway).processPayment(amount, orderId.toString());
        verify(orderService).updateOrderStatusToPaid(orderId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("ProcessPayment - gateway failure - payment FAILED, order stays CONFIRMED")
    void processPayment_gatewayFailure() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "payment-key-456";
        BigDecimal amount = new BigDecimal("100.00");

        PaymentRequest request = new PaymentRequest();
        request.setAmount(amount);

        Order order = createTestOrder(userId, amount, OrderStatus.CONFIRMED);
        order.setId(orderId);

        PaymentGatewayResult gatewayResult = new PaymentGatewayResult(false, "error-ref");

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentGateway.processPayment(amount, orderId.toString())).thenReturn(gatewayResult);

        Payment savedPayment = new Payment(order, amount, PaymentStatus.FAILED, idempotencyKey, "error-ref");
        savedPayment.setId(UUID.randomUUID());

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // When
        PaymentResponse response = paymentService.processPayment(orderId, userId, idempotencyKey, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);

        verify(paymentGateway).processPayment(amount, orderId.toString());
        verify(orderService, never()).updateOrderStatusToPaid(any());
    }

    @Test
    @DisplayName("ProcessPayment - idempotent - existing success returned without reprocessing")
    void processPayment_idempotent_existingSuccess() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "existing-key";
        BigDecimal amount = new BigDecimal("100.00");

        PaymentRequest request = new PaymentRequest();
        request.setAmount(amount);

        Order order = createTestOrder(userId, amount, OrderStatus.PAID);

        Payment existingPayment = new Payment(order, amount, PaymentStatus.SUCCESS, idempotencyKey, "ref-123");
        existingPayment.setId(UUID.randomUUID());

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        // When
        PaymentResponse response = paymentService.processPayment(orderId, userId, idempotencyKey, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        // Payment gateway should not be called again
        verify(paymentGateway, never()).processPayment(any(), anyString());
        verify(orderRepository, never()).findByIdAndUserId(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("ProcessPayment - amount mismatch - throws PaymentAmountMismatchException")
    void processPayment_amountMismatch_throws() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "payment-key-789";

        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("100.00"));

        Order order = createTestOrder(userId, new BigDecimal("200.00"), OrderStatus.CONFIRMED);
        order.setId(orderId);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(orderId, userId, idempotencyKey, request))
                .isInstanceOf(PaymentAmountMismatchException.class);

        verify(paymentGateway, never()).processPayment(any(), anyString());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("ProcessPayment - not confirmed order - throws InvalidOrderStateException")
    void processPayment_notConfirmedOrder_throws() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "payment-key-111";
        BigDecimal amount = new BigDecimal("100.00");

        PaymentRequest request = new PaymentRequest();
        request.setAmount(amount);

        Order order = createTestOrder(userId, amount, OrderStatus.CANCELLED);
        order.setId(orderId);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(orderId, userId, idempotencyKey, request))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("CANCELLED");

        verify(paymentGateway, never()).processPayment(any(), anyString());
    }

    @Test
    @DisplayName("ProcessPayment - order not found - throws ResourceNotFoundException")
    void processPayment_orderNotFound_throws() {
        // Given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "payment-key-222";

        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(orderId, userId, idempotencyKey, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");

        verify(paymentGateway, never()).processPayment(any(), anyString());
    }
}
