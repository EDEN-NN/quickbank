package com.quickbank.ledger.app;

import java.math.BigDecimal;
import java.time.Instant;

public record StoredDeposit(
        String id,
        String idempotencyKey,
        String ownerSubject,
        String accountNumber,
        BigDecimal amount,
        String status,
        Instant createdAt,
        Instant postedAt
) {
    public boolean matches(String owner, BigDecimal candidate) {
        return ownerSubject.equals(owner) && amount.compareTo(candidate) == 0;
    }

    public boolean posted() {
        return "POSTED".equals(status);
    }

    public StoredDeposit postedAt(Instant when) {
        return new StoredDeposit(id, idempotencyKey, ownerSubject, accountNumber, amount, "POSTED", createdAt, when);
    }
}
