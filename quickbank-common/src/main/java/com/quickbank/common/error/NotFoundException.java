package com.quickbank.common.error;

public final class NotFoundException extends QuickBankException {

    public NotFoundException(String detail) {
        super("not-found", "Not found", 404, detail);
    }
}
