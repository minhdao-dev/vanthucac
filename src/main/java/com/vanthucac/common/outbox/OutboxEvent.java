package com.vanthucac.common.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_events_status_next_retry", columnList = "status, next_retry_at"),
                @Index(name = "idx_outbox_events_aggregate", columnList = "aggregate_type, aggregate_id"),
                @Index(name = "idx_outbox_events_event_type", columnList = "event_type")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

    private static final int DEFAULT_MAX_RETRIES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = DEFAULT_MAX_RETRIES;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum OutboxStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        FAILED
    }

    public static OutboxEvent create(
            String eventType,
            String aggregateType,
            Long aggregateId,
            String payload
    ) {
        var event = new OutboxEvent();
        event.eventType = eventType;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        event.retryCount = 0;
        event.maxRetries = DEFAULT_MAX_RETRIES;
        event.nextRetryAt = Instant.now();
        return event;
    }

    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public void markProcessed() {
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String errorMessage, Instant nextRetryAt) {
        this.retryCount++;
        this.lastError = truncateError(errorMessage);
        this.nextRetryAt = nextRetryAt;
        this.status = retryCount >= maxRetries ? OutboxStatus.FAILED : OutboxStatus.PENDING;
    }

    public boolean canProcess() {
        return status == OutboxStatus.PENDING || status == OutboxStatus.FAILED;
    }

    private String truncateError(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() <= 1000 ? errorMessage : errorMessage.substring(0, 1000);
    }
}