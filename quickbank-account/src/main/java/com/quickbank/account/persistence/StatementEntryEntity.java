package com.quickbank.account.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "acc_statement_entries")
public class StatementEntryEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "account_number", length = 16, nullable = false)
    private String accountNumber;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "entry_type", length = 24, nullable = false)
    private String entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "counterparty", length = 64, nullable = false)
    private String counterparty;

    @Column(name = "running_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal runningBalance;

    @Column(name = "transfer_id", length = 36)
    private String transferId;

    @Column(name = "source_event_id", length = 36, nullable = false)
    private String sourceEventId;

    protected StatementEntryEntity() {
    }

    public StatementEntryEntity(
            String id,
            String accountNumber,
            Instant occurredAt,
            String entryType,
            BigDecimal amount,
            String counterparty,
            BigDecimal runningBalance,
            String transferId,
            String sourceEventId
    ) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.occurredAt = occurredAt;
        this.entryType = entryType;
        this.amount = amount;
        this.counterparty = counterparty;
        this.runningBalance = runningBalance;
        this.transferId = transferId;
        this.sourceEventId = sourceEventId;
    }

    public String getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }
}
