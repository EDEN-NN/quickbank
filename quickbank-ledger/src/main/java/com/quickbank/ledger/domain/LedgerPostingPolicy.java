package com.quickbank.ledger.domain;

import com.quickbank.common.error.InsufficientFundsException;
import com.quickbank.common.error.SelfTransferException;
import com.quickbank.common.money.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class LedgerPostingPolicy {

    public static final String VAULT = "VAULT";

    private LedgerPostingPolicy() {
    }

    public static JournalPlan deposit(
            String customerNumber,
            BigDecimal amount,
            BigDecimal customerBalance,
            BigDecimal vaultBalance
    ) {
        BigDecimal normalized = Money.amount(amount);
        BigDecimal customerAfter = customerBalance.add(normalized).setScale(2);
        BigDecimal vaultAfter = vaultBalance.add(normalized).setScale(2);
        return new JournalPlan(
                List.of(
                        new Posting(VAULT, Direction.DEBIT, normalized),
                        new Posting(customerNumber, Direction.CREDIT, normalized)),
                Map.of(VAULT, vaultAfter, customerNumber, customerAfter));
    }

    public static JournalPlan transfer(
            String from,
            String to,
            BigDecimal amount,
            BigDecimal fromBalance,
            BigDecimal toBalance
    ) {
        BigDecimal normalized = Money.amount(amount);
        if (from.equals(to)) {
            throw new SelfTransferException(from);
        }
        if (fromBalance.compareTo(normalized) < 0) {
            throw new InsufficientFundsException(from, fromBalance, normalized);
        }
        return new JournalPlan(
                List.of(
                        new Posting(from, Direction.DEBIT, normalized),
                        new Posting(to, Direction.CREDIT, normalized)),
                Map.of(
                        from, fromBalance.subtract(normalized).setScale(2),
                        to, toBalance.add(normalized).setScale(2)));
    }
}
