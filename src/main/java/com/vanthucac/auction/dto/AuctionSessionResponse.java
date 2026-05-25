package com.vanthucac.auction.dto;

import com.vanthucac.auction.entity.AuctionSession;

import java.time.Instant;
import java.util.List;

public record AuctionSessionResponse(
        Long id,
        String title,
        Instant startTime,
        Instant endTime,
        String status,
        List<AuctionItemResponse> items,
        Instant createdAt
) {
    public static AuctionSessionResponse from(AuctionSession session) {
        var items = session.getItems().stream()
                .map(AuctionItemResponse::from)
                .toList();

        return new AuctionSessionResponse(
                session.getId(),
                session.getTitle(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus().name(),
                items,
                session.getCreatedAt()
        );
    }
}