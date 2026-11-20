package com.ledgerlite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferRequest(
        @NotNull UUID sourceWalletId,
        @NotNull UUID targetWalletId,
        @NotNull @DecimalMin(value = "0.01") @Schema(example = "40.00") BigDecimal amount,
        @Schema(example = "Rent split") String reference
) {
}