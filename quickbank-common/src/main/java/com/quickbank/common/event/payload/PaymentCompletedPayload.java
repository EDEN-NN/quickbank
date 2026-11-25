package com.quickbank.common.event.payload;

public record PaymentCompletedPayload(
        String paymentId,
        String status,
        String accountNumber,
        String amount,
        String currency,
        String idempotencyKey
) {
}
