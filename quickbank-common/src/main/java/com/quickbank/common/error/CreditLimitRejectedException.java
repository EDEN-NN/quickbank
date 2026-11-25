package com.quickbank.common.error;

public final class CreditLimitRejectedException extends QuickBankException {

    public CreditLimitRejectedException() {
        super(
                "credit-limit-rejected",
                "Credit limit was not accepted",
                409,
                "The credit limit was not accepted.");
    }
}
