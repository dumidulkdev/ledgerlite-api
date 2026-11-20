package com.ledgerlite.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;

public record StatementResponse(
        Instant from,
        Instant to,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        Page<LedgerEntryResponse> entries
) {
}