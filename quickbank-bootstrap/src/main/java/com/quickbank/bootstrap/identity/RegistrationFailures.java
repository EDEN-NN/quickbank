package com.quickbank.bootstrap.identity;

final class RegistrationFailures {

    private RegistrationFailures() {
    }

    static RuntimeException fromConstraint(String message) {
        if (message != null && message.contains("uk_id_email")) {
            return new EmailAlreadyRegisteredException();
        }
        return new RegistrationRejectedException();
    }
}
