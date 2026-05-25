package com.vanthucac.auction.dto;

import com.vanthucac.auction.entity.Bid;

import java.math.BigDecimal;
import java.time.Instant;

public record BidResponse(
        Long id,
        Long auctionItemId,
        Long bidderId,
        String bidderName,
        BigDecimal amount,
        Instant createdAt
) {
    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuctionItem().getId(),
                bid.getUser().getId(),
                bid.getUser().getFullName(),
                bid.getAmount(),
                bid.getCreatedAt()
        );
    }
}