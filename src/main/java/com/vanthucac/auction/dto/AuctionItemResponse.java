package com.vanthucac.auction.dto;

import com.vanthucac.auction.entity.AuctionItem;
import com.vanthucac.catalog.dto.BookCatalogResponse;

import java.math.BigDecimal;

public record AuctionItemResponse(
        Long id,
        BookCatalogResponse book,
        BigDecimal startingPrice,
        BigDecimal currentPrice,
        BigDecimal minBidIncrement,
        String status,
        Long winnerId,
        String winnerName
) {
    public static AuctionItemResponse from(AuctionItem item) {
        return new AuctionItemResponse(
                item.getId(),
                BookCatalogResponse.from(item.getBookCatalog()),
                item.getStartingPrice(),
                item.getCurrentPrice(),
                item.getMinBidIncrement(),
                item.getStatus().name(),
                item.getWinner() != null ? item.getWinner().getId() : null,
                item.getWinner() != null ? item.getWinner().getFullName() : null
        );
    }
}