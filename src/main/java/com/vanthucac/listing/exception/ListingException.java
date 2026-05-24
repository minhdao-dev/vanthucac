package com.vanthucac.listing.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ListingException extends BusinessException {

    public ListingException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static ListingException listingNotFound() {
        return new ListingException(
                "Listing not found",
                ListingErrorCode.LISTING_NOT_FOUND,
                HttpStatus.NOT_FOUND
        );
    }

    public static ListingException accessDenied() {
        return new ListingException(
                "You do not have permission to modify this listing",
                ListingErrorCode.LISTING_ACCESS_DENIED,
                HttpStatus.FORBIDDEN
        );
    }

    public static ListingException invalidCondition(String condition) {
        return new ListingException(
                "Invalid condition: " + condition,
                ListingErrorCode.INVALID_CONDITION,
                HttpStatus.BAD_REQUEST
        );
    }

    public static ListingException invalidStatus(String status) {
        return new ListingException(
                "Invalid status: " + status,
                ListingErrorCode.INVALID_STATUS,
                HttpStatus.BAD_REQUEST
        );
    }
}