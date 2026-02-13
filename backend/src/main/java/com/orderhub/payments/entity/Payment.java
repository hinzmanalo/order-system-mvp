package com.orderhub.payments.entity;

import com.orderhub.orders.entity.Order;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a payment transaction for an order.
 * <p>
 * Each payment is uniquely identified by an idempotency key to prevent
 * duplicate processing. The payment captures the amount, gateway response,
 * and final status (SUCCESS or FAILED).
 * </p>
 * <p>
 * Thread-safety: Payment entities are immutable after creation and should
 * be managed within transaction boundaries.
 * </p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @Column(name = "gateway_reference")
    private String gatewayReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Default constructor for JPA.
     */
    public Payment() {
    }

    /**
     * Creates a new payment transaction.
     *
     * @param order            the order being paid for
     * @param amount           the payment amount
     * @param status           the payment status
     * @param idempotencyKey   unique key to prevent duplicate processing
     * @param gatewayReference optional reference from payment gateway
     */
    public Payment(Order order, BigDecimal amount, PaymentStatus status,
            String idempotencyKey, String gatewayReference) {
        this.order = order;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.gatewayReference = gatewayReference;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
