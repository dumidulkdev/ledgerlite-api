CREATE TABLE ledger_transactions (
    id           UUID PRIMARY KEY,
    type         VARCHAR(32) NOT NULL,
    reference    VARCHAR(255),
    created_at   TIMESTAMP NOT NULL
);

CREATE TABLE ledger_entries (
    id               UUID PRIMARY KEY,
    transaction_id   UUID NOT NULL REFERENCES ledger_transactions (id),
    wallet_id        UUID,
    account_type     VARCHAR(32) NOT NULL,
    entry_type       VARCHAR(16) NOT NULL,
    amount           NUMERIC(19, 2) NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    CONSTRAINT fk_ledger_entry_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id)
);

CREATE INDEX idx_ledger_entries_wallet_id ON ledger_entries (wallet_id);
CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries (transaction_id);
CREATE INDEX idx_ledger_entries_created_at ON ledger_entries (created_at DESC);

CREATE TABLE transfers (
    id                UUID PRIMARY KEY,
    source_wallet_id  UUID NOT NULL REFERENCES wallets (id),
    target_wallet_id  UUID NOT NULL REFERENCES wallets (id),
    amount            NUMERIC(19, 2) NOT NULL,
    reference         VARCHAR(255),
    ledger_transaction_id UUID NOT NULL REFERENCES ledger_transactions (id),
    created_at        TIMESTAMP NOT NULL
);

CREATE INDEX idx_transfers_created_at ON transfers (created_at DESC);