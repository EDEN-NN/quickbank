package com.quickbank.account.api;

import com.quickbank.account.app.AccountApplicationService;
import com.quickbank.account.app.AccountApplicationService.AccountView;
import com.quickbank.account.app.StatementLine;
import com.quickbank.common.money.Money;
import com.quickbank.common.security.ClientUsers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountApiController {

    private final AccountApplicationService accounts;

    public AccountApiController(AccountApplicationService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open() {
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(accounts.open(ClientUsers.require())));
    }

    @GetMapping("/me")
    public AccountResponse me() {
        return AccountResponse.from(accounts.requireMine(ClientUsers.require()));
    }

    @GetMapping("/me/statement")
    public List<StatementResponse> statement() {
        return accounts.statement(ClientUsers.require()).stream().map(StatementResponse::from).toList();
    }

    public record AccountResponse(
            String accountNumber,
            String ownerName,
            String cashAvailable,
            String creditLimit,
            String creditUsed,
            String creditAvailable,
            String currency
    ) {
        static AccountResponse from(AccountView view) {
            return new AccountResponse(
                    view.accountNumber(),
                    view.ownerName(),
                    Money.format(view.cashAvailable()),
                    Money.format(view.creditLimit()),
                    Money.format(view.creditUsed()),
                    Money.format(view.creditAvailable()),
                    Money.CURRENCY);
        }
    }

    public record StatementResponse(
            String id,
            String occurredAt,
            String type,
            String amount,
            String counterparty,
            String runningBalance,
            String transferId
    ) {
        static StatementResponse from(StatementLine line) {
            return new StatementResponse(
                    line.id(),
                    line.occurredAt().toString(),
                    line.type(),
                    Money.format(line.amount()),
                    line.counterparty(),
                    Money.format(line.runningBalance()),
                    line.transferId());
        }
    }
}
