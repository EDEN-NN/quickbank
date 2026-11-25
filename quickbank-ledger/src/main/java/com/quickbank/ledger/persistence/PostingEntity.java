package com.quickbank.ledger.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "led_postings")
public class PostingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_id", length = 36, nullable = false)
    private String journalId;

    @Column(name = "account_number", length = 16, nullable = false)
    private String accountNumber;

    @Column(name = "direction", length = 6, nullable = false)
    private String direction;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    protected PostingEntity() {
    }

    public PostingEntity(String journalId, String accountNumber, String direction, BigDecimal amount) {
        this.journalId = journalId;
        this.accountNumber = accountNumber;
        this.direction = direction;
        this.amount = amount;
    }
}
