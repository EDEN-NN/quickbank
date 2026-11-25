package com.quickbank.account.app;

import com.quickbank.common.error.CreditLimitRejectedException;
import com.quickbank.common.error.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public final class CreditLimits {

    private static final Pattern TEXT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private CreditLimits() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || !TEXT.matcher(raw.trim()).matches()) {
            throw new InvalidAmountException("Credit limit must be a non-negative amount with at most two decimal places.");
        }
        return new BigDecimal(raw.trim()).setScale(2, RoundingMode.UNNECESSARY);
    }

    public static void ensureWithin(BigDecimal requested, BigDecimal maximum, BigDecimal used) {
        if (requested.signum() < 0 || requested.compareTo(maximum) > 0 || requested.compareTo(used) < 0) {
            throw new CreditLimitRejectedException();
        }
    }
}
