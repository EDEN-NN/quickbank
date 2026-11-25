CREATE TABLE ntf_inbox (
    event_id VARCHAR(36) NOT NULL,
    template VARCHAR(40) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id),
    KEY idx_ntf_recipient (recipient)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
