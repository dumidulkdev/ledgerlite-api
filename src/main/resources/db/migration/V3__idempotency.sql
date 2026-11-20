CREATE TABLE idempotency_records (
    id              UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    endpoint        VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    response_status INT,
    response_body   CLOB,
    completed       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key)
);