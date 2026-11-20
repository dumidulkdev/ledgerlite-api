package com.ledgerlite.service;

import com.ledgerlite.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MoneyService {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount is required");
        }
        if (amount.scale() > SCALE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must have at most 2 decimal places");
        }
        BigDecimal normalized = amount.setScale(SCALE, ROUNDING);
        if (normalized.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        return normalized;
    }

    public BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    public BigDecimal add(BigDecimal left, BigDecimal right) {
        return left.add(right).setScale(SCALE, ROUNDING);
    }

    public BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return left.subtract(right).setScale(SCALE, ROUNDING);
    }

    public boolean isLessThan(BigDecimal value, BigDecimal other) {
        return value.compareTo(other) < 0;
    }
}