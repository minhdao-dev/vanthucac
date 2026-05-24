package com.vanthucac.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpgradeSellerRequest(

        @NotBlank(message = "Shop name is required")
        String shopName,

        String description
) {
}