package com.quickbank.account.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "acc_snapshots")
public class SnapshotEntity {

    @Id
    @Column(name = "account_number", length = 16, nullable = false)
    private String accountNumber;

    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "credit_used", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditUsed;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SnapshotEntity() {
    }

    public SnapshotEntity(
            String accountNumber,
            BigDecimal cashBalance,
            BigDecimal creditLimit,
            BigDecimal creditUsed,
            Instant updatedAt
    ) {
        this.accountNumber = accountNumber;
        this.cashBalance = cashBalance;
        this.creditLimit = creditLimit;
        this.creditUsed = creditUsed;
        this.updatedAt = updatedAt;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCreditUsed() {
        return creditUsed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
