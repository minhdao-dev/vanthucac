package com.vanthucac.auction.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionScheduler.class);

    private final AuctionService auctionService;

    public AuctionScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void processAuctionSessions() {
        log.debug("Auction scheduler running...");
        try {
            auctionService.processScheduledSessions();
        } catch (Exception e) {
            log.error("Auction scheduler error: {}", e.getMessage(), e);
        }
    }
}