package com.vanthucac.order.controller;

import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.order.dto.CheckoutRequest;
import com.vanthucac.order.dto.OrderResponse;
import com.vanthucac.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var order = orderService.checkout(request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order created successfully", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var orders = orderService.getMyOrders(page, size, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Orders retrieved successfully", orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var order = orderService.getOrderById(orderId, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Order retrieved successfully", order));
    }

    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        orderService.confirmOrder(orderId, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Order confirmed successfully"));
    }

    @PutMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        orderService.completeOrder(orderId, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Order completed successfully"));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        orderService.cancelOrder(orderId, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully"));
    }
}