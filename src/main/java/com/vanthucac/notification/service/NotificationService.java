package com.vanthucac.notification.service;

import com.vanthucac.auth.entity.User;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.common.util.PageableUtils;
import com.vanthucac.notification.dto.NotificationResponse;
import com.vanthucac.notification.entity.Notification;
import com.vanthucac.notification.exception.NotificationException;
import com.vanthucac.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(
            boolean unreadOnly,
            int page,
            int size,
            Jwt jwt
    ) {
        var userId = extractUserId(jwt);
        var pageable = PageableUtils.build(page, size, "createdAt,desc");

        var notificationsPage = unreadOnly
                ? notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return PageResponse.from(notificationsPage.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long countUnread(Jwt jwt) {
        return notificationRepository.countByUserIdAndReadFalse(extractUserId(jwt));
    }

    @Transactional
    public void markAsRead(Long notificationId, Jwt jwt) {
        var userId = extractUserId(jwt);
        var notification = notificationRepository.findById(notificationId)
                .orElseThrow(NotificationException::notFound);

        if (!notification.getUser().getId().equals(userId)) {
            throw NotificationException.accessDenied();
        }

        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Jwt jwt) {
        notificationRepository.markAllAsRead(extractUserId(jwt));
    }

    @Transactional
    public void createNotification(
            User user,
            Notification.NotificationType type,
            String title,
            String content
    ) {
        var notification = Notification.create(user, type, title, content);
        notificationRepository.save(notification);
        log.debug("Notification saved for user {} — type {}", user.getId(), type);
    }

    private Long extractUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}