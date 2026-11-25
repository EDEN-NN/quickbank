package com.quickbank.common.error;

public final class InvalidAccountNumberException extends QuickBankException {

    public InvalidAccountNumberException(String detail) {
        super("invalid-account-number", "Invalid account number", 400, detail);
    }
}
