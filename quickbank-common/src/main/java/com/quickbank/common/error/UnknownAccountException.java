package com.quickbank.common.error;

public final class UnknownAccountException extends QuickBankException {

    private final String accountNumber;

    public UnknownAccountException(String accountNumber) {
        super(
                "unknown-account",
                "Unknown account",
                404,
                "No account uses the number " + accountNumber + ". If it was just opened, wait a second and retry.");
        this.accountNumber = accountNumber;
    }

    public String accountNumber() {
        return accountNumber;
    }
}
