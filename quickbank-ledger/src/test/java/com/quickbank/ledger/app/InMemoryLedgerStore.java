package com.quickbank.ledger.app;

import com.quickbank.common.event.EventEnvelope;
import com.quickbank.ledger.domain.JournalPlan;
import com.quickbank.ledger.domain.LedgerPostingPolicy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryLedgerStore implements LedgerStore {

    private final Map<String, CustomerBook> byNumber = new LinkedHashMap<>();
    private final Map<String, StoredDeposit> depositsById = new LinkedHashMap<>();
    private final Map<String, StoredDeposit> depositsByKey = new LinkedHashMap<>();
    private final Map<String, StoredTransfer> transfersByKey = new LinkedHashMap<>();
    private final List<JournalPlan> journals = new ArrayList<>();
    private final List<EventEnvelope> events = new ArrayList<>();

    public InMemoryLedgerStore() {
        byNumber.put(LedgerPostingPolicy.VAULT, new CustomerBook(
                LedgerPostingPolicy.VAULT, null, null, "Cash vault", new BigDecimal("0.00"), "VAULT"));
    }

    public void provision(CustomerBook book) {
        byNumber.put(book.accountNumber(), book);
    }

    public BigDecimal balance(String accountNumber) {
        return byNumber.get(accountNumber).cashBalance();
    }

    public List<JournalPlan> journals() {
        return List.copyOf(journals);
    }

    public List<EventEnvelope> events() {
        return List.copyOf(events);
    }

    @Override
    public Optional<StoredDeposit> findDepositByKey(String idempotencyKey) {
        return Optional.ofNullable(depositsByKey.get(idempotencyKey));
    }

    @Override
    public Optional<StoredDeposit> findDepositById(String depositId) {
        return Optional.ofNullable(depositsById.get(depositId));
    }

    @Override
    public Optional<StoredTransfer> findTransferByKey(String idempotencyKey) {
        return Optional.ofNullable(transfersByKey.get(idempotencyKey));
    }

    @Override
    public Optional<CustomerBook> findByOwner(String ownerSubject) {
        return byNumber.values().stream().filter(book -> ownerSubject.equals(book.ownerSubject())).findFirst();
    }

    @Override
    public Optional<CustomerBook> findByNumber(String accountNumber) {
        return Optional.ofNullable(byNumber.get(accountNumber));
    }

    @Override
    public Optional<CustomerBook> lockCustomer(String accountNumber) {
        return findByNumber(accountNumber);
    }

    @Override
    public CustomerBook lockVault() {
        return byNumber.get(LedgerPostingPolicy.VAULT);
    }

    @Override
    public void insertCustomer(CustomerBook book) {
        if (byNumber.containsKey(book.accountNumber())) {
            throw new org.springframework.dao.DuplicateKeyException(book.accountNumber());
        }
        byNumber.put(book.accountNumber(), book);
    }

    @Override
    public void saveBalance(CustomerBook book) {
        byNumber.put(book.accountNumber(), book);
    }

    @Override
    public void insertJournal(String journalId, String kind, String idempotencyKey, JournalPlan plan, Instant createdAt) {
        journals.add(plan);
    }

    @Override
    public void insertDeposit(StoredDeposit deposit) {
        if (depositsByKey.containsKey(deposit.idempotencyKey())) {
            throw new org.springframework.dao.DuplicateKeyException(deposit.idempotencyKey());
        }
        depositsById.put(deposit.id(), deposit);
        depositsByKey.put(deposit.idempotencyKey(), deposit);
    }

    @Override
    public Optional<StoredDeposit> lockDeposit(String depositId) {
        return findDepositById(depositId);
    }

    @Override
    public void markDepositPosted(String depositId, Instant postedAt) {
        StoredDeposit current = depositsById.get(depositId);
        StoredDeposit posted = current.postedAt(postedAt);
        depositsById.put(depositId, posted);
        depositsByKey.put(posted.idempotencyKey(), posted);
    }

    @Override
    public void insertTransfer(StoredTransfer transfer) {
        if (transfersByKey.containsKey(transfer.idempotencyKey())) {
            throw new org.springframework.dao.DuplicateKeyException(transfer.idempotencyKey());
        }
        transfersByKey.put(transfer.idempotencyKey(), transfer);
    }

    @Override
    public void enqueue(String topic, String messageKey, EventEnvelope event) {
        events.add(event);
    }
}
