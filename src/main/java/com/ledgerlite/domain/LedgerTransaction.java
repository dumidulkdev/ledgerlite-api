package com.ledgerlite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LedgerTransactionType type;

    @Column(length = 255)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerTransaction() {
    }

    public LedgerTransaction(UUID id, LedgerTransactionType type, String reference, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.reference = reference;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public LedgerTransactionType getType() {
        return type;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}