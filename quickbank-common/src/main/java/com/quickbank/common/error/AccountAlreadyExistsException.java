package com.quickbank.common.error;

public final class AccountAlreadyExistsException extends QuickBankException {

    public AccountAlreadyExistsException(String accountNumber) {
        super(
                "account-exists",
                "Account already open",
                409,
                "This user already has account " + accountNumber + ".");
    }
}
