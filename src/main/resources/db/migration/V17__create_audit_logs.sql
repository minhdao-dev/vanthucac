CREATE TABLE audit_logs
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id      BIGINT,
    actor_email   VARCHAR(255),
    actor_role    VARCHAR(50),
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id   BIGINT,
    description   VARCHAR(1000),
    ip_address    VARCHAR(100),
    user_agent    VARCHAR(500),
    created_at    DATETIME(6) NOT NULL
);

CREATE INDEX idx_audit_logs_actor_id
    ON audit_logs (actor_id);

CREATE INDEX idx_audit_logs_action
    ON audit_logs (action);

CREATE INDEX idx_audit_logs_resource
    ON audit_logs (resource_type, resource_id);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs (created_at);