CREATE TABLE led_accounts (
    account_number VARCHAR(16) NOT NULL,
    owner_subject VARCHAR(128) NULL,
    owner_email VARCHAR(255) NULL,
    owner_name VARCHAR(120) NULL,
    kind VARCHAR(16) NOT NULL,
    cash_balance DECIMAL(19, 2) NOT NULL,
    opened_at DATETIME(6) NOT NULL,
    PRIMARY KEY (account_number),
    UNIQUE KEY uk_led_owner (owner_subject),
    CONSTRAINT chk_led_cash_nonneg CHECK (cash_balance >= 0),
    CONSTRAINT chk_led_kind CHECK (kind IN ('CUSTOMER', 'VAULT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO led_accounts (account_number, owner_subject, owner_email, owner_name, kind, cash_balance, opened_at)
VALUES ('VAULT', NULL, NULL, 'Cash vault', 'VAULT', 0.00, CURRENT_TIMESTAMP(6));

CREATE TABLE led_journals (
    id VARCHAR(36) NOT NULL,
    kind VARCHAR(24) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_led_journal_kind (kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE led_postings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    journal_id VARCHAR(36) NOT NULL,
    account_number VARCHAR(16) NOT NULL,
    direction VARCHAR(6) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_led_postings_journal (journal_id),
    KEY idx_led_postings_account (account_number),
    CONSTRAINT fk_led_postings_journal FOREIGN KEY (journal_id) REFERENCES led_journals (id),
    CONSTRAINT chk_led_posting_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_led_posting_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE led_deposits (
    id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    owner_subject VARCHAR(128) NOT NULL,
    account_number VARCHAR(16) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    posted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_led_deposit_idem (idempotency_key),
    KEY idx_led_deposit_account (account_number),
    CONSTRAINT chk_led_deposit_amount CHECK (amount > 0),
    CONSTRAINT chk_led_deposit_status CHECK (status IN ('PENDING', 'POSTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE led_transfers (
    id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(80) NOT NULL,
    owner_subject VARCHAR(128) NOT NULL,
    from_account VARCHAR(16) NOT NULL,
    to_account VARCHAR(16) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    from_balance_after DECIMAL(19, 2) NOT NULL,
    to_balance_after DECIMAL(19, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_led_transfer_idem (idempotency_key),
    KEY idx_led_transfer_from (from_account),
    KEY idx_led_transfer_to (to_account),
    CONSTRAINT chk_led_transfer_amount CHECK (amount > 0),
    CONSTRAINT chk_led_transfer_not_self CHECK (from_account <> to_account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE led_outbox (
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
    UNIQUE KEY uk_led_outbox_event (event_id),
    KEY idx_led_outbox_unpublished (published_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
