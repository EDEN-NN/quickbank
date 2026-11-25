package com.quickbank.common.error;

public final class SelfTransferException extends QuickBankException {

    public SelfTransferException(String accountNumber) {
        super(
                "self-transfer",
                "Transfer to the same account",
                422,
                "Account " + accountNumber + " cannot transfer to itself.");
    }
}
