package com.vanthucac.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(

        @NotBlank(message = "Shipping address is required")
        @Size(max = 500, message = "Shipping address must not exceed 500 characters")
        String shippingAddress
) {
}