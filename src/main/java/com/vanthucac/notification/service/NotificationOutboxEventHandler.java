package com.vanthucac.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.common.outbox.OutboxEvent;
import com.vanthucac.common.outbox.OutboxEventHandler;
import com.vanthucac.notification.entity.Notification;
import com.vanthucac.user.exception.UserException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class NotificationOutboxEventHandler implements OutboxEventHandler {

    private static final String AUCTION_WON_EVENT = "AUCTION_WON_NOTIFICATION_REQUESTED";

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public NotificationOutboxEventHandler(
            ObjectMapper objectMapper,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public boolean supports(String eventType) {
        return AUCTION_WON_EVENT.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        var payload = readAuctionWonPayload(event);
        var winner = userRepository.findById(payload.winnerId())
                .orElseThrow(UserException::userNotFound);

        notificationService.createNotification(
                winner,
                Notification.NotificationType.AUCTION_WON,
                "Bạn đã thắng đấu giá!",
                "Chúc mừng! Bạn đã thắng phiên đấu giá cho cuốn \"" + payload.bookTitle() + "\"."
        );
    }

    private AuctionWonNotificationPayload readAuctionWonPayload(OutboxEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(), AuctionWonNotificationPayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid auction won notification outbox payload", ex);
        }
    }

    private record AuctionWonNotificationPayload(
            Long auctionItemId,
            Long winnerId,
            String bookTitle
    ) {
    }
}