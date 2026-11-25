package com.quickbank.ledger.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "led_transfers")
public class TransferEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "idempotency_key", length = 80, nullable = false)
    private String idempotencyKey;

    @Column(name = "owner_subject", length = 128, nullable = false)
    private String ownerSubject;

    @Column(name = "from_account", length = 16, nullable = false)
    private String fromAccount;

    @Column(name = "to_account", length = 16, nullable = false)
    private String toAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "from_balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal fromBalanceAfter;

    @Column(name = "to_balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal toBalanceAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TransferEntity() {
    }

    public TransferEntity(
            String id,
            String idempotencyKey,
            String ownerSubject,
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            BigDecimal fromBalanceAfter,
            BigDecimal toBalanceAfter,
            Instant createdAt
    ) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.ownerSubject = ownerSubject;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.fromBalanceAfter = fromBalanceAfter;
        this.toBalanceAfter = toBalanceAfter;
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

    public String getFromAccount() {
        return fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFromBalanceAfter() {
        return fromBalanceAfter;
    }

    public BigDecimal getToBalanceAfter() {
        return toBalanceAfter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
