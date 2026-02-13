package com.orderhub.orders.service;

import com.orderhub.auth.entity.Role;
import com.orderhub.auth.entity.User;
import com.orderhub.auth.repository.UserRepository;
import com.orderhub.catalog.entity.Product;
import com.orderhub.catalog.repository.ProductRepository;
import com.orderhub.common.exception.InsufficientStockException;
import com.orderhub.common.exception.InvalidOrderStateException;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.inventory.service.InventoryService;
import com.orderhub.orders.dto.CreateOrderRequest;
import com.orderhub.orders.dto.OrderItemRequest;
import com.orderhub.orders.dto.OrderResponse;
import com.orderhub.orders.entity.Order;
import com.orderhub.orders.entity.OrderItem;
import com.orderhub.orders.entity.OrderStatus;
import com.orderhub.orders.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setRole(Role.USER);
        return user;
    }

    private Product createTestProduct(String name, String sku, BigDecimal price, boolean active) {
        Product product = new Product(name, "Description", price, sku);
        product.setId(UUID.randomUUID());
        product.setActive(active);
        return product;
    }

    @Test
    @DisplayName("CreateOrder - success - multiple items with inventory decrement")
    void createOrder_success_multipleItems() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser();

        Product product1 = createTestProduct("Product 1", "SKU-001", new BigDecimal("10.00"), true);
        Product product2 = createTestProduct("Product 2", "SKU-002", new BigDecimal("20.00"), true);

        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductId(product1.getId());
        item1.setQuantity(2);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductId(product2.getId());
        item2.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Arrays.asList(item1, item2));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(product1.getId())).thenReturn(Optional.of(product1));
        when(productRepository.findById(product2.getId())).thenReturn(Optional.of(product2));

        // Mock inventory decrement - should not throw
        doNothing().when(inventoryService).decrementStock(any(UUID.class), anyInt());

        Order savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setUser(user);
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        savedOrder.setTotalAmount(new BigDecimal("40.00"));

        OrderItem orderItem1 = new OrderItem();
        orderItem1.setProduct(product1);
        orderItem1.setQuantity(2);
        orderItem1.setUnitPrice(new BigDecimal("10.00"));
        orderItem1.setSubtotal(new BigDecimal("20.00"));

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setProduct(product2);
        orderItem2.setQuantity(1);
        orderItem2.setUnitPrice(new BigDecimal("20.00"));
        orderItem2.setSubtotal(new BigDecimal("20.00"));

        savedOrder.addItem(orderItem1);
        savedOrder.addItem(orderItem2);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        OrderResponse response = orderService.createOrder(userId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(response.getItems()).hasSize(2);

        // Verify inventory was decremented for both products
        verify(inventoryService).decrementStock(product1.getId(), 2);
        verify(inventoryService).decrementStock(product2.getId(), 1);
    }

    @Test
    @DisplayName("CreateOrder - inactive product - throws InvalidOrderStateException")
    void createOrder_inactiveProduct_throws() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser();

        Product inactiveProduct = createTestProduct("Inactive Product", "SKU-001", new BigDecimal("10.00"), false);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(inactiveProduct.getId());
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Collections.singletonList(item));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(inactiveProduct.getId())).thenReturn(Optional.of(inactiveProduct));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("inactive");

        verify(inventoryService, never()).decrementStock(any(), anyInt());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("CreateOrder - insufficient stock - throws InsufficientStockException")
    void createOrder_insufficientStock_throws() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser();

        Product product = createTestProduct("Product", "SKU-001", new BigDecimal("10.00"), true);

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(product.getId());
        item.setQuantity(100);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Collections.singletonList(item));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        // Mock insufficient stock exception
        doThrow(new InsufficientStockException("Product", 10, 100))
                .when(inventoryService).decrementStock(product.getId(), 100);

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("CreateOrder - product not found - throws ResourceNotFoundException")
    void createOrder_productNotFound_throws() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = createTestUser();
        UUID nonExistentProductId = UUID.randomUUID();

        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(nonExistentProductId);
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(Collections.singletonList(item));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    @DisplayName("CancelOrder - confirmed order - success with inventory restore")
    void cancelOrder_confirmed_success() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        User user = createTestUser();
        Product product = createTestProduct("Product", "SKU-001", new BigDecimal("10.00"), true);

        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setStatus(OrderStatus.CONFIRMED);

        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(5);
        order.addItem(orderItem);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(inventoryService).restoreStock(any(UUID.class), anyInt());

        // When
        OrderResponse response = orderService.cancelOrder(orderId, userId);

        // Then
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).restoreStock(product.getId(), 5);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("CancelOrder - paid order - throws InvalidOrderStateException")
    void cancelOrder_paid_throws() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        User user = createTestUser();
        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setStatus(OrderStatus.PAID);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("PAID");

        verify(inventoryService, never()).restoreStock(any(), anyInt());
    }

    @Test
    @DisplayName("CancelOrder - already cancelled - throws InvalidOrderStateException")
    void cancelOrder_cancelled_throws() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        User user = createTestUser();
        Order order = new Order();
        order.setId(orderId);
        order.setUser(user);
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("CancelOrder - wrong user - throws ResourceNotFoundException")
    void cancelOrder_wrongUser_throws() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    @DisplayName("GetUserOrders - filters by userId - only own orders returned")
    void getUserOrders_onlyOwnOrders() {
        // Given - this is implicitly tested by the repository mock
        // The repository method findByUserIdWithFilters ensures only the user's orders
        // are returned
        // This test verifies the service calls the correct repository method
        UUID userId = UUID.randomUUID();

        when(orderRepository.findByUserIdWithFilters(eq(userId), any(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        // When
        orderService.getUserOrders(userId, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        // Then
        verify(orderRepository).findByUserIdWithFilters(eq(userId), any(), any(), any(), any());
    }

    @Test
    @DisplayName("UpdateOrderStatusToPaid - success - status changed to PAID")
    void updateOrderStatusToPaid_success() {
        // Given
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // When
        orderService.updateOrderStatusToPaid(orderId);

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("UpdateOrderStatusToPaid - not confirmed - throws InvalidOrderStateException")
    void updateOrderStatusToPaid_notConfirmed_throws() {
        // Given
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatusToPaid(orderId))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("CANCELLED");

        verify(orderRepository, never()).save(any());
    }
}
