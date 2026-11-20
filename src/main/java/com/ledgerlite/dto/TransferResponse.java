package com.ledgerlite.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceWalletId,
        UUID targetWalletId,
        BigDecimal amount,
        String reference,
        Instant createdAt
) {
}