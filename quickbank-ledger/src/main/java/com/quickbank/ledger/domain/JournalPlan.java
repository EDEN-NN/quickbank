package com.quickbank.ledger.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record JournalPlan(List<Posting> postings, Map<String, BigDecimal> balancesAfter) {

    public boolean balanced() {
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (Posting posting : postings) {
            if (posting.direction() == Direction.DEBIT) {
                debits = debits.add(posting.amount());
            } else {
                credits = credits.add(posting.amount());
            }
        }
        return debits.compareTo(credits) == 0 && debits.signum() > 0;
    }
}
