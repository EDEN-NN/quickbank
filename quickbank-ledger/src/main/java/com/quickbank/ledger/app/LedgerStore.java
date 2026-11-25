package com.quickbank.ledger.app;

import com.quickbank.common.event.EventEnvelope;
import com.quickbank.ledger.domain.JournalPlan;

import java.time.Instant;
import java.util.Optional;

public interface LedgerStore {

    Optional<StoredDeposit> findDepositByKey(String idempotencyKey);

    Optional<StoredDeposit> findDepositById(String depositId);

    Optional<StoredTransfer> findTransferByKey(String idempotencyKey);

    Optional<CustomerBook> findByOwner(String ownerSubject);

    Optional<CustomerBook> findByNumber(String accountNumber);

    Optional<CustomerBook> lockCustomer(String accountNumber);

    CustomerBook lockVault();

    void insertCustomer(CustomerBook book);

    void saveBalance(CustomerBook book);

    void insertJournal(String journalId, String kind, String idempotencyKey, JournalPlan plan, Instant createdAt);

    void insertDeposit(StoredDeposit deposit);

    Optional<StoredDeposit> lockDeposit(String depositId);

    void markDepositPosted(String depositId, Instant postedAt);

    void insertTransfer(StoredTransfer transfer);

    void enqueue(String topic, String messageKey, EventEnvelope event);
}
