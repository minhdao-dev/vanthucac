package com.vanthucac.notification.dto;

import com.vanthucac.notification.entity.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String content,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getContent(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}