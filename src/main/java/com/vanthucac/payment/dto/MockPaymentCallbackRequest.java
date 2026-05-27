package com.vanthucac.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockPaymentCallbackRequest(
        @NotBlank
        String providerPaymentId,

        @NotNull
        Boolean success
) {
}