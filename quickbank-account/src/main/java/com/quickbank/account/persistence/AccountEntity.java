package com.quickbank.account.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "acc_accounts")
public class AccountEntity {

    @Id
    @Column(name = "account_number", length = 16, nullable = false)
    private String accountNumber;

    @Column(name = "owner_subject", length = 128, nullable = false)
    private String ownerSubject;

    @Column(name = "owner_email", length = 255, nullable = false)
    private String ownerEmail;

    @Column(name = "owner_name", length = 120, nullable = false)
    private String ownerName;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "credit_limit_max", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimitMax;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    protected AccountEntity() {
    }

    public AccountEntity(
            String accountNumber,
            String ownerSubject,
            String ownerEmail,
            String ownerName,
            BigDecimal creditLimit,
            BigDecimal creditLimitMax,
            Instant openedAt
    ) {
        this.accountNumber = accountNumber;
        this.ownerSubject = ownerSubject;
        this.ownerEmail = ownerEmail;
        this.ownerName = ownerName;
        this.creditLimit = creditLimit;
        this.creditLimitMax = creditLimitMax;
        this.openedAt = openedAt;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerSubject() {
        return ownerSubject;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getCreditLimitMax() {
        return creditLimitMax;
    }

    public void renameOwner(String ownerName, String ownerEmail) {
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }
}
