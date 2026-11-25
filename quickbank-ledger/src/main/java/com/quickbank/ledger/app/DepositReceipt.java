package com.quickbank.ledger.app;

public record DepositReceipt(
        String depositId,
        String status,
        String accountNumber,
        String amount,
        String currency
) {
}
