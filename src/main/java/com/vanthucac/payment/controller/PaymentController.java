package com.vanthucac.payment.controller;

import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.payment.dto.PaymentResponse;
import com.vanthucac.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var payment = paymentService.getPaymentByOrderId(orderId, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Payment retrieved successfully", payment));
    }

    @PostMapping("/orders/{orderId}/mock-complete")
    public ResponseEntity<ApiResponse<PaymentResponse>> completeMockPayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var payment = paymentService.completeMockPayment(orderId, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Payment completed successfully", payment));
    }
}