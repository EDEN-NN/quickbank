package com.quickbank.common.event;

public final class EventTypes {

    public static final String ACCOUNT_OPENED = "AccountOpened";
    public static final String PAYMENT_REQUESTED = "PaymentRequested";
    public static final String PAYMENT_COMPLETED = "PaymentCompleted";
    public static final String DEPOSIT_POSTED = "DepositPosted";
    public static final String TRANSFER_POSTED = "TransferPosted";
    public static final String SEND_EMAIL = "SendEmail";

    private EventTypes() {
    }
}
