package com.quickbank.common.money;

import com.quickbank.common.error.InvalidIdempotencyKeyException;

import java.util.regex.Pattern;

public final class IdempotencyKeys {

    private static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,80}");

    private IdempotencyKeys() {
    }

    public static String require(String raw) {
        if (raw == null || !PATTERN.matcher(raw.trim()).matches()) {
            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key must be 8 to 80 characters and use only letters, digits, and . _ : -.");
        }
        return raw.trim();
    }
}
