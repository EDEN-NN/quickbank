package com.quickbank.ledger.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccountEntity, String> {

    Optional<LedgerAccountEntity> findByOwnerSubject(String ownerSubject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LedgerAccountEntity a where a.accountNumber = :accountNumber")
    Optional<LedgerAccountEntity> lockByNumber(@Param("accountNumber") String accountNumber);
}
