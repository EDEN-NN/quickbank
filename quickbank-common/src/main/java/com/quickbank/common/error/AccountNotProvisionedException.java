package com.quickbank.common.error;

public final class AccountNotProvisionedException extends QuickBankException {

    public AccountNotProvisionedException() {
        super(
                "account-not-ready",
                "Account is not ready",
                409,
                "Open an account first. If you just opened one, the ledger is still provisioning it. Retry in a moment.");
    }
}
