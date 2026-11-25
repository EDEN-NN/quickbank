package com.quickbank.notification.app;

import com.quickbank.common.event.payload.SendEmailPayload;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailDraftTest {

    @Test
    void draftsTheFourCustomerEmails() {
        Instant when = Instant.parse("2026-11-24T15:00:00Z");
        EmailDraft opened = EmailDraft.from(new SendEmailPayload(
                "ACCOUNT_OPENED", "ana@quickbank.local", "ana", "10000001", null, null, "BRL", when));
        EmailDraft deposit = EmailDraft.from(new SendEmailPayload(
                "DEPOSIT_POSTED", "ana@quickbank.local", "ana", "10000001", null, "50.00", "BRL", when));
        EmailDraft sent = EmailDraft.from(new SendEmailPayload(
                "TRANSFER_SENT", "ana@quickbank.local", "ana", "10000001", "10000002", "10.00", "BRL", when));
        EmailDraft received = EmailDraft.from(new SendEmailPayload(
                "TRANSFER_RECEIVED", "bruno@quickbank.local", "bruno", "10000002", "10000001", "10.00", "BRL", when));

        assertTrue(opened.subject().contains("10000001"));
        assertTrue(opened.body().contains("cash"));
        assertTrue(deposit.body().contains("50.00 BRL"));
        assertTrue(sent.subject().contains("10000002"));
        assertTrue(received.body().contains("from account 10000001"));
    }
}
