package com.quickbank.ledger.app;

import java.math.BigDecimal;

public record CustomerBook(
        String accountNumber,
        String ownerSubject,
        String ownerEmail,
        String ownerName,
        BigDecimal cashBalance,
        String kind
) {
    public CustomerBook withBalance(BigDecimal balance) {
        return new CustomerBook(accountNumber, ownerSubject, ownerEmail, ownerName, balance, kind);
    }

    public boolean customer() {
        return "CUSTOMER".equals(kind);
    }
}
