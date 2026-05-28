package com.vanthucac.auction.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AuctionException extends BusinessException {

    public AuctionException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static AuctionException sessionNotFound() {
        return new AuctionException("Auction session not found",
                AuctionErrorCode.SESSION_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static AuctionException itemNotFound() {
        return new AuctionException("Auction item not found",
                AuctionErrorCode.ITEM_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public static AuctionException sessionNotActive() {
        return new AuctionException("Auction session is not active",
                AuctionErrorCode.SESSION_NOT_ACTIVE, HttpStatus.BAD_REQUEST);
    }

    public static AuctionException sessionNotScheduled() {
        return new AuctionException("Auction session is not in SCHEDULED status",
                AuctionErrorCode.SESSION_NOT_SCHEDULED, HttpStatus.BAD_REQUEST);
    }

    public static AuctionException bidTooLow(String minAmount) {
        return new AuctionException("Bid amount must be at least " + minAmount,
                AuctionErrorCode.BID_TOO_LOW, HttpStatus.BAD_REQUEST);
    }

    public static AuctionException alreadyHighestBidder() {
        return new AuctionException("You are already the highest bidder on this item",
                AuctionErrorCode.ALREADY_HIGHEST_BIDDER, HttpStatus.BAD_REQUEST);
    }

    public static AuctionException invalidTime() {
        return new AuctionException("End time must be after start time",
                AuctionErrorCode.INVALID_TIME, HttpStatus.BAD_REQUEST);
    }
}