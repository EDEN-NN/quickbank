package com.quickbank.common.money;

import com.quickbank.common.error.InvalidAccountNumberException;

import java.util.regex.Pattern;

public final class AccountNumbers {

    private static final Pattern PATTERN = Pattern.compile("[1-9][0-9]{7}");

    private AccountNumbers() {
    }

    public static String require(String raw) {
        if (raw == null || !PATTERN.matcher(raw.trim()).matches()) {
            throw new InvalidAccountNumberException("Account numbers are 8 digits and do not start with zero.");
        }
        return raw.trim();
    }

    public static String fromSequence(long value) {
        if (value < 10_000_000L || value > 99_999_999L) {
            throw new IllegalStateException("Account number sequence is outside the 8-digit range.");
        }
        return Long.toString(value);
    }
}
