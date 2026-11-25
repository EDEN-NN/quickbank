package com.quickbank.account.app;

import com.quickbank.common.error.AccountAlreadyExistsException;
import com.quickbank.common.error.NotFoundException;
import com.quickbank.common.event.EventEnvelope;
import com.quickbank.common.event.EventFactory;
import com.quickbank.common.event.EventTypes;
import com.quickbank.common.event.Topics;
import com.quickbank.common.event.payload.AccountOpenedPayload;
import com.quickbank.common.event.payload.DepositPostedPayload;
import com.quickbank.common.event.payload.SendEmailPayload;
import com.quickbank.common.event.payload.TransferPostedPayload;
import com.quickbank.common.money.AccountNumbers;
import com.quickbank.common.money.Money;
import com.quickbank.common.outbox.OutboxTables;
import com.quickbank.common.outbox.OutboxWriter;
import com.quickbank.common.security.ClientUser;
import com.quickbank.common.trace.QuickBankSpans;
import com.quickbank.account.persistence.AccountEntity;
import com.quickbank.account.persistence.AccountRepository;
import com.quickbank.account.persistence.SnapshotEntity;
import com.quickbank.account.persistence.SnapshotRepository;
import com.quickbank.account.persistence.StatementEntryEntity;
import com.quickbank.account.persistence.StatementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AccountApplicationService.class);

    private final AccountRepository accounts;
    private final SnapshotRepository snapshots;
    private final StatementRepository statements;
    private final JdbcTemplate jdbc;
    private final OutboxWriter outbox;
    private final EventFactory events;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final QuickBankSpans spans;
    private final TransactionTemplate transaction;
    private final BigDecimal creditLimit;

    public AccountApplicationService(
            AccountRepository accounts,
            SnapshotRepository snapshots,
            StatementRepository statements,
            JdbcTemplate jdbc,
            OutboxWriter outbox,
            EventFactory events,
            ObjectMapper mapper,
            Clock clock,
            QuickBankSpans spans,
            PlatformTransactionManager transactions,
            @Value("${quickbank.account.credit-limit:2000.00}") String creditLimit
    ) {
        this.accounts = accounts;
        this.snapshots = snapshots;
        this.statements = statements;
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
        this.spans = spans;
        this.transaction = new TransactionTemplate(transactions);
        this.creditLimit = new BigDecimal(creditLimit).setScale(2, RoundingMode.UNNECESSARY);
    }

    public AccountView open(ClientUser user) {
        try {
            return transaction.execute(status -> spans.run(
                    "quickbank.account.open",
                    Map.of("module", "account", "mock", "false", "movement.type", "OPEN"),
                    Map.of(),
                    bag -> {
                        accounts.findByOwnerSubject(user.subject()).ifPresent(existing -> {
                            throw new AccountAlreadyExistsException(existing.getAccountNumber());
                        });
                        String number = nextAccountNumber();
                        bag.attribute("account.number", number);
                        Instant now = clock.instant();
                        accounts.save(new AccountEntity(number, user.subject(), user.email(), user.name(), creditLimit, creditLimit, now));
                        snapshots.save(new SnapshotEntity(number, Money.ZERO, creditLimit, Money.ZERO, now));
                        var payload = new AccountOpenedPayload(
                                number,
                                user.subject(),
                                user.email(),
                                user.name(),
                                Money.format(creditLimit),
                                Money.CURRENCY,
                                now);
                        EventEnvelope opened = events.create(EventTypes.ACCOUNT_OPENED, number, payload);
                        outbox.append(OutboxTables.ACCOUNT, Topics.ACCOUNT_EVENTS, number, opened);
                        outbox.append(OutboxTables.ACCOUNT, Topics.NOTIFICATION_COMMANDS, number, events.create(
                                EventTypes.SEND_EMAIL,
                                opened.eventId().toString(),
                                new SendEmailPayload(
                                        "ACCOUNT_OPENED",
                                        user.email(),
                                        user.name(),
                                        number,
                                        null,
                                        null,
                                        Money.CURRENCY,
                                        now)));
                        return view(number, user.name(), user.email(), Money.ZERO, creditLimit, Money.ZERO);
                    }));
        } catch (DataIntegrityViolationException ex) {
            String number = accounts.findByOwnerSubject(user.subject())
                    .map(AccountEntity::getAccountNumber)
                    .orElse("unknown");
            throw new AccountAlreadyExistsException(number);
        }
    }

    public Optional<AccountView> findMine(ClientUser user) {
        TransactionTemplate read = readOnly();
        return read.execute(status -> accounts.findByOwnerSubject(user.subject()).map(account -> {
            SnapshotEntity snapshot = snapshots.findById(account.getAccountNumber()).orElseThrow();
            return view(
                    account.getAccountNumber(),
                    account.getOwnerName(),
                    account.getOwnerEmail(),
                    snapshot.getCashBalance(),
                    snapshot.getCreditLimit(),
                    snapshot.getCreditUsed());
        }));
    }

    public AccountView requireMine(ClientUser user) {
        return findMine(user).orElseThrow(() -> new NotFoundException("Open an account first."));
    }

    public CreditView credit(ClientUser user) {
        TransactionTemplate read = readOnly();
        return read.execute(status -> creditView(requireAccount(user)));
    }

    public CreditView changeCreditLimit(ClientUser user, String rawLimit) {
        return transaction.execute(status -> spans.run(
                "quickbank.account.credit-limit",
                Map.of("module", "account", "operation", "credit-limit"),
                Map.of(),
                bag -> {
                    AccountEntity account = requireAccount(user);
                    SnapshotEntity snapshot = snapshots.findById(account.getAccountNumber()).orElseThrow();
                    BigDecimal requested = CreditLimits.parse(rawLimit);
                    CreditLimits.ensureWithin(requested, account.getCreditLimitMax(), snapshot.getCreditUsed());
                    Instant now = clock.instant();
                    account.setCreditLimit(requested);
                    snapshot.setCreditLimit(requested);
                    snapshot.setUpdatedAt(now);
                    accounts.save(account);
                    snapshots.save(snapshot);
                    bag.attribute("account.number", account.getAccountNumber());
                    bag.attribute("credit.result", "accepted");
                    log.info("credit limit updated account={}", account.getAccountNumber());
                    return creditView(account);
                }));
    }

    public void updateOwnerContact(String subject, String name, String email) {
        transaction.executeWithoutResult(status -> accounts.findByOwnerSubject(subject).ifPresent(account -> {
            String stored = name.length() > 120 ? name.substring(0, 120) : name;
            account.renameOwner(stored, email);
            accounts.save(account);
        }));
    }

    private AccountEntity requireAccount(ClientUser user) {
        return accounts.findByOwnerSubject(user.subject())
                .orElseThrow(() -> new NotFoundException("Open an account first."));
    }

    private CreditView creditView(AccountEntity account) {
        SnapshotEntity snapshot = snapshots.findById(account.getAccountNumber()).orElseThrow();
        return new CreditView(
                account.getAccountNumber(),
                snapshot.getCreditLimit(),
                account.getCreditLimitMax(),
                snapshot.getCreditUsed());
    }

    public List<StatementLine> statement(ClientUser user) {
        TransactionTemplate read = readOnly();
        return read.execute(status -> {
            AccountEntity account = accounts.findByOwnerSubject(user.subject())
                    .orElseThrow(() -> new NotFoundException("Open an account first."));
            return statements.findByAccountNumberOrderByOccurredAtAscIdAsc(account.getAccountNumber()).stream()
                    .map(entry -> new StatementLine(
                            entry.getId(),
                            entry.getAccountNumber(),
                            entry.getOccurredAt(),
                            entry.getEntryType(),
                            entry.getAmount(),
                            entry.getCounterparty(),
                            entry.getRunningBalance(),
                            entry.getTransferId(),
                            entry.getSourceEventId()))
                    .toList();
        });
    }

    public void apply(EventEnvelope event) {
        transaction.executeWithoutResult(status -> spans.run(
                "quickbank.account.project",
                Map.of("module", "account", "mock", "false", "movement.type", movement(event.eventType())),
                Map.of(),
                bag -> {
                    if (EventTypes.DEPOSIT_POSTED.equals(event.eventType())) {
                        applyDeposit(event, bag);
                    } else if (EventTypes.TRANSFER_POSTED.equals(event.eventType())) {
                        applyTransfer(event, bag);
                    }
                    return null;
                }));
    }

    private void applyDeposit(EventEnvelope event, QuickBankSpans.SpanBag bag) {
        DepositPostedPayload payload = tree(event, DepositPostedPayload.class);
        bag.attribute("account.number", payload.accountNumber());
        if (statements.existsByAccountNumberAndSourceEventIdAndEntryType(
                payload.accountNumber(), event.eventId().toString(), "DEPOSIT")) {
            return;
        }
        SnapshotView current = load(payload.accountNumber());
        Projection projection = StatementProjector.deposit(current, payload, event.eventId().toString());
        store(projection, event.eventId().toString());
    }

    private void applyTransfer(EventEnvelope event, QuickBankSpans.SpanBag bag) {
        TransferPostedPayload payload = tree(event, TransferPostedPayload.class);
        bag.attribute("account.number", payload.fromAccount());
        bag.attribute("transfer.id", payload.transferId());
        boolean outExists = statements.existsByAccountNumberAndSourceEventIdAndEntryType(
                payload.fromAccount(), event.eventId().toString(), "TRANSFER_OUT");
        boolean inExists = statements.existsByAccountNumberAndSourceEventIdAndEntryType(
                payload.toAccount(), event.eventId().toString(), "TRANSFER_IN");
        if (outExists && inExists) {
            return;
        }
        List<Projection> projections = StatementProjector.transfer(
                load(payload.fromAccount()),
                load(payload.toAccount()),
                payload,
                event.eventId().toString());
        for (Projection projection : projections) {
            store(projection, event.eventId().toString());
        }
    }

    private void store(Projection projection, String sourceEventId) {
        SnapshotEntity entity = snapshots.findById(projection.snapshot().accountNumber()).orElseThrow();
        entity.setCashBalance(projection.snapshot().cash());
        entity.setUpdatedAt(projection.snapshot().updatedAt());
        snapshots.save(entity);
        for (StatementLine line : projection.lines()) {
            statements.save(new StatementEntryEntity(
                    line.id(),
                    line.accountNumber(),
                    line.occurredAt(),
                    line.type(),
                    line.amount(),
                    line.counterparty(),
                    line.runningBalance(),
                    line.transferId(),
                    line.sourceEventId()));
        }
        for (SendEmailPayload email : projection.emails()) {
            outbox.append(
                    OutboxTables.ACCOUNT,
                    Topics.NOTIFICATION_COMMANDS,
                    email.accountNumber(),
                    events.create(EventTypes.SEND_EMAIL, sourceEventId + ":" + email.template(), email));
        }
    }

    private SnapshotView load(String accountNumber) {
        AccountEntity account = accounts.findById(accountNumber)
                .orElseThrow(() -> new NotFoundException("Account " + accountNumber + " is not open yet."));
        SnapshotEntity snapshot = snapshots.findById(accountNumber).orElseThrow();
        return new SnapshotView(
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getOwnerEmail(),
                snapshot.getCashBalance(),
                snapshot.getCreditLimit(),
                snapshot.getCreditUsed(),
                snapshot.getUpdatedAt());
    }

    private String nextAccountNumber() {
        Long next = jdbc.queryForObject("SELECT next_number FROM acc_number_seq WHERE id = 1 FOR UPDATE", Long.class);
        if (next == null) {
            throw new IllegalStateException("Account number sequence is missing.");
        }
        jdbc.update("UPDATE acc_number_seq SET next_number = ? WHERE id = 1", next + 1);
        return AccountNumbers.fromSequence(next);
    }

    private <T> T tree(EventEnvelope event, Class<T> type) {
        return mapper.convertValue(event.payload(), type);
    }

    private TransactionTemplate readOnly() {
        TransactionTemplate read = new TransactionTemplate(transaction.getTransactionManager());
        read.setReadOnly(true);
        return read;
    }

    private static String movement(String eventType) {
        if (EventTypes.DEPOSIT_POSTED.equals(eventType)) {
            return "DEPOSIT";
        }
        if (EventTypes.TRANSFER_POSTED.equals(eventType)) {
            return "TRANSFER";
        }
        return "UNKNOWN";
    }

    private static AccountView view(
            String number,
            String name,
            String email,
            BigDecimal cash,
            BigDecimal limit,
            BigDecimal used
    ) {
        return new AccountView(number, name, email, cash, limit, used);
    }

    public record AccountView(
            String accountNumber,
            String ownerName,
            String ownerEmail,
            BigDecimal cashAvailable,
            BigDecimal creditLimit,
            BigDecimal creditUsed
    ) {
        public BigDecimal creditAvailable() {
            return creditLimit.subtract(creditUsed).setScale(2, RoundingMode.UNNECESSARY);
        }
    }

    public record CreditView(
            String accountNumber,
            BigDecimal creditLimit,
            BigDecimal creditLimitMax,
            BigDecimal creditUsed
    ) {
        public BigDecimal creditAvailable() {
            return creditLimit.subtract(creditUsed).setScale(2, RoundingMode.UNNECESSARY);
        }
    }
}
