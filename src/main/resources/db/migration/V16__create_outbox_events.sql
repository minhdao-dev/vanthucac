CREATE TABLE outbox_events
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type     VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    retry_count    INT          NOT NULL DEFAULT 0,
    max_retries    INT          NOT NULL DEFAULT 5,
    next_retry_at  DATETIME(6) NOT NULL,
    processed_at   DATETIME(6),
    last_error     VARCHAR(1000),
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL
);

CREATE INDEX idx_outbox_events_status_next_retry
    ON outbox_events (status, next_retry_at);

CREATE INDEX idx_outbox_events_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

CREATE INDEX idx_outbox_events_event_type
    ON outbox_events (event_type);