package com.quickbank.common.event;

import java.util.List;

public final class Topics {

    public static final String ACCOUNT_EVENTS = "quickbank.account.events";
    public static final String LEDGER_EVENTS = "quickbank.ledger.events";
    public static final String PAYMENT_COMMANDS = "quickbank.payment.commands";
    public static final String PAYMENT_EVENTS = "quickbank.payment.events";
    public static final String NOTIFICATION_COMMANDS = "quickbank.notification.commands";

    private Topics() {
    }

    public static List<String> all() {
        return List.of(ACCOUNT_EVENTS, LEDGER_EVENTS, PAYMENT_COMMANDS, PAYMENT_EVENTS, NOTIFICATION_COMMANDS);
    }
}
