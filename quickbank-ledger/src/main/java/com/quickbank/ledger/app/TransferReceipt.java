package com.quickbank.ledger.app;

public record TransferReceipt(
        String transferId,
        String fromAccount,
        String toAccount,
        String amount,
        String fromBalanceAfter,
        String toBalanceAfter,
        String currency
) {
}
