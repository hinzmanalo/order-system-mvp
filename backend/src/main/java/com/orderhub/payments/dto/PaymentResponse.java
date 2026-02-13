package com.orderhub.payments.dto;

import com.orderhub.payments.entity.Payment;
import com.orderhub.payments.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for payment information.
 * <p>
 * Returns payment details including status, gateway reference, and
 * associated order information. Exposed to clients after payment
 * processing or when retrieving payment history.
 * </p>
 *
 * @since 1.0.0
 */
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String gatewayReference;
    private LocalDateTime createdAt;

    /**
     * Default constructor for Jackson serialization.
     */
    public PaymentResponse() {
    }

    /**
     * Creates a payment response with all fields.
     *
     * @param id               the payment ID
     * @param orderId          the associated order ID
     * @param amount           the payment amount
     * @param status           the payment status
     * @param gatewayReference the gateway transaction reference
     * @param createdAt        the payment creation timestamp
     */
    public PaymentResponse(UUID id, UUID orderId, BigDecimal amount,
            PaymentStatus status, String gatewayReference,
            LocalDateTime createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.gatewayReference = gatewayReference;
        this.createdAt = createdAt;
    }

    /**
     * Creates a PaymentResponse from a Payment entity.
     *
     * @param payment the payment entity
     * @return a PaymentResponse DTO
     */
    public static PaymentResponse fromEntity(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getGatewayReference(),
                payment.getCreatedAt());
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
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
