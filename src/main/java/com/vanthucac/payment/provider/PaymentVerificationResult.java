package com.vanthucac.payment.provider;

public record PaymentVerificationResult(
        boolean success,
        String providerPaymentId
) {
}