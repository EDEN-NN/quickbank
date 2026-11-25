package com.quickbank.bootstrap.identity;

import com.quickbank.common.error.QuickBankException;

public final class EmailAlreadyRegisteredException extends QuickBankException {

    public EmailAlreadyRegisteredException() {
        super(
                "email-already-registered",
                "Registration was not accepted",
                409,
                "Registration was not accepted.");
    }
}
