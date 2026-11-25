package com.quickbank.account.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record SnapshotView(
        String accountNumber,
        String ownerName,
        String ownerEmail,
        BigDecimal cash,
        BigDecimal creditLimit,
        BigDecimal creditUsed,
        Instant updatedAt
) {
    public BigDecimal creditAvailable() {
        return creditLimit.subtract(creditUsed).setScale(2, RoundingMode.UNNECESSARY);
    }

    public SnapshotView withCash(BigDecimal cash, Instant updatedAt) {
        return new SnapshotView(accountNumber, ownerName, ownerEmail, cash, creditLimit, creditUsed, updatedAt);
    }
}
