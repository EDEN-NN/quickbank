package com.quickbank.common.error;

public final class InvalidIdempotencyKeyException extends QuickBankException {

    public InvalidIdempotencyKeyException(String detail) {
        super("invalid-idempotency-key", "Invalid idempotency key", 400, detail);
    }
}
