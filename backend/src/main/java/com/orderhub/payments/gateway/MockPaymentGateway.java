package com.orderhub.payments.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock payment gateway implementation for development and testing.
 * <p>
 * Simulates payment processing with a 90% success rate. This allows
 * testing both successful and failed payment scenarios without requiring
 * a real payment gateway integration.
 * </p>
 * <p>
 * Thread-safety: This implementation is thread-safe using ThreadLocalRandom.
 * </p>
 * <p>
 * To use a real payment gateway (Stripe, PayPal, etc.), create a new
 * implementation of PaymentGateway and replace this component.
 * </p>
 *
 * @since 1.0.0
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger logger = LoggerFactory.getLogger(MockPaymentGateway.class);
    private static final double SUCCESS_RATE = 0.9;

    @Override
    public PaymentGatewayResult processPayment(BigDecimal amount, String referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        logger.info("Processing mock payment: amount={}, reference={}", amount, referenceId);

        // Simulate 90% success rate
        boolean isSuccessful = ThreadLocalRandom.current().nextDouble() < SUCCESS_RATE;

        if (isSuccessful) {
            String gatewayReference = "MOCK-REF-" + UUID.randomUUID();
            logger.info("Mock payment successful: reference={}, gateway={}",
                    referenceId, gatewayReference);
            return PaymentGatewayResult.success(gatewayReference);
        } else {
            logger.warn("Mock payment failed: reference={}", referenceId);
            return PaymentGatewayResult.failure();
        }
    }
}
