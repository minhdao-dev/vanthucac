package com.vanthucac.payment.provider;

import com.vanthucac.payment.entity.Payment;

public record PaymentIntent(
        Payment.PaymentMethod paymentMethod,
        String providerPaymentId,
        String checkoutUrl
) {
}