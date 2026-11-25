package com.quickbank.ledger.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "led_accounts")
public class LedgerAccountEntity {

    @Id
    @Column(name = "account_number", length = 16, nullable = false)
    private String accountNumber;

    @Column(name = "owner_subject", length = 128)
    private String ownerSubject;

    @Column(name = "owner_email", length = 255)
    private String ownerEmail;

    @Column(name = "owner_name", length = 120)
    private String ownerName;

    @Column(name = "kind", length = 16, nullable = false)
    private String kind;

    @Column(name = "cash_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashBalance;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    protected LedgerAccountEntity() {
    }

    public LedgerAccountEntity(
            String accountNumber,
            String ownerSubject,
            String ownerEmail,
            String ownerName,
            String kind,
            BigDecimal cashBalance,
            Instant openedAt
    ) {
        this.accountNumber = accountNumber;
        this.ownerSubject = ownerSubject;
        this.ownerEmail = ownerEmail;
        this.ownerName = ownerName;
        this.kind = kind;
        this.cashBalance = cashBalance;
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

    public String getKind() {
        return kind;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }
}
