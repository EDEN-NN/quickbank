package com.quickbank.common.event.payload;

import java.time.Instant;

public record AccountOpenedPayload(
        String accountNumber,
        String ownerSubject,
        String ownerEmail,
        String ownerName,
        String creditLimit,
        String currency,
        Instant openedAt
) {
}
