package com.vanthucac.order.dto;

import com.vanthucac.order.entity.Order;
import com.vanthucac.seller.dto.SellerProfileResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long parentOrderId,
        SellerProfileResponse seller,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        String status,
        String orderType,
        String shippingAddress,
        List<OrderResponse> subOrders,
        Instant createdAt
) {
    public static OrderResponse fromParent(Order order, List<OrderItemResponse> items,
                                           List<OrderResponse> subOrders) {
        return new OrderResponse(
                order.getId(),
                null,
                null,
                items,
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getOrderType().name(),
                order.getShippingAddress(),
                subOrders,
                order.getCreatedAt()
        );
    }

    public static OrderResponse fromSub(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(),
                order.getParentOrder().getId(),
                order.getSeller() != null
                        ? SellerProfileResponse.from(order.getSeller())
                        : null,
                items,
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getOrderType().name(),
                order.getShippingAddress(),
                List.of(),
                order.getCreatedAt()
        );
    }
}