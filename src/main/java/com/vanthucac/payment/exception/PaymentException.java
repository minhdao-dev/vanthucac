package com.vanthucac.payment.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PaymentException extends BusinessException {

    public PaymentException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static PaymentException paymentNotFound() {
        return new PaymentException("Payment not found",
                PaymentErrorCode.PAYMENT_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static PaymentException accessDenied() {
        return new PaymentException("You do not have access to this payment",
                PaymentErrorCode.PAYMENT_ACCESS_DENIED, HttpStatus.FORBIDDEN);
    }

    public static PaymentException paymentNotPayable() {
        return new PaymentException("Payment cannot be completed at current status",
                PaymentErrorCode.PAYMENT_NOT_PAYABLE, HttpStatus.BAD_REQUEST);
    }

    public static PaymentException paymentNotCompleted() {
        return new PaymentException("Payment has not been completed",
                PaymentErrorCode.PAYMENT_NOT_COMPLETED, HttpStatus.BAD_REQUEST);
    }

    public static PaymentException providerRejected() {
        return new PaymentException("Payment provider rejected the transaction",
                PaymentErrorCode.PAYMENT_PROVIDER_REJECTED, HttpStatus.BAD_REQUEST);
    }
}