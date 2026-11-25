# Dynatrace queries for QuickBank

These are the copy-paste queries for the spans this process emits. The same text is rendered in the in-app lab at `/lab` and `GET /api/observability/lab`.

Attribute names are the ones set in code:

| Attribute | Cardinality | Meaning |
| --- | --- | --- |
| `module` | low | `account`, `ledger`, `payment`, or `notification` |
| `mock` | low | `true` on the payment and email mocks, `false` on real ledger and account work |
| `movement.type` | low | `OPEN`, `DEPOSIT`, or `TRANSFER` |
| `account.number` | high | 8-digit customer account. On a transfer span this is the source account. |
| `transfer.id` | high | Ledger transfer id |
| `error.type` | high | Problem slug when a business rule rejects the call, for example `insufficient-funds` |

Dotted names are wrapped in backticks because that is how DQL addresses OTLP attributes ingested into Grail. Span duration in DQL is nanoseconds; the payment query divides by 1e6 to show milliseconds. The mock sleep defaults to 750ms, so the payment average should sit near that number plus a little scheduling time.

If a field name in your tenant differs by a prefix, filter on `span.name` first. The names below are stable.

Local Jaeger shows the same spans under service `quickbank`. Jaeger does not run DQL. Use the span name and the tags listed above. Dynatrace is optional: leave `DT_OTLP_ENDPOINT` unset and the app exports to the Compose collector instead.

## account-opened

Shows each attempt to open an account, including the account number that was allocated. A second open for the same user is a rejected span with `error.type = account-exists`.

Filters on `span.name = quickbank.account.open`, `module = account`, `movement.type = OPEN`, and `account.number`.

```
fetch spans, from: now() - 2h
| filter span.name == "quickbank.account.open"
| filter `module` == "account" and `movement.type` == "OPEN"
| fields timestamp, span.name, `account.number`, `error.type`, duration, span.status_code
| sort timestamp desc
```

Metric: filter `quickbank.account.open` where `movement.type = OPEN`.

## deposit

Shows the deposit path as separate spans: the ledger accepts the request, the mock payment runs, the ledger posts cash, and the account projection applies the event. `mock = true` is only the payment span.

Filters on `movement.type = DEPOSIT`, `account.number`, `module`, and `mock`. Span names: `quickbank.ledger.deposit.accept`, `quickbank.payment.mock`, `quickbank.ledger.deposit.post`, `quickbank.account.project`.

```
fetch spans, from: now() - 2h
| filter `movement.type` == "DEPOSIT"
| filter span.name == "quickbank.ledger.deposit.accept"
    or span.name == "quickbank.payment.mock"
    or span.name == "quickbank.ledger.deposit.post"
    or span.name == "quickbank.account.project"
| fields timestamp, span.name, `account.number`, `module`, `mock`, duration, span.status_code
| sort timestamp desc
```

Metric: `quickbank.ledger.deposit.post`, the timer around the cash posting.

## transfer

Shows one span per transfer attempt. A successful span carries `transfer.id`. Insufficient funds and a transfer to the same account set `error.type` and a failed span status. The cash movement itself is one MySQL transaction; this span wraps that transaction.

Filters on `span.name = quickbank.ledger.transfer`, `movement.type = TRANSFER`, `account.number`, `transfer.id`, and `error.type`.

```
fetch spans, from: now() - 2h
| filter span.name == "quickbank.ledger.transfer"
| filter `movement.type` == "TRANSFER"
| fields timestamp, `account.number`, `transfer.id`, `error.type`, duration, span.status_code
| sort timestamp desc
```

Metric: `quickbank.ledger.transfer`, optionally split by `error.type`.

## payment-mock-latency

Shows how long the mock payment port holds a command. The span wraps the simulated delay and the outbox write. No payment service provider is called. `mock = true` is the filter that keeps this query on the fake port.

Filters on `span.name = quickbank.payment.mock`, `mock = true`, and `module = payment`.

```
fetch spans, from: now() - 2h
| filter span.name == "quickbank.payment.mock" and `mock` == "true"
| summarize count = count(), avgMs = avg(duration) / 1000000, p95Ms = percentile(duration, 95) / 1000000
```

Metric selector: `quickbank.payment.mock:filter(eq(mock,true)):percentile(95)`.

## email-mock

Shows each logged email: account opened, deposit posted, transfer sent, transfer received. `movement.type` distinguishes them (`OPEN`, `DEPOSIT`, `TRANSFER`). Nothing is handed to SMTP.

Filters on `span.name = quickbank.notification.mock`, `mock = true`, `module = notification`, `movement.type`, and `account.number`.

```
fetch spans, from: now() - 2h
| filter span.name == "quickbank.notification.mock" and `mock` == "true"
| fields timestamp, `account.number`, `movement.type`, `module`, duration
| sort timestamp desc
```

Metric selector: `quickbank.notification.mock:filter(eq(mock,true)):splitBy(movement.type):count`.

## errors

Shows failed QuickBank spans and business rejections. `error.type` is the problem slug (`insufficient-funds`, `self-transfer`, `unknown-account`, `account-exists`, `account-not-ready`, `idempotency-conflict`). HTTP 422 responses are these rejections, not a crashed server.

Filters on `span.status_code = ERROR` or a present `error.type`, limited to span names that start with `quickbank.`. Also projects `account.number`, `transfer.id`, and `module`.

```
fetch spans, from: now() - 2h
| filter span.status_code == "ERROR" or isNotNull(`error.type`)
| filter startsWith(span.name, "quickbank.")
| fields timestamp, span.name, `error.type`, `account.number`, `transfer.id`, `module`, span.status_message
| sort timestamp desc
```

Metric: `quickbank.ledger.transfer:filter(eq(error.type,insufficient-funds)):count`. Drop the `error.type` clause to count every transfer outcome.

## account-number

Shows every QuickBank span that named one account. Replace `10000001` with the number from the UI. On transfers the attribute is the source account; the destination appears on `quickbank.account.project` as well when that side is applied, and the source span is the one carrying `transfer.id`.

Filters on `account.number`.

```
fetch spans, from: now() - 2h
| filter `account.number` == "10000001"
| fields timestamp, span.name, `movement.type`, `transfer.id`, `module`, `mock`, `error.type`, duration
| sort timestamp desc
```

`account.number` is high cardinality, so this span query is the one to use for a single customer. A metric split by account number will explode the dimension set; don't put this attribute on a dashboard tile that groups by it.

Span names referenced above: `quickbank.account.open`, `quickbank.ledger.deposit.accept`, `quickbank.payment.mock`, `quickbank.ledger.deposit.post`, `quickbank.ledger.transfer`, `quickbank.account.project`, `quickbank.notification.mock`.
