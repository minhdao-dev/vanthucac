package com.vanthucac.auction.entity;

import com.vanthucac.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "auction_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AuctionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
    private Set<AuctionItem> items = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum SessionStatus {
        SCHEDULED, ACTIVE, CLOSED
    }

    public static AuctionSession create(String title, Instant startTime,
                                        Instant endTime, User createdBy) {
        var session = new AuctionSession();
        session.title = title;
        session.startTime = startTime;
        session.endTime = endTime;
        session.createdBy = createdBy;
        return session;
    }

    public void activate() {
        this.status = SessionStatus.ACTIVE;
    }

    public void close() {
        this.status = SessionStatus.CLOSED;
    }

    public boolean isScheduled() {
        return status == SessionStatus.SCHEDULED;
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }
}