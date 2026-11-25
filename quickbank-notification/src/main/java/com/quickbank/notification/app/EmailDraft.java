package com.quickbank.notification.app;

import com.quickbank.common.event.payload.SendEmailPayload;

public record EmailDraft(String subject, String body) {

    public static EmailDraft from(SendEmailPayload payload) {
        String name = payload.recipientName() == null ? "there" : payload.recipientName();
        return switch (payload.template()) {
            case "ACCOUNT_OPENED" -> new EmailDraft(
                    "Your QuickBank account " + payload.accountNumber() + " is open",
                    "Hello " + name + ", account " + payload.accountNumber()
                            + " is open. Cash starts at 0.00 BRL. The credit line is a snapshot; transfers spend cash only.");
            case "DEPOSIT_POSTED" -> new EmailDraft(
                    "Deposit of " + payload.amount() + " " + payload.currency() + " posted",
                    "Hello " + name + ", " + payload.amount() + " " + payload.currency()
                            + " was credited to account " + payload.accountNumber() + ".");
            case "TRANSFER_SENT" -> new EmailDraft(
                    "You sent " + payload.amount() + " " + payload.currency() + " to " + payload.counterpartyAccount(),
                    "Hello " + name + ", account " + payload.accountNumber() + " sent " + payload.amount() + " "
                            + payload.currency() + " to account " + payload.counterpartyAccount() + ".");
            case "TRANSFER_RECEIVED" -> new EmailDraft(
                    "You received " + payload.amount() + " " + payload.currency() + " from " + payload.counterpartyAccount(),
                    "Hello " + name + ", account " + payload.accountNumber() + " received " + payload.amount() + " "
                            + payload.currency() + " from account " + payload.counterpartyAccount() + ".");
            default -> new EmailDraft(
                    "QuickBank notification",
                    "Hello " + name + ", QuickBank recorded template " + payload.template() + " for account " + payload.accountNumber() + ".");
        };
    }
}
