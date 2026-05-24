package com.vanthucac.order.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class OrderException extends BusinessException {

    public OrderException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static OrderException orderNotFound() {
        return new OrderException("Order not found",
                OrderErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static OrderException accessDenied() {
        return new OrderException("You do not have access to this order",
                OrderErrorCode.ORDER_ACCESS_DENIED, HttpStatus.FORBIDDEN);
    }

    public static OrderException cartEmpty() {
        return new OrderException("Cannot checkout with empty cart",
                OrderErrorCode.CART_EMPTY, HttpStatus.BAD_REQUEST);
    }

    public static OrderException stockInsufficient(String bookTitle) {
        return new OrderException("Insufficient stock for: " + bookTitle,
                OrderErrorCode.STOCK_INSUFFICIENT, HttpStatus.BAD_REQUEST);
    }

    public static OrderException stockConflict() {
        return new OrderException(
                "Stock was modified by another request, please try again",
                OrderErrorCode.STOCK_CONFLICT, HttpStatus.CONFLICT);
    }

    public static OrderException notCancellable() {
        return new OrderException("Order cannot be cancelled at current status",
                OrderErrorCode.ORDER_NOT_CANCELLABLE, HttpStatus.BAD_REQUEST);
    }

    public static OrderException invalidStatusTransition() {
        return new OrderException("Invalid order status transition",
                OrderErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.BAD_REQUEST);
    }
}