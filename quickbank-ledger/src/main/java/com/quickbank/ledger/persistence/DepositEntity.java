package com.quickbank.ledger.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "led_deposits")
public class DepositEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "idempotency_key", length = 80, nullable = false)
    private String idempotencyKey;

    @Column(name = "owner_subject", length = 128, nullable = false)
    private String ownerSubject;

    @Column(name = "account_number", length = 16, nullable = false)
    private String accountNumber;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    protected DepositEntity() {
    }

    public DepositEntity(
            String id,
            String idempotencyKey,
            String ownerSubject,
            String accountNumber,
            BigDecimal amount,
            String status,
            Instant createdAt
    ) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.ownerSubject = ownerSubject;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getOwnerSubject() {
        return ownerSubject;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void markPosted(Instant postedAt) {
        this.status = "POSTED";
        this.postedAt = postedAt;
    }
}
