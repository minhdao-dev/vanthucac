package com.vanthucac.user.dto;

import com.vanthucac.common.validation.VietnamesePhone;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(

        @NotBlank(message = "Full name is required")
        String fullName,

        @VietnamesePhone
        String phone,

        String avatarUrl
) {
}