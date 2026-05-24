package com.vanthucac.order.dto;

import com.vanthucac.listing.dto.ListingResponse;
import com.vanthucac.order.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemResponse(
        Long id,
        ListingResponse listing,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    public static OrderItemResponse from(OrderItem item, List<String> imageUrls) {
        return new OrderItemResponse(
                item.getId(),
                ListingResponse.from(item.getListing(), imageUrls),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}