package com.vanthucac.notification.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanthucac.common.outbox.OutboxEvent;
import com.vanthucac.common.outbox.OutboxEventHandler;
import com.vanthucac.notification.service.EmailNotificationService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(0)
public class AuctionWinnerEmailOutboxEventHandler implements OutboxEventHandler {

    private final ObjectMapper objectMapper;
    private final EmailNotificationService emailNotificationService;

    public AuctionWinnerEmailOutboxEventHandler(
            ObjectMapper objectMapper,
            EmailNotificationService emailNotificationService
    ) {
        this.objectMapper = objectMapper;
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    public boolean supports(String eventType) {
        return NotificationEventType.AUCTION_WINNER_EMAIL_REQUESTED.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        var payload = readPayload(event);

        emailNotificationService.sendAuctionWinnerNotification(
                payload.winnerEmail(),
                payload.winnerFullName(),
                payload.bookTitle(),
                new BigDecimal(payload.winningPrice()),
                payload.auctionItemId()
        );
    }

    private AuctionWinnerEmailPayload readPayload(OutboxEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(), AuctionWinnerEmailPayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid auction winner email outbox payload", ex);
        }
    }

    private record AuctionWinnerEmailPayload(
            Long auctionItemId,
            Long winnerId,
            String winnerEmail,
            String winnerFullName,
            String bookTitle,
            String winningPrice
    ) {
    }
}