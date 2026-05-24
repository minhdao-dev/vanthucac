package com.vanthucac.cart.dto;

import com.vanthucac.cart.entity.CartItem;
import com.vanthucac.listing.dto.ListingResponse;

import java.math.BigDecimal;
import java.util.List;

public record CartItemResponse(
        Long id,
        ListingResponse listing,
        Integer quantity,
        BigDecimal subtotal
) {
    public static CartItemResponse from(CartItem item, List<String> imageUrls) {
        var subtotal = item.getListing().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                ListingResponse.from(item.getListing(), imageUrls),
                item.getQuantity(),
                subtotal
        );
    }
}