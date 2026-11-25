package com.quickbank.ledger.app;

import com.quickbank.common.error.IdempotencyConflictException;
import com.quickbank.common.error.InsufficientFundsException;
import com.quickbank.common.error.SelfTransferException;
import com.quickbank.common.error.UnknownAccountException;
import com.quickbank.common.event.EventFactory;
import com.quickbank.common.event.EventTypes;
import com.quickbank.common.event.JsonMappers;
import com.quickbank.common.event.payload.PaymentCompletedPayload;
import com.quickbank.common.money.Money;
import com.quickbank.common.security.ClientUser;
import com.quickbank.ledger.domain.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerWorkflowTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T15:00:00Z"), ZoneOffset.UTC);
    private static final ClientUser ANA = new ClientUser("sub-ana", "ana@quickbank.local", "ana");
    private static final ClientUser BRUNO = new ClientUser("sub-bruno", "bruno@quickbank.local", "bruno");

    private InMemoryLedgerStore store;
    private LedgerWorkflow workflow;

    @BeforeEach
    void setUp() {
        store = new InMemoryLedgerStore();
        store.provision(new CustomerBook("10000001", ANA.subject(), ANA.email(), ANA.name(), new BigDecimal("100.00"), "CUSTOMER"));
        store.provision(new CustomerBook("10000002", BRUNO.subject(), BRUNO.email(), BRUNO.name(), new BigDecimal("5.00"), "CUSTOMER"));
        workflow = new LedgerWorkflow(store, new EventFactory(JsonMappers.events(), CLOCK), CLOCK);
    }

    @Test
    void transferMovesCashOnceAndReplaysTheSameReceipt() {
        TransferReceipt first = workflow.transfer(ANA, "transfer-key-1", new BigDecimal("40"), "10000002");
        TransferReceipt replay = workflow.transfer(ANA, "transfer-key-1", new BigDecimal("40.00"), "10000002");

        assertEquals(first.transferId(), replay.transferId());
        assertEquals("60.00", replay.fromBalanceAfter());
        assertEquals("45.00", replay.toBalanceAfter());
        assertEquals(new BigDecimal("60.00"), store.balance("10000001"));
        assertEquals(new BigDecimal("45.00"), store.balance("10000002"));
        assertEquals(1, store.journals().size());
        assertTrue(store.journals().get(0).balanced());
        assertEquals(Direction.DEBIT, store.journals().get(0).postings().get(0).direction());
        assertEquals(1, store.events().stream().filter(event -> EventTypes.TRANSFER_POSTED.equals(event.eventType())).count());
    }

    @Test
    void rejectsInsufficientFundsWithoutPosting() {
        assertThrows(InsufficientFundsException.class,
                () -> workflow.transfer(ANA, "transfer-key-2", new BigDecimal("100.01"), "10000002"));
        assertEquals(new BigDecimal("100.00"), store.balance("10000001"));
        assertEquals(new BigDecimal("5.00"), store.balance("10000002"));
        assertTrue(store.journals().isEmpty());
    }

    @Test
    void rejectsTransferToSelf() {
        assertThrows(SelfTransferException.class,
                () -> workflow.transfer(ANA, "transfer-key-3", new BigDecimal("1.00"), "10000001"));
        assertEquals(new BigDecimal("100.00"), store.balance("10000001"));
    }

    @Test
    void rejectsUnknownDestination() {
        assertThrows(UnknownAccountException.class,
                () -> workflow.transfer(ANA, "transfer-key-4", new BigDecimal("1.00"), "19999999"));
    }

    @Test
    void rejectsIdempotencyKeyReuseWithADifferentAmount() {
        workflow.transfer(ANA, "transfer-key-5", new BigDecimal("10.00"), "10000002");
        assertThrows(IdempotencyConflictException.class,
                () -> workflow.transfer(ANA, "transfer-key-5", new BigDecimal("11.00"), "10000002"));
        assertEquals(new BigDecimal("90.00"), store.balance("10000001"));
    }

    @Test
    void depositPostsOnceWhenTheMockPaymentResultIsReplayed() {
        DepositReceipt pending = workflow.acceptDeposit(ANA, "deposit-key-1", new BigDecimal("25.50"));
        assertEquals("PENDING", pending.status());
        assertEquals(new BigDecimal("100.00"), store.balance("10000001"));

        var result = new PaymentCompletedPayload(pending.depositId(), "SUCCEEDED", "10000001", "25.50", Money.CURRENCY, "deposit-key-1");
        workflow.postPayment(result);
        workflow.postPayment(result);

        assertEquals(new BigDecimal("125.50"), store.balance("10000001"));
        assertEquals(new BigDecimal("25.50"), store.balance("VAULT"));
        assertEquals("POSTED", workflow.findDeposit(pending.depositId(), ANA.subject()).orElseThrow().status());
        assertEquals(1, store.journals().size());
        assertTrue(store.journals().get(0).balanced());
        assertEquals(1, store.events().stream().filter(event -> EventTypes.DEPOSIT_POSTED.equals(event.eventType())).count());
    }

    @Test
    void depositReplayReturnsTheOriginalIntent() {
        DepositReceipt first = workflow.acceptDeposit(BRUNO, "deposit-key-2", new BigDecimal("8"));
        DepositReceipt replay = workflow.acceptDeposit(BRUNO, "deposit-key-2", new BigDecimal("8.00"));
        assertEquals(first.depositId(), replay.depositId());
        assertEquals("PENDING", replay.status());
        assertThrows(IdempotencyConflictException.class,
                () -> workflow.acceptDeposit(BRUNO, "deposit-key-2", new BigDecimal("9.00")));
    }
}
