package com.quickbank.account.app;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;

public record StatementLine(
        String id,
        String accountNumber,
        Instant occurredAt,
        String type,
        BigDecimal amount,
        String counterparty,
        BigDecimal runningBalance,
        String transferId,
        String sourceEventId
) {
    public static final Comparator<StatementLine> CHRONOLOGICAL =
            Comparator.comparing(StatementLine::occurredAt).thenComparing(StatementLine::id);
}
