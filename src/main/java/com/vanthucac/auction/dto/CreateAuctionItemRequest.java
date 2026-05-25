package com.vanthucac.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAuctionItemRequest(

        @NotNull(message = "Book catalog ID is required")
        Long bookCatalogId,

        @NotNull(message = "Starting price is required")
        @DecimalMin(value = "1000", message = "Starting price must be at least 1000 VND")
        BigDecimal startingPrice,

        @NotNull(message = "Min bid increment is required")
        @DecimalMin(value = "1000", message = "Min bid increment must be at least 1000 VND")
        BigDecimal minBidIncrement
) {
}