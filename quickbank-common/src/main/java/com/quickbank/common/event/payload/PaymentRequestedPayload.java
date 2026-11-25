package com.quickbank.common.event.payload;

public record PaymentRequestedPayload(
        String paymentId,
        String accountNumber,
        String amount,
        String currency,
        String idempotencyKey,
        String reason
) {
}
