package com.quickbank.common.outbox;

import java.util.Set;

public final class OutboxTables {

    public static final String ACCOUNT = "acc_outbox";
    public static final String LEDGER = "led_outbox";
    public static final String PAYMENT = "pay_outbox";

    private static final Set<String> ALLOWED = Set.of(ACCOUNT, LEDGER, PAYMENT);

    private OutboxTables() {
    }

    public static String require(String table) {
        if (!ALLOWED.contains(table)) {
            throw new IllegalArgumentException("Refusing to touch an outbox table that QuickBank does not own.");
        }
        return table;
    }
}
