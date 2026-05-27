package com.vanthucac.notification.outbox;

public final class NotificationEventType {

    private NotificationEventType() {
    }

    public static final String AUCTION_WINNER_EMAIL_REQUESTED = "AUCTION_WINNER_EMAIL_REQUESTED";
    public static final String NOTIFICATION_REQUESTED = "NOTIFICATION_REQUESTED";
}