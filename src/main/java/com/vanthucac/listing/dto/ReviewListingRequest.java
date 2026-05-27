package com.vanthucac.listing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewListingRequest(

        @NotBlank(message = "Reason is required when rejecting a listing")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}