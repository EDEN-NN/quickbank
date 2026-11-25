CREATE TABLE id_registrations (
    id VARCHAR(36) NOT NULL,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(255) NOT NULL,
    keycloak_user_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_id_username (username),
    UNIQUE KEY uk_id_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
