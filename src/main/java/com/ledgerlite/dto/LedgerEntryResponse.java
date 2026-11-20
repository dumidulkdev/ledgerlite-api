package com.ledgerlite.dto;

import com.ledgerlite.domain.LedgerAccountType;
import com.ledgerlite.domain.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID transactionId,
        UUID walletId,
        LedgerAccountType accountType,
        LedgerEntryType entryType,
        BigDecimal amount,
        Instant createdAt
) {
}