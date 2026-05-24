package com.vanthucac.cart.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CartException extends BusinessException {

    public CartException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static CartException cartNotFound() {
        return new CartException("Cart not found",
                CartErrorCode.CART_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static CartException itemNotFound() {
        return new CartException("Cart item not found",
                CartErrorCode.CART_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static CartException listingNotAvailable() {
        return new CartException("This listing is not available for purchase",
                CartErrorCode.LISTING_NOT_AVAILABLE, HttpStatus.BAD_REQUEST);
    }
}