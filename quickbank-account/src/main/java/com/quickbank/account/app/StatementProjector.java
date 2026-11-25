package com.quickbank.account.app;

import com.quickbank.common.event.payload.DepositPostedPayload;
import com.quickbank.common.event.payload.SendEmailPayload;
import com.quickbank.common.event.payload.TransferPostedPayload;
import com.quickbank.common.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class StatementProjector {

    private StatementProjector() {
    }

    public static Projection deposit(SnapshotView current, DepositPostedPayload payload, String eventId) {
        BigDecimal amount = Money.amount(new BigDecimal(payload.amount()));
        BigDecimal running = Money.nonNegative(new BigDecimal(payload.runningBalance()));
        StatementLine line = new StatementLine(
                UUID.randomUUID().toString(),
                payload.accountNumber(),
                payload.postedAt(),
                "DEPOSIT",
                amount,
                "External deposit",
                running,
                null,
                eventId);
        SendEmailPayload email = new SendEmailPayload(
                "DEPOSIT_POSTED",
                current.ownerEmail(),
                current.ownerName(),
                current.accountNumber(),
                null,
                Money.format(amount),
                Money.CURRENCY,
                payload.postedAt());
        return new Projection(current.withCash(running, payload.postedAt()), List.of(line), List.of(email));
    }

    public static List<Projection> transfer(
            SnapshotView source,
            SnapshotView destination,
            TransferPostedPayload payload,
            String eventId
    ) {
        BigDecimal amount = Money.amount(new BigDecimal(payload.amount()));
        BigDecimal fromRunning = Money.nonNegative(new BigDecimal(payload.fromRunningBalance()));
        BigDecimal toRunning = Money.nonNegative(new BigDecimal(payload.toRunningBalance()));
        StatementLine out = new StatementLine(
                UUID.randomUUID().toString(),
                source.accountNumber(),
                payload.postedAt(),
                "TRANSFER_OUT",
                amount,
                destination.accountNumber(),
                fromRunning,
                payload.transferId(),
                eventId);
        StatementLine in = new StatementLine(
                UUID.randomUUID().toString(),
                destination.accountNumber(),
                payload.postedAt(),
                "TRANSFER_IN",
                amount,
                source.accountNumber(),
                toRunning,
                payload.transferId(),
                eventId);
        SendEmailPayload sent = new SendEmailPayload(
                "TRANSFER_SENT",
                source.ownerEmail(),
                source.ownerName(),
                source.accountNumber(),
                destination.accountNumber(),
                Money.format(amount),
                Money.CURRENCY,
                payload.postedAt());
        SendEmailPayload received = new SendEmailPayload(
                "TRANSFER_RECEIVED",
                destination.ownerEmail(),
                destination.ownerName(),
                destination.accountNumber(),
                source.accountNumber(),
                Money.format(amount),
                Money.CURRENCY,
                payload.postedAt());
        return List.of(
                new Projection(source.withCash(fromRunning, payload.postedAt()), List.of(out), List.of(sent)),
                new Projection(destination.withCash(toRunning, payload.postedAt()), List.of(in), List.of(received)));
    }
}
