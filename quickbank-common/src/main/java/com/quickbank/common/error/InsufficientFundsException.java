package com.quickbank.common.error;

import java.math.BigDecimal;

public final class InsufficientFundsException extends QuickBankException {

    private final String accountNumber;
    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientFundsException(String accountNumber, BigDecimal available, BigDecimal requested) {
        super(
                "insufficient-funds",
                "Insufficient funds",
                422,
                "Account " + accountNumber + " has " + available.setScale(2) + " BRL in cash, which is less than "
                        + requested.setScale(2) + " BRL.");
        this.accountNumber = accountNumber;
        this.available = available;
        this.requested = requested;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public BigDecimal available() {
        return available;
    }

    public BigDecimal requested() {
        return requested;
    }
}
