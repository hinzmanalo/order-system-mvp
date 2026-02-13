package com.orderhub.payments.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for processing a payment.
 * <p>
 * Contains the payment amount which must match the order's total amount
 * exactly. The comparison uses BigDecimal.compareTo() to handle scale
 * differences correctly.
 * </p>
 *
 * @since 1.0.0
 */
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    /**
     * Default constructor for Jackson deserialization.
     */
    public PaymentRequest() {
    }

    /**
     * Creates a payment request with the specified amount.
     *
     * @param amount the payment amount
     */
    public PaymentRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
