package com.quickbank.bootstrap.identity;

import com.quickbank.common.error.QuickBankException;

public final class RegistrationRejectedException extends QuickBankException {

    public RegistrationRejectedException() {
        super(
                "registration-rejected",
                "Registration was not accepted",
                409,
                "Registration was not accepted.");
    }
}
