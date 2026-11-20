package com.ledgerlite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    private UUID id;

    @Column(name = "source_wallet_id", nullable = false)
    private UUID sourceWalletId;

    @Column(name = "target_wallet_id", nullable = false)
    private UUID targetWalletId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String reference;

    @Column(name = "ledger_transaction_id", nullable = false)
    private UUID ledgerTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    public Transfer(UUID id, UUID sourceWalletId, UUID targetWalletId, BigDecimal amount, String reference,
                    UUID ledgerTransactionId, Instant createdAt) {
        this.id = id;
        this.sourceWalletId = sourceWalletId;
        this.targetWalletId = targetWalletId;
        this.amount = amount;
        this.reference = reference;
        this.ledgerTransactionId = ledgerTransactionId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceWalletId() {
        return sourceWalletId;
    }

    public UUID getTargetWalletId() {
        return targetWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReference() {
        return reference;
    }

    public UUID getLedgerTransactionId() {
        return ledgerTransactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}