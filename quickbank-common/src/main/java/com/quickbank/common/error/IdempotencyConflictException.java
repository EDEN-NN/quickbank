package com.quickbank.common.error;

public final class IdempotencyConflictException extends QuickBankException {

    public IdempotencyConflictException() {
        super(
                "idempotency-conflict",
                "Idempotency key reused",
                409,
                "This Idempotency-Key was already used for a different amount or account.");
    }
}
