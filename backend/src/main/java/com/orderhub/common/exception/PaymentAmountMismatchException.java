package com.orderhub.common.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when payment amount does not match the expected order total.
 */
public class PaymentAmountMismatchException extends RuntimeException {

    private final BigDecimal expected;
    private final BigDecimal provided;

    public PaymentAmountMismatchException(BigDecimal expected, BigDecimal provided) {
        super(String.format("Payment amount mismatch: expected %s, provided %s",
                expected, provided));
        this.expected = expected;
        this.provided = provided;
    }

    public BigDecimal getExpected() {
        return expected;
    }

    public BigDecimal getProvided() {
        return provided;
    }
}
