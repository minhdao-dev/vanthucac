package com.vanthucac.cart.dto;

import com.vanthucac.cart.entity.Cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        Long id,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount,
        Instant updatedAt
) {
    public static CartResponse from(Cart cart, List<CartItemResponse> itemResponses) {
        var totalAmount = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(
                cart.getId(),
                itemResponses,
                itemResponses.size(),
                totalAmount,
                cart.getUpdatedAt()
        );
    }

    public static CartResponse empty() {
        return new CartResponse(null, List.of(), 0, BigDecimal.ZERO, Instant.now());
    }
}