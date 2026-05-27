package com.vanthucac.payment.dto;

import com.vanthucac.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        String status,
        String paymentMethod,
        String providerPaymentId,
        String checkoutUrl,
        Instant paidAt,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getPaymentMethod().name(),
                payment.getProviderPaymentId(),
                payment.getCheckoutUrl(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}