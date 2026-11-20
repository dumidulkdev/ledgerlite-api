package com.ledgerlite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MoneyMovementRequest(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true, message = "amount must be positive")
        @Schema(example = "125.50")
        BigDecimal amount,
        @Schema(example = "Payroll November")
        String reference
) {
}