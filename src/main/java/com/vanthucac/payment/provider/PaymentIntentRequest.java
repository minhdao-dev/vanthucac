package com.vanthucac.payment.provider;

import com.vanthucac.payment.entity.Payment;

import java.math.BigDecimal;

public record PaymentIntentRequest(
        Long orderId,
        BigDecimal amount,
        Payment.PaymentMethod paymentMethod
) {
}