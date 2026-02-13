package com.orderhub.orders.service;

import com.orderhub.auth.entity.User;
import com.orderhub.auth.repository.UserRepository;
import com.orderhub.catalog.entity.Product;
import com.orderhub.catalog.repository.ProductRepository;
import com.orderhub.common.exception.InvalidOrderStateException;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.inventory.service.InventoryService;
import com.orderhub.orders.dto.CreateOrderRequest;
import com.orderhub.orders.dto.OrderItemRequest;
import com.orderhub.orders.dto.OrderItemResponse;
import com.orderhub.orders.dto.OrderResponse;
import com.orderhub.orders.entity.Order;
import com.orderhub.orders.entity.OrderItem;
import com.orderhub.orders.entity.OrderStatus;
import com.orderhub.orders.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of OrderService.
 * <p>
 * Handles order lifecycle management with atomic inventory operations.
 * All inventory modifications are performed within the same transaction
 * to ensure consistency.
 * </p>
 *
 * @since 1.0.0
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        logger.info("Creating order for user {} with {} items", userId, request.getItems().size());

        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Create order entity
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.CONFIRMED);

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Process each item
        for (OrderItemRequest itemRequest : request.getItems()) {
            logger.debug("Processing order item: productId={}, quantity={}",
                    itemRequest.getProductId(), itemRequest.getQuantity());

            // Load product
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product", "id", itemRequest.getProductId()));

            // Validate product is active
            if (!product.isActive()) {
                logger.warn("Attempted to order inactive product: {}", product.getId());
                throw new InvalidOrderStateException(
                        "inactive", "order product " + product.getName());
            }

            // Decrement inventory (throws InsufficientStockException if not enough stock)
            // This participates in the transaction - if this fails, entire order rolls back
            inventoryService.decrementStock(product.getId(), itemRequest.getQuantity());

            // Create order item with price snapshot
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);

            logger.debug("Order item processed: productId={}, unitPrice={}, subtotal={}",
                    product.getId(), unitPrice, subtotal);
        }

        order.setTotalAmount(totalAmount);

        // Save order
        Order savedOrder = orderRepository.save(order);

        logger.info("Order created successfully: orderId={}, totalAmount={}, items={}",
                savedOrder.getId(), savedOrder.getTotalAmount(), savedOrder.getItems().size());

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, UUID userId) {
        logger.debug("Retrieving order {} for user {}", orderId, userId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", orderId));

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(UUID userId, String status,
                                             LocalDateTime after, LocalDateTime before,
                                             Pageable pageable) {
        logger.debug("Retrieving orders for user {} with filters: status={}, after={}, before={}",
                userId, status, after, before);

        OrderStatus orderStatus = status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;

        Page<Order> orders = orderRepository.findByUserIdWithFilters(
                userId, orderStatus, after, before, pageable);

        logger.info("Retrieved {} orders for user {}", orders.getTotalElements(), userId);

        return orders.map(this::mapToOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId) {
        logger.info("Cancelling order {} for user {}", orderId, userId);

        // Find order for user
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", orderId));

        // Validate order can be cancelled
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            logger.warn("Attempted to cancel order {} with status {}", orderId, order.getStatus());
            throw new InvalidOrderStateException(
                    order.getStatus().toString(), "cancel order");
        }

        // Restore inventory for each item
        for (OrderItem item : order.getItems()) {
            logger.debug("Restoring stock for product {}: quantity {}",
                    item.getProduct().getId(), item.getQuantity());
            inventoryService.restoreStock(item.getProduct().getId(), item.getQuantity());
        }

        // Update status
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        logger.info("Order {} cancelled successfully", orderId);

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(String status, LocalDateTime after,
                                            LocalDateTime before, Pageable pageable) {
        logger.debug("Admin retrieving all orders with filters: status={}, after={}, before={}",
                status, after, before);

        OrderStatus orderStatus = status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;

        Page<Order> orders = orderRepository.findAllWithFilters(
                orderStatus, after, before, pageable);

        logger.info("Admin retrieved {} total orders", orders.getTotalElements());

        return orders.map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getAnyOrderById(UUID orderId) {
        logger.debug("Admin retrieving order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", orderId));

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelAnyOrder(UUID orderId) {
        logger.info("Admin cancelling order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", orderId));

        // Validate order can be cancelled
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            logger.warn("Admin attempted to cancel order {} with status {}", orderId, order.getStatus());
            throw new InvalidOrderStateException(
                    order.getStatus().toString(), "cancel order");
        }

        // Restore inventory
        for (OrderItem item : order.getItems()) {
            logger.debug("Admin restoring stock for product {}: quantity {}",
                    item.getProduct().getId(), item.getQuantity());
            inventoryService.restoreStock(item.getProduct().getId(), item.getQuantity());
        }

        // Update status
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);

        logger.info("Admin cancelled order {} successfully", orderId);

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public void updateOrderStatusToPaid(UUID orderId) {
        logger.info("Updating order {} status to PAID", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", orderId));

        // Validate order is in CONFIRMED status
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            logger.error("Attempted to mark order {} as PAID but status is {}", orderId, order.getStatus());
            throw new InvalidOrderStateException(
                    order.getStatus().toString(), "mark order as PAID");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        logger.info("Order {} marked as PAID successfully", orderId);
    }

    /**
     * Maps an Order entity to OrderResponse DTO.
     *
     * @param order the order entity
     * @return the order response DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUser().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    /**
     * Maps an OrderItem entity to OrderItemResponse DTO.
     *
     * @param item the order item entity
     * @return the order item response DTO
     */
    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
