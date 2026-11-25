package com.quickbank.ledger.domain;

import com.quickbank.common.error.InsufficientFundsException;
import com.quickbank.common.error.SelfTransferException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerPostingPolicyTest {

    @Test
    void depositDebitsVaultAndCreditsCustomer() {
        JournalPlan plan = LedgerPostingPolicy.deposit(
                "10000001",
                new BigDecimal("80.00"),
                new BigDecimal("20.00"),
                new BigDecimal("0.00"));

        assertTrue(plan.balanced());
        assertEquals(new BigDecimal("100.00"), plan.balancesAfter().get("10000001"));
        assertEquals(new BigDecimal("80.00"), plan.balancesAfter().get(LedgerPostingPolicy.VAULT));
        assertEquals(Direction.DEBIT, plan.postings().get(0).direction());
        assertEquals(Direction.CREDIT, plan.postings().get(1).direction());
    }

    @Test
    void transferDebitsSourceAndCreditsDestination() {
        JournalPlan plan = LedgerPostingPolicy.transfer(
                "10000001",
                "10000002",
                new BigDecimal("30.50"),
                new BigDecimal("40.00"),
                new BigDecimal("10.00"));

        assertTrue(plan.balanced());
        assertEquals(new BigDecimal("9.50"), plan.balancesAfter().get("10000001"));
        assertEquals(new BigDecimal("40.50"), plan.balancesAfter().get("10000002"));
    }

    @Test
    void rejectsInsufficientCash() {
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class, () -> LedgerPostingPolicy.transfer(
                "10000001",
                "10000002",
                new BigDecimal("25.00"),
                new BigDecimal("10.00"),
                new BigDecimal("0.00")));
        assertEquals("10000001", ex.accountNumber());
        assertEquals(new BigDecimal("10.00"), ex.available());
        assertEquals(new BigDecimal("25.00"), ex.requested());
    }

    @Test
    void rejectsTransferToSelf() {
        assertThrows(SelfTransferException.class, () -> LedgerPostingPolicy.transfer(
                "10000001",
                "10000001",
                new BigDecimal("1.00"),
                new BigDecimal("5.00"),
                new BigDecimal("5.00")));
    }
}
