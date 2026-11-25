package com.quickbank.common.error;

public final class InvalidAmountException extends QuickBankException {

    public InvalidAmountException(String detail) {
        super("invalid-amount", "Invalid amount", 400, detail);
    }
}
