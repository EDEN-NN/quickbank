CREATE TABLE acc_number_seq (
    id TINYINT NOT NULL,
    next_number BIGINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO acc_number_seq (id, next_number) VALUES (1, 10000001);

CREATE TABLE acc_accounts (
    account_number VARCHAR(16) NOT NULL,
    owner_subject VARCHAR(128) NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    owner_name VARCHAR(120) NOT NULL,
    credit_limit DECIMAL(19, 2) NOT NULL,
    opened_at DATETIME(6) NOT NULL,
    PRIMARY KEY (account_number),
    UNIQUE KEY uk_acc_owner (owner_subject),
    CONSTRAINT chk_acc_credit_limit CHECK (credit_limit >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE acc_snapshots (
    account_number VARCHAR(16) NOT NULL,
    cash_balance DECIMAL(19, 2) NOT NULL,
    credit_limit DECIMAL(19, 2) NOT NULL,
    credit_used DECIMAL(19, 2) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (account_number),
    CONSTRAINT fk_acc_snapshot_account FOREIGN KEY (account_number) REFERENCES acc_accounts (account_number),
    CONSTRAINT chk_acc_cash_nonneg CHECK (cash_balance >= 0),
    CONSTRAINT chk_acc_credit_used_nonneg CHECK (credit_used >= 0),
    CONSTRAINT chk_acc_credit_used_within_limit CHECK (credit_used <= credit_limit)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE acc_statement_entries (
    id VARCHAR(36) NOT NULL,
    account_number VARCHAR(16) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    entry_type VARCHAR(24) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    counterparty VARCHAR(64) NOT NULL,
    running_balance DECIMAL(19, 2) NOT NULL,
    transfer_id VARCHAR(36) NULL,
    source_event_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_acc_stmt_source (account_number, source_event_id, entry_type),
    KEY idx_acc_stmt_order (account_number, occurred_at, id),
    CONSTRAINT chk_acc_stmt_amount CHECK (amount > 0),
    CONSTRAINT chk_acc_stmt_type CHECK (entry_type IN ('DEPOSIT', 'TRANSFER_IN', 'TRANSFER_OUT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE acc_outbox (
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
    UNIQUE KEY uk_acc_outbox_event (event_id),
    KEY idx_acc_outbox_unpublished (published_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
