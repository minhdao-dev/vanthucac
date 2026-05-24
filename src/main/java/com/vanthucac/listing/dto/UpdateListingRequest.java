package com.vanthucac.listing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.List;

public record UpdateListingRequest(

        @DecimalMin(value = "1000", message = "Price must be at least 1000 VND")
        BigDecimal price,

        String condition,

        @Min(value = 0, message = "Stock must be at least 0")
        Integer stock,

        String status,

        List<String> imageUrls
) {
}