package com.quickbank.common.money;

import com.quickbank.common.error.InvalidAmountException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void normalizesWholeNumbersToScaleTwo() {
        assertEquals(new BigDecimal("10.00"), Money.amount(new BigDecimal("10")));
        assertEquals("10.50", Money.format(Money.parse("10.5")));
    }

    @Test
    void rejectsFractionsBeyondCentavos() {
        assertThrows(InvalidAmountException.class, () -> Money.amount(new BigDecimal("10.005")));
        assertThrows(InvalidAmountException.class, () -> Money.parse("10.555"));
    }

    @Test
    void rejectsZeroNegativeAndHugeAmounts() {
        assertThrows(InvalidAmountException.class, () -> Money.amount(new BigDecimal("0.00")));
        assertThrows(InvalidAmountException.class, () -> Money.parse("-5.00"));
        assertThrows(InvalidAmountException.class, () -> Money.parse("1000000.01"));
    }

    @Test
    void displayUsesRealCurrencyNotAFloat() {
        String shown = Money.display(new BigDecimal("1234.50"));
        org.junit.jupiter.api.Assertions.assertTrue(shown.contains("1.234,50") || shown.contains("1,234.50"));
    }
}
