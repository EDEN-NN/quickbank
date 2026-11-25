package com.quickbank.observability.lab;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DynatraceCatalog {

    public List<SpanSpec> spans() {
        return List.of(
                new SpanSpec("quickbank.account.open", "account", "A signed-in client opens an account.",
                        "module=account, mock=false, movement.type=OPEN, account.number, error.type"),
                new SpanSpec("quickbank.ledger.deposit.accept", "ledger", "A deposit is accepted and a payment command is written to the outbox.",
                        "module=ledger, mock=false, movement.type=DEPOSIT, account.number, error.type"),
                new SpanSpec("quickbank.payment.mock", "payment", "The mock payment port waits, then always approves the command.",
                        "module=payment, mock=true, movement.type=DEPOSIT, account.number"),
                new SpanSpec("quickbank.ledger.deposit.post", "ledger", "The ledger posts the cash credit after the mock payment succeeds.",
                        "module=ledger, mock=false, movement.type=DEPOSIT, account.number"),
                new SpanSpec("quickbank.ledger.transfer", "ledger", "The ledger debits the source and credits the destination in one transaction.",
                        "module=ledger, mock=false, movement.type=TRANSFER, account.number, transfer.id, error.type"),
                new SpanSpec("quickbank.account.project", "account", "The account read model applies a deposit or transfer event.",
                        "module=account, mock=false, movement.type, account.number, transfer.id"),
                new SpanSpec("quickbank.notification.mock", "notification", "The mock mailer logs the email it would have sent.",
                        "module=notification, mock=true, movement.type, account.number"));
    }

    public List<QuerySpec> queries() {
        return List.of(
                query("account-opened", "Account opened",
                        "Spans for a successful or rejected account opening, including the new account number.",
                        "span.name, module, movement.type, account.number",
                        """
                        fetch spans, from: now() - 2h
                        | filter span.name == "quickbank.account.open"
                        | filter `module` == "account" and `movement.type` == "OPEN"
                        | fields timestamp, span.name, `account.number`, `error.type`, duration, span.status_code
                        | sort timestamp desc
                        """,
                        "Filter metric quickbank.account.open where movement.type = OPEN."),
                query("deposit", "Deposit",
                        "The deposit path: the ledger accepts the request, the mock payment runs, then the ledger posts cash.",
                        "span.name, movement.type=DEPOSIT, account.number, mock",
                        """
                        fetch spans, from: now() - 2h
                        | filter `movement.type` == "DEPOSIT"
                        | filter span.name == "quickbank.ledger.deposit.accept"
                            or span.name == "quickbank.payment.mock"
                            or span.name == "quickbank.ledger.deposit.post"
                            or span.name == "quickbank.account.project"
                        | fields timestamp, span.name, `account.number`, `module`, `mock`, duration, span.status_code
                        | sort timestamp desc
                        """,
                        "timeseries avg(quickbank.ledger.deposit.post) — timer created from the post observation."),
                query("transfer", "Transfer",
                        "One span per transfer attempt. Insufficient funds and self-transfer set error.type and a failed span status.",
                        "span.name, movement.type=TRANSFER, account.number, transfer.id, error.type",
                        """
                        fetch spans, from: now() - 2h
                        | filter span.name == "quickbank.ledger.transfer"
                        | filter `movement.type` == "TRANSFER"
                        | fields timestamp, `account.number`, `transfer.id`, `error.type`, duration, span.status_code
                        | sort timestamp desc
                        """,
                        "Filter metric quickbank.ledger.transfer by movement.type = TRANSFER and split by error.type when present."),
                query("payment-mock-latency", "Payment mock latency",
                        "How long the mock payment port holds the command. The span includes the configured sleep, so this is the simulated PSP latency, not a real provider.",
                        "span.name=quickbank.payment.mock, mock=true, module=payment",
                        """
                        fetch spans, from: now() - 2h
                        | filter span.name == "quickbank.payment.mock" and `mock` == "true"
                        | summarize count = count(), avgMs = avg(duration) / 1000000, p95Ms = percentile(duration, 95) / 1000000
                        """,
                        "builtin-style selector: quickbank.payment.mock:filter(eq(mock,true)):percentile(95). Duration is the Micrometer timer around the mock."),
                query("email-mock", "Email mock",
                        "Each logged email: account opened, deposit posted, transfer sent, transfer received. mock=true means nothing was handed to SMTP.",
                        "span.name=quickbank.notification.mock, mock=true, module=notification, movement.type, account.number",
                        """
                        fetch spans, from: now() - 2h
                        | filter span.name == "quickbank.notification.mock" and `mock` == "true"
                        | fields timestamp, `account.number`, `movement.type`, `module`, duration
                        | sort timestamp desc
                        """,
                        "quickbank.notification.mock:filter(eq(mock,true)):splitBy(movement.type):count"),
                query("errors", "Errors",
                        "Failed spans plus business rejections. error.type is the problem slug, for example insufficient-funds or self-transfer.",
                        "span.status_code, error.type, module, account.number",
                        """
                        fetch spans, from: now() - 2h
                        | filter span.status_code == "ERROR" or isNotNull(`error.type`)
                        | filter startsWith(span.name, "quickbank.")
                        | fields timestamp, span.name, `error.type`, `account.number`, `transfer.id`, `module`, span.status_message
                        | sort timestamp desc
                        """,
                        "quickbank.ledger.transfer:filter(eq(error.type,insufficient-funds)):count — drop the error.type filter to see every transfer outcome."),
                query("account-number", "Filter by account number",
                        "Every QuickBank span that named this account. Paste the 8-digit number from the UI.",
                        "account.number",
                        """
                        fetch spans, from: now() - 2h
                        | filter `account.number` == "10000001"
                        | fields timestamp, span.name, `movement.type`, `transfer.id`, `module`, `mock`, `error.type`, duration
                        | sort timestamp desc
                        """,
                        "Any quickbank.* timer or counter filtered with eq(account.number,10000001). account.number is high cardinality, so prefer the span query for a single account."));
    }

    private static QuerySpec query(String id, String title, String shows, String filtersOn, String dql, String metric) {
        return new QuerySpec(id, title, shows, filtersOn, dql.strip(), metric);
    }
}
