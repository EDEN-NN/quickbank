package com.quickbank.common.event.payload;

import java.time.Instant;

public record TransferPostedPayload(
        String transferId,
        String fromAccount,
        String toAccount,
        String amount,
        String currency,
        String fromRunningBalance,
        String toRunningBalance,
        Instant postedAt
) {
}
