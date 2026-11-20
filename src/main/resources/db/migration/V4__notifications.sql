CREATE TABLE notification_logs (
    id           UUID PRIMARY KEY,
    event_type   VARCHAR(64)  NOT NULL,
    payload      CLOB         NOT NULL,
    created_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_notification_logs_created_at ON notification_logs (created_at DESC);