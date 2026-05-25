package com.vanthucac.order.exception;

public final class OrderErrorCode {
    private OrderErrorCode() {
    }

    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String ORDER_ACCESS_DENIED = "ORDER_ACCESS_DENIED";
    public static final String ORDER_NOT_CANCELLABLE = "ORDER_NOT_CANCELLABLE";
    public static final String CART_EMPTY = "ORDER_CART_EMPTY";
    public static final String STOCK_INSUFFICIENT = "ORDER_STOCK_INSUFFICIENT";
    public static final String STOCK_CONFLICT = "ORDER_STOCK_CONFLICT";
    public static final String INVALID_STATUS_TRANSITION = "ORDER_INVALID_STATUS_TRANSITION";
}