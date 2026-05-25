package com.vanthucac.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceBidRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1000", message = "Bid amount must be at least 1000 VND")
        BigDecimal amount
) {
}