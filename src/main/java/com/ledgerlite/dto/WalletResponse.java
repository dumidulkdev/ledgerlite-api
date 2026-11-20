package com.ledgerlite.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        String ownerName,
        String currency,
        BigDecimal balance,
        Instant createdAt,
        Instant updatedAt
) {
}