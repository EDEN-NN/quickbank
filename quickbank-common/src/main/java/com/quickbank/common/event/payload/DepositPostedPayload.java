package com.quickbank.common.event.payload;

import java.time.Instant;

public record DepositPostedPayload(
        String depositId,
        String accountNumber,
        String amount,
        String currency,
        String runningBalance,
        Instant postedAt
) {
}
