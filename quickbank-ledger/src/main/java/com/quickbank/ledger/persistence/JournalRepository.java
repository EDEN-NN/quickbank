package com.quickbank.ledger.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalRepository extends JpaRepository<JournalEntity, String> {
}
