package com.vanthucac.seller.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SellerException extends BusinessException {

    public SellerException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static SellerException alreadySeller() {
        return new SellerException(
                "User is already a seller",
                SellerErrorCode.ALREADY_SELLER,
                HttpStatus.CONFLICT
        );
    }

    public static SellerException sellerNotFound() {
        return new SellerException(
                "Seller profile not found",
                SellerErrorCode.SELLER_NOT_FOUND,
                HttpStatus.NOT_FOUND
        );
    }

    public static SellerException walletNotFound() {
        return new SellerException(
                "Seller wallet not found",
                SellerErrorCode.WALLET_NOT_FOUND,
                HttpStatus.NOT_FOUND
        );
    }
}