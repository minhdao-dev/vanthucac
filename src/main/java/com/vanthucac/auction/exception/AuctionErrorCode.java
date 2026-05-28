package com.vanthucac.auction.exception;

public final class AuctionErrorCode {
    private AuctionErrorCode() {
    }

    public static final String SESSION_NOT_FOUND = "AUCTION_SESSION_NOT_FOUND";
    public static final String ITEM_NOT_FOUND = "AUCTION_ITEM_NOT_FOUND";
    public static final String SESSION_NOT_ACTIVE = "AUCTION_SESSION_NOT_ACTIVE";
    public static final String SESSION_NOT_SCHEDULED = "AUCTION_SESSION_NOT_SCHEDULED";
    public static final String BID_TOO_LOW = "AUCTION_BID_TOO_LOW";
    public static final String ALREADY_HIGHEST_BIDDER = "AUCTION_ALREADY_HIGHEST_BIDDER";
    public static final String INVALID_TIME = "AUCTION_INVALID_TIME";
}