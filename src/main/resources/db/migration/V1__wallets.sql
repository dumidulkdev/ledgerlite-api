CREATE TABLE wallets (
    id          UUID PRIMARY KEY,
    owner_name  VARCHAR(255) NOT NULL,
    currency    VARCHAR(3)    NOT NULL,
    balance     NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL
);

CREATE INDEX idx_wallets_owner_name ON wallets (owner_name);