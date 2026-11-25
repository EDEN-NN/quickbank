package com.quickbank.ledger.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "led_journals")
public class JournalEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "kind", length = 24, nullable = false)
    private String kind;

    @Column(name = "idempotency_key", length = 80, nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JournalEntity() {
    }

    public JournalEntity(String id, String kind, String idempotencyKey, Instant createdAt) {
        this.id = id;
        this.kind = kind;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }
}
