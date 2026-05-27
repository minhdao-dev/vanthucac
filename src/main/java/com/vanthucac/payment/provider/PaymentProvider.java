package com.vanthucac.payment.provider;

public interface PaymentProvider {

    PaymentIntent createPaymentIntent(PaymentIntentRequest request);

    PaymentVerificationResult verifyPayment(String providerPaymentId);
}