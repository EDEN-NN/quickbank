package com.quickbank.common.money;

import com.quickbank.common.error.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Currency amounts are {@link BigDecimal} with scale 2. Nothing in QuickBank stores or computes money as float or double.
 */
public final class Money {

    public static final String CURRENCY = "BRL";
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    public static final BigDecimal MAX = new BigDecimal("1000000.00");

    private static final Pattern TEXT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private Money() {
    }

    public static BigDecimal amount(BigDecimal value) {
        if (value == null) {
            throw new InvalidAmountException("Amount is required.");
        }
        BigDecimal scaled;
        try {
            scaled = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidAmountException("Amounts use at most two decimal places.");
        }
        if (scaled.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (scaled.compareTo(MAX) > 0) {
            throw new InvalidAmountException("Amount cannot exceed 1000000.00 BRL.");
        }
        return scaled;
    }

    public static BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            throw new InvalidAmountException("Amount is required.");
        }
        try {
            BigDecimal scaled = value.setScale(2, RoundingMode.UNNECESSARY);
            if (scaled.signum() < 0) {
                throw new InvalidAmountException("Balance cannot be negative.");
            }
            return scaled;
        } catch (ArithmeticException ex) {
            throw new InvalidAmountException("Amounts use at most two decimal places.");
        }
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidAmountException("Amount is required.");
        }
        String trimmed = raw.trim();
        if (!TEXT.matcher(trimmed).matches()) {
            throw new InvalidAmountException(
                    "Use a positive amount with a dot and at most two decimal places, for example 100.50.");
        }
        return amount(new BigDecimal(trimmed));
    }

    public static String format(BigDecimal value) {
        return nonNegative(value).toPlainString();
    }

    public static String display(BigDecimal value) {
        var format = java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        format.setCurrency(Currency.getInstance(CURRENCY));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(nonNegative(value));
    }
}
