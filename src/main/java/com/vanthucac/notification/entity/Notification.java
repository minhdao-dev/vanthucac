package com.vanthucac.notification.entity;

import com.vanthucac.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum NotificationType {
        ORDER_PLACED,
        ORDER_CONFIRMED,
        ORDER_COMPLETED,
        ORDER_CANCELLED,
        AUCTION_WON,
        AUCTION_OUTBID,
        LISTING_APPROVED,
        LISTING_REJECTED,
        WALLET_CREDITED
    }

    public static Notification create(User user, NotificationType type,
                                      String title, String content) {
        var notification = new Notification();
        notification.user = user;
        notification.type = type;
        notification.title = title;
        notification.content = content;
        return notification;
    }

    public void markAsRead() {
        this.read = true;
    }
}