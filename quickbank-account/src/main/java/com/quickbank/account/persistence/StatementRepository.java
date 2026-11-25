package com.quickbank.account.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatementRepository extends JpaRepository<StatementEntryEntity, String> {

    List<StatementEntryEntity> findByAccountNumberOrderByOccurredAtAscIdAsc(String accountNumber);

    boolean existsByAccountNumberAndSourceEventIdAndEntryType(String accountNumber, String sourceEventId, String entryType);
}
