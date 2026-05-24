package com.vanthucac.listing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateListingRequest(

        @NotNull(message = "Book catalog ID is required")
        Integer bookCatalogId,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "1000", message = "Price must be at least 1000 VND")
        BigDecimal price,

        @NotBlank(message = "Condition is required")
        String condition,

        @NotNull(message = "Stock is required")
        @Min(value = 1, message = "Stock must be at least 1")
        Integer stock,

        List<String> imageUrls
) {
}