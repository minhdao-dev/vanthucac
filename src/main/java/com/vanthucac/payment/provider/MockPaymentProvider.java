package com.vanthucac.payment.provider;

import com.vanthucac.payment.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentRequest request) {
        var providerPaymentId = "mock_" + UUID.randomUUID();
        var checkoutUrl = "/api/v1/payments/mock/callback?providerPaymentId=" + providerPaymentId;
        return new PaymentIntent(Payment.PaymentMethod.MOCK, providerPaymentId, checkoutUrl);
    }

    @Override
    public PaymentVerificationResult verifyPayment(String providerPaymentId) {
        return new PaymentVerificationResult(true, providerPaymentId);
    }
}