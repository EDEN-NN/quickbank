package com.quickbank.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemsTest {

    @Test
    void insufficientFundsIsProblemJsonWithMoneyStrings() {
        var ex = new InsufficientFundsException("10000001", new BigDecimal("10.00"), new BigDecimal("25.00"));
        ProblemDetail problem = Problems.of(ex, "/api/transfers");
        assertEquals(422, problem.getStatus());
        assertEquals("Insufficient funds", problem.getTitle());
        assertEquals("https://quickbank.dev/problems/insufficient-funds", problem.getType().toString());
        assertEquals("10.00", problem.getProperties().get("available"));
        assertEquals("25.00", problem.getProperties().get("requested"));
        assertEquals("BRL", problem.getProperties().get("currency"));
    }
}
