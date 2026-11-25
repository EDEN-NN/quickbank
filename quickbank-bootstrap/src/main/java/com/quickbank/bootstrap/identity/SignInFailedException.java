package com.quickbank.bootstrap.identity;

import com.quickbank.common.error.QuickBankException;

public final class SignInFailedException extends QuickBankException {

    public SignInFailedException() {
        super(
                "sign-in-failed",
                "Sign in failed",
                401,
                "The credentials were not accepted.");
    }
}
