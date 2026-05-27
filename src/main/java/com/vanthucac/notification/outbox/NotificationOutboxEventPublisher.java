package com.vanthucac.notification.outbox;

import com.vanthucac.common.outbox.OutboxEventService;
import com.vanthucac.notification.entity.Notification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class NotificationOutboxEventPublisher {

    private static final String AUCTION_ITEM_AGGREGATE = "AUCTION_ITEM";
    private static final String NOTIFICATION_AGGREGATE = "NOTIFICATION";
    private static final String LISTING_AGGREGATE = "LISTING";

    private final OutboxEventService outboxEventService;

    public NotificationOutboxEventPublisher(OutboxEventService outboxEventService) {
        this.outboxEventService = outboxEventService;
    }

    public void publishAuctionWinnerEmail(
            Long auctionItemId,
            Long winnerId,
            String winnerEmail,
            String winnerFullName,
            String bookTitle,
            BigDecimal winningPrice
    ) {
        outboxEventService.publish(
                NotificationEventType.AUCTION_WINNER_EMAIL_REQUESTED,
                AUCTION_ITEM_AGGREGATE,
                auctionItemId,
                Map.of(
                        "auctionItemId", auctionItemId,
                        "winnerId", winnerId,
                        "winnerEmail", winnerEmail,
                        "winnerFullName", winnerFullName,
                        "bookTitle", bookTitle,
                        "winningPrice", winningPrice.toPlainString()
                )
        );
    }

    public void publishAuctionWonNotification(
            Long auctionItemId,
            Long winnerId,
            String bookTitle
    ) {
        publishNotification(
                winnerId,
                Notification.NotificationType.AUCTION_WON,
                "Bạn đã thắng đấu giá!",
                "Chúc mừng! Bạn đã thắng phiên đấu giá cho cuốn \"" + bookTitle + "\".",
                AUCTION_ITEM_AGGREGATE,
                auctionItemId
        );
    }

    public void publishAuctionOutbidNotification(
            Long auctionItemId,
            Long userId,
            String bookTitle
    ) {
        publishNotification(
                userId,
                Notification.NotificationType.AUCTION_OUTBID,
                "Bạn đã bị outbid",
                "Ai đó vừa đặt giá cao hơn bạn cho cuốn \"" + bookTitle + "\". Đặt lại ngay!",
                AUCTION_ITEM_AGGREGATE,
                auctionItemId
        );
    }

    public void publishListingApprovedNotification(
            Long userId,
            Long listingId
    ) {
        publishNotification(
                userId,
                Notification.NotificationType.LISTING_APPROVED,
                "Listing đã được duyệt",
                "Listing #" + listingId + " đã được admin duyệt và hiển thị cho người mua.",
                LISTING_AGGREGATE,
                listingId
        );
    }

    public void publishListingRejectedNotification(
            Long userId,
            Long listingId,
            String reason
    ) {
        publishNotification(
                userId,
                Notification.NotificationType.LISTING_REJECTED,
                "Listing bị từ chối",
                "Listing #" + listingId + " bị từ chối. Lý do: " + reason,
                LISTING_AGGREGATE,
                listingId
        );
    }

    private void publishNotification(
            Long userId,
            Notification.NotificationType notificationType,
            String title,
            String content,
            String sourceType,
            Long sourceId
    ) {
        outboxEventService.publish(
                NotificationEventType.NOTIFICATION_REQUESTED,
                NOTIFICATION_AGGREGATE,
                sourceId,
                Map.of(
                        "userId", userId,
                        "notificationType", notificationType.name(),
                        "title", title,
                        "content", content,
                        "sourceType", sourceType,
                        "sourceId", sourceId
                )
        );
    }
}