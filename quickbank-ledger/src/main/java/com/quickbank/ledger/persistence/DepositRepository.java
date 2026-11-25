package com.quickbank.ledger.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DepositRepository extends JpaRepository<DepositEntity, String> {

    Optional<DepositEntity> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepositEntity d where d.id = :id")
    Optional<DepositEntity> lockById(@Param("id") String id);
}
