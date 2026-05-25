package com.vanthucac.seller.dto;

import com.vanthucac.seller.entity.SellerWallet;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        Instant updatedAt
) {
    public static WalletResponse from(SellerWallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getUpdatedAt()
        );
    }
}