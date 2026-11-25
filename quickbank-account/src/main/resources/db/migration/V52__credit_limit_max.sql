ALTER TABLE acc_accounts
    ADD COLUMN credit_limit_max DECIMAL(19, 2) NULL;

UPDATE acc_accounts
SET credit_limit_max = credit_limit
WHERE credit_limit_max IS NULL;

ALTER TABLE acc_accounts
    MODIFY credit_limit_max DECIMAL(19, 2) NOT NULL;

ALTER TABLE acc_accounts
    ADD CONSTRAINT chk_acc_credit_limit_max CHECK (credit_limit_max >= 0),
    ADD CONSTRAINT chk_acc_credit_within_max CHECK (credit_limit <= credit_limit_max);
