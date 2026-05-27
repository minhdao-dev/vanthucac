package com.vanthucac.seller.dto;

public record UpgradeSellerResponse(
        SellerProfileResponse seller,
        String newAccessToken
) {
    public static UpgradeSellerResponse of(SellerProfileResponse seller, String newAccessToken) {
        return new UpgradeSellerResponse(seller, newAccessToken);
    }
}