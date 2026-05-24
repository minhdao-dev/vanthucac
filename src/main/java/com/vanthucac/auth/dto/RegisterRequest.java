package com.vanthucac.auth.dto;

import com.vanthucac.common.validation.StrongPassword;
import com.vanthucac.common.validation.VietnamesePhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        @NotBlank(message = "Full name is required")
        String fullName,

        @VietnamesePhone
        String phone
) {
}