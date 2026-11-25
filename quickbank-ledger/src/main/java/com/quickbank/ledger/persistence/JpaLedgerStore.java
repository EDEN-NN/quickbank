package com.quickbank.ledger.persistence;

import com.quickbank.common.event.EventEnvelope;
import com.quickbank.common.outbox.OutboxTables;
import com.quickbank.common.outbox.OutboxWriter;
import com.quickbank.ledger.app.CustomerBook;
import com.quickbank.ledger.app.LedgerStore;
import com.quickbank.ledger.app.StoredDeposit;
import com.quickbank.ledger.app.StoredTransfer;
import com.quickbank.ledger.domain.JournalPlan;
import com.quickbank.ledger.domain.LedgerPostingPolicy;
import com.quickbank.ledger.domain.Posting;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Component
public class JpaLedgerStore implements LedgerStore {

    private final LedgerAccountRepository accounts;
    private final JournalRepository journals;
    private final PostingRepository postings;
    private final DepositRepository deposits;
    private final TransferRepository transfers;
    private final OutboxWriter outbox;
    private final Clock clock;

    public JpaLedgerStore(
            LedgerAccountRepository accounts,
            JournalRepository journals,
            PostingRepository postings,
            DepositRepository deposits,
            TransferRepository transfers,
            OutboxWriter outbox,
            Clock clock
    ) {
        this.accounts = accounts;
        this.journals = journals;
        this.postings = postings;
        this.deposits = deposits;
        this.transfers = transfers;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    public Optional<StoredDeposit> findDepositByKey(String idempotencyKey) {
        return deposits.findByIdempotencyKey(idempotencyKey).map(JpaLedgerStore::toDeposit);
    }

    @Override
    public Optional<StoredDeposit> findDepositById(String depositId) {
        return deposits.findById(depositId).map(JpaLedgerStore::toDeposit);
    }

    @Override
    public Optional<StoredTransfer> findTransferByKey(String idempotencyKey) {
        return transfers.findByIdempotencyKey(idempotencyKey).map(JpaLedgerStore::toTransfer);
    }

    @Override
    public Optional<CustomerBook> findByOwner(String ownerSubject) {
        return accounts.findByOwnerSubject(ownerSubject).map(JpaLedgerStore::toBook);
    }

    @Override
    public Optional<CustomerBook> findByNumber(String accountNumber) {
        return accounts.findById(accountNumber).map(JpaLedgerStore::toBook);
    }

    @Override
    public Optional<CustomerBook> lockCustomer(String accountNumber) {
        return accounts.lockByNumber(accountNumber).map(JpaLedgerStore::toBook);
    }

    @Override
    public CustomerBook lockVault() {
        return accounts.lockByNumber(LedgerPostingPolicy.VAULT)
                .map(JpaLedgerStore::toBook)
                .orElseThrow(() -> new IllegalStateException("Cash vault is missing. Flyway should have seeded VAULT."));
    }

    @Override
    public void insertCustomer(CustomerBook book) {
        accounts.save(new LedgerAccountEntity(
                book.accountNumber(),
                book.ownerSubject(),
                book.ownerEmail(),
                book.ownerName(),
                book.kind(),
                book.cashBalance(),
                clock.instant()));
    }

    @Override
    public void saveBalance(CustomerBook book) {
        LedgerAccountEntity entity = accounts.findById(book.accountNumber())
                .orElseThrow(() -> new IllegalStateException("Missing ledger account " + book.accountNumber()));
        entity.setCashBalance(book.cashBalance());
        accounts.save(entity);
    }

    @Override
    public void insertJournal(String journalId, String kind, String idempotencyKey, JournalPlan plan, Instant createdAt) {
        journals.save(new JournalEntity(journalId, kind, idempotencyKey, createdAt));
        for (Posting posting : plan.postings()) {
            postings.save(new PostingEntity(journalId, posting.accountNumber(), posting.direction().name(), posting.amount()));
        }
    }

    @Override
    public void insertDeposit(StoredDeposit deposit) {
        deposits.save(new DepositEntity(
                deposit.id(),
                deposit.idempotencyKey(),
                deposit.ownerSubject(),
                deposit.accountNumber(),
                deposit.amount(),
                deposit.status(),
                deposit.createdAt()));
    }

    @Override
    public Optional<StoredDeposit> lockDeposit(String depositId) {
        return deposits.lockById(depositId).map(JpaLedgerStore::toDeposit);
    }

    @Override
    public void markDepositPosted(String depositId, Instant postedAt) {
        DepositEntity entity = deposits.findById(depositId)
                .orElseThrow(() -> new IllegalStateException("Missing deposit " + depositId));
        entity.markPosted(postedAt);
        deposits.save(entity);
    }

    @Override
    public void insertTransfer(StoredTransfer transfer) {
        transfers.save(new TransferEntity(
                transfer.id(),
                transfer.idempotencyKey(),
                transfer.ownerSubject(),
                transfer.fromAccount(),
                transfer.toAccount(),
                transfer.amount(),
                transfer.fromBalanceAfter(),
                transfer.toBalanceAfter(),
                transfer.createdAt()));
    }

    @Override
    public void enqueue(String topic, String messageKey, EventEnvelope event) {
        outbox.append(OutboxTables.LEDGER, topic, messageKey, event);
    }

    private static CustomerBook toBook(LedgerAccountEntity entity) {
        return new CustomerBook(
                entity.getAccountNumber(),
                entity.getOwnerSubject(),
                entity.getOwnerEmail(),
                entity.getOwnerName(),
                entity.getCashBalance(),
                entity.getKind());
    }

    private static StoredDeposit toDeposit(DepositEntity entity) {
        return new StoredDeposit(
                entity.getId(),
                entity.getIdempotencyKey(),
                entity.getOwnerSubject(),
                entity.getAccountNumber(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getPostedAt());
    }

    private static StoredTransfer toTransfer(TransferEntity entity) {
        return new StoredTransfer(
                entity.getId(),
                entity.getIdempotencyKey(),
                entity.getOwnerSubject(),
                entity.getFromAccount(),
                entity.getToAccount(),
                entity.getAmount(),
                entity.getFromBalanceAfter(),
                entity.getToBalanceAfter(),
                entity.getCreatedAt());
    }
}
