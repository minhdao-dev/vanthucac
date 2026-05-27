package com.vanthucac.payment.exception;

public final class PaymentErrorCode {
    private PaymentErrorCode() {
    }

    public static final String PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND";
    public static final String PAYMENT_ACCESS_DENIED = "PAYMENT_ACCESS_DENIED";
    public static final String PAYMENT_NOT_PAYABLE = "PAYMENT_NOT_PAYABLE";
    public static final String PAYMENT_NOT_COMPLETED = "PAYMENT_NOT_COMPLETED";
    public static final String PAYMENT_PROVIDER_REJECTED = "PAYMENT_PROVIDER_REJECTED";
}