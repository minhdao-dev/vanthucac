package com.vanthucac.auction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BidBroadcastMessage(
        Long auctionItemId,
        BigDecimal newPrice,
        Long bidderId,
        String bidderName,
        Instant bidTime
) {
}