package com.quickbank.account.app;

import com.quickbank.common.event.payload.DepositPostedPayload;
import com.quickbank.common.event.payload.TransferPostedPayload;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementProjectorTest {

    private static final Instant MORNING = Instant.parse("2026-11-24T12:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-24T15:30:00Z");

    @Test
    void depositKeepsCreditUntouchedAndSetsRunningCash() {
        SnapshotView opened = new SnapshotView(
                "10000001", "ana", "ana@quickbank.local",
                new BigDecimal("0.00"), new BigDecimal("2000.00"), new BigDecimal("0.00"), MORNING);

        Projection projection = StatementProjector.deposit(
                opened,
                new DepositPostedPayload("dep-1", "10000001", "150.00", "BRL", "150.00", LATER),
                "event-1");

        assertEquals(new BigDecimal("150.00"), projection.snapshot().cash());
        assertEquals(new BigDecimal("2000.00"), projection.snapshot().creditLimit());
        assertEquals(new BigDecimal("0.00"), projection.snapshot().creditUsed());
        assertEquals(new BigDecimal("2000.00"), projection.snapshot().creditAvailable());
        assertEquals("DEPOSIT", projection.lines().get(0).type());
        assertEquals("External deposit", projection.lines().get(0).counterparty());
        assertEquals(new BigDecimal("150.00"), projection.lines().get(0).runningBalance());
        assertEquals("DEPOSIT_POSTED", projection.emails().get(0).template());
        assertEquals("ana@quickbank.local", projection.emails().get(0).recipient());
    }

    @Test
    void transferBuildsBothLegsAndBothEmails() {
        SnapshotView ana = new SnapshotView(
                "10000001", "ana", "ana@quickbank.local",
                new BigDecimal("150.00"), new BigDecimal("2000.00"), new BigDecimal("0.00"), MORNING);
        SnapshotView bruno = new SnapshotView(
                "10000002", "bruno", "bruno@quickbank.local",
                new BigDecimal("0.00"), new BigDecimal("2000.00"), new BigDecimal("0.00"), MORNING);

        List<Projection> projections = StatementProjector.transfer(
                ana,
                bruno,
                new TransferPostedPayload("tx-1", "10000001", "10000002", "40.00", "BRL", "110.00", "40.00", LATER),
                "event-2");

        assertEquals("TRANSFER_OUT", projections.get(0).lines().get(0).type());
        assertEquals("10000002", projections.get(0).lines().get(0).counterparty());
        assertEquals(new BigDecimal("110.00"), projections.get(0).lines().get(0).runningBalance());
        assertEquals(new BigDecimal("0.00"), projections.get(0).snapshot().creditUsed());
        assertEquals("TRANSFER_SENT", projections.get(0).emails().get(0).template());

        assertEquals("TRANSFER_IN", projections.get(1).lines().get(0).type());
        assertEquals("10000001", projections.get(1).lines().get(0).counterparty());
        assertEquals(new BigDecimal("40.00"), projections.get(1).snapshot().cash());
        assertEquals("TRANSFER_RECEIVED", projections.get(1).emails().get(0).template());
        assertEquals("tx-1", projections.get(0).lines().get(0).transferId());
    }

    @Test
    void statementSortsByTimeThenId() {
        var later = line("b", LATER);
        var earlierSameIdOrder = line("m", MORNING);
        var earlierFirst = line("a", MORNING);
        var shuffled = new ArrayList<>(List.of(later, earlierSameIdOrder, earlierFirst));
        shuffled.sort(StatementLine.CHRONOLOGICAL);
        assertEquals(List.of("a", "m", "b"), shuffled.stream().map(StatementLine::id).toList());
    }

    private static StatementLine line(String id, Instant when) {
        return new StatementLine(id, "10000001", when, "DEPOSIT", new BigDecimal("1.00"), "External deposit",
                new BigDecimal("1.00"), null, "evt-" + id);
    }
}
