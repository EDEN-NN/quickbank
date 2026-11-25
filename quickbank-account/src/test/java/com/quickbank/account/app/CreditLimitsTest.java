package com.quickbank.account.app;

import com.quickbank.common.error.CreditLimitRejectedException;
import com.quickbank.common.error.InvalidAmountException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreditLimitsTest {

    @Test
    void zeroIsAllowed() {
        assertEquals(new BigDecimal("0.00"), CreditLimits.parse("0"));
        CreditLimits.ensureWithin(new BigDecimal("0.00"), new BigDecimal("2000.00"), new BigDecimal("0.00"));
    }

    @Test
    void negativeAndExtraScaleAreRejected() {
        assertThrows(InvalidAmountException.class, () -> CreditLimits.parse("-1.00"));
        assertThrows(InvalidAmountException.class, () -> CreditLimits.parse("10.999"));
    }

    @Test
    void aboveTheAccountMaximumStaysGeneric() {
        CreditLimitRejectedException ex = assertThrows(
                CreditLimitRejectedException.class,
                () -> CreditLimits.ensureWithin(new BigDecimal("2000.01"), new BigDecimal("2000.00"), BigDecimal.ZERO));
        assertEquals("The credit limit was not accepted.", ex.getMessage());
        assertFalse(ex.getMessage().contains("2000"));
    }

    @Test
    void belowTheAmountInUseIsRejected() {
        assertThrows(
                CreditLimitRejectedException.class,
                () -> CreditLimits.ensureWithin(new BigDecimal("10.00"), new BigDecimal("2000.00"), new BigDecimal("50.00")));
    }
}
