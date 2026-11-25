package com.quickbank.ledger.domain;

import java.math.BigDecimal;

public record Posting(String accountNumber, Direction direction, BigDecimal amount) {
}
