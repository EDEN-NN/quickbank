package com.quickbank.bootstrap.identity;

import com.quickbank.common.error.QuickBankException;

public final class InvalidRegistrationException extends QuickBankException {

    public InvalidRegistrationException() {
        super(
                "validation",
                "Request is not valid",
                400,
                "Registration was not accepted.");
    }
}
