CREATE TABLE pay_inbox (
    payment_id VARCHAR(36) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pay_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_version INT NOT NULL,
    topic VARCHAR(128) NOT NULL,
    message_key VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    body JSON NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_outbox_event (event_id),
    KEY idx_pay_outbox_unpublished (published_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
