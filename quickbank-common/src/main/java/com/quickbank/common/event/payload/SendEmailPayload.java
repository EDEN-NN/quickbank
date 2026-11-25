package com.quickbank.common.event.payload;

import java.time.Instant;

public record SendEmailPayload(
        String template,
        String recipient,
        String recipientName,
        String accountNumber,
        String counterpartyAccount,
        String amount,
        String currency,
        Instant occurredAt
) {
}
