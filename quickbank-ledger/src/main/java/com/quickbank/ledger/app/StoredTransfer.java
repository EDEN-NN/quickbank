package com.quickbank.ledger.app;

import java.math.BigDecimal;
import java.time.Instant;

public record StoredTransfer(
        String id,
        String idempotencyKey,
        String ownerSubject,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        BigDecimal fromBalanceAfter,
        BigDecimal toBalanceAfter,
        Instant createdAt
) {
    public boolean matches(String owner, String destination, BigDecimal candidate) {
        return ownerSubject.equals(owner) && toAccount.equals(destination) && amount.compareTo(candidate) == 0;
    }
}
