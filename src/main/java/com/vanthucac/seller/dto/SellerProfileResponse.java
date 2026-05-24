package com.vanthucac.seller.dto;

import com.vanthucac.seller.entity.SellerProfile;

import java.time.Instant;

public record SellerProfileResponse(
        Integer id,
        String shopName,
        String description,
        String status,
        Instant createdAt
) {
    public static SellerProfileResponse from(SellerProfile profile) {
        return new SellerProfileResponse(
                profile.getId(),
                profile.getShopName(),
                profile.getDescription(),
                profile.getStatus().name(),
                profile.getCreatedAt()
        );
    }
}