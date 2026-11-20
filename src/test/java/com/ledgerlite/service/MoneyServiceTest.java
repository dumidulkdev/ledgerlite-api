package com.ledgerlite.service;

import com.ledgerlite.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyServiceTest {

    private final MoneyService moneyService = new MoneyService();

    @Test
    void keepsScaleAtTwoDecimals() {
        assertEquals(new BigDecimal("2.50"), moneyService.normalize(new BigDecimal("2.5")));
        assertEquals(new BigDecimal("10.00"), moneyService.add(new BigDecimal("4.50"), new BigDecimal("5.50")));
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThrows(ApiException.class, () -> moneyService.normalize(new BigDecimal("1.234")));
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThrows(ApiException.class, () -> moneyService.normalize(BigDecimal.ZERO));
    }
}