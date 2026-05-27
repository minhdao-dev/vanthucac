package com.vanthucac.notification.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vanthucac.auth.repository.UserRepository;
import com.vanthucac.common.outbox.OutboxEvent;
import com.vanthucac.common.outbox.OutboxEventHandler;
import com.vanthucac.notification.entity.Notification;
import com.vanthucac.notification.service.NotificationService;
import com.vanthucac.user.exception.UserException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class NotificationOutboxEventHandler implements OutboxEventHandler {

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
        return NotificationEventType.NOTIFICATION_REQUESTED.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        var payload = readPayload(event);
        var user = userRepository.findById(payload.userId())
                .orElseThrow(UserException::userNotFound);

        notificationService.createNotification(
                user,
                Notification.NotificationType.valueOf(payload.notificationType()),
                payload.title(),
                payload.content()
        );
    }

    private NotificationPayload readPayload(OutboxEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(), NotificationPayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid notification outbox payload", ex);
        }
    }

    private record NotificationPayload(
            Long userId,
            String notificationType,
            String title,
            String content,
            String sourceType,
            Long sourceId
    ) {
    }
}