package com.quickbank.common.error;

import com.quickbank.common.money.Money;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public final class Problems {

    private Problems() {
    }

    public static ProblemDetail of(QuickBankException ex, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(ex.status()), ex.getMessage());
        problem.setType(URI.create(ex.type()));
        problem.setTitle(ex.title());
        if (instance != null && !instance.isBlank()) {
            problem.setInstance(URI.create(instance));
        }
        if (ex instanceof InsufficientFundsException funds) {
            problem.setProperty("accountNumber", funds.accountNumber());
            problem.setProperty("available", Money.format(funds.available()));
            problem.setProperty("requested", Money.format(funds.requested()));
            problem.setProperty("currency", Money.CURRENCY);
        }
        if (ex instanceof UnknownAccountException unknown) {
            problem.setProperty("accountNumber", unknown.accountNumber());
        }
        return problem;
    }

    public static ProblemDetail validation(String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://quickbank.dev/problems/validation"));
        problem.setTitle("Request is not valid");
        if (instance != null && !instance.isBlank()) {
            problem.setInstance(URI.create(instance));
        }
        return problem;
    }
}
