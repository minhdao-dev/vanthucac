package com.vanthucac.notification.exception;

import com.vanthucac.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class NotificationException extends BusinessException {

    public NotificationException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public static NotificationException notFound() {
        return new NotificationException(
                "Notification not found",
                "NOTIFICATION_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public static NotificationException accessDenied() {
        return new NotificationException(
                "You do not have access to this notification",
                "NOTIFICATION_ACCESS_DENIED",
                HttpStatus.FORBIDDEN
        );
    }
}