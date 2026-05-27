package com.vanthucac.notification.controller;

import com.vanthucac.common.dto.ApiResponse;
import com.vanthucac.common.dto.PageResponse;
import com.vanthucac.notification.dto.NotificationResponse;
import com.vanthucac.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var notifications = notificationService.getMyNotifications(unreadOnly, page, size, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt
    ) {
        var count = notificationService.countUnread(jwt);
        return ResponseEntity.ok(ApiResponse.ok("Unread count retrieved", count));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        notificationService.markAsRead(notificationId, jwt);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt
    ) {
        notificationService.markAllAsRead(jwt);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }
}