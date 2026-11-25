package com.quickbank.ledger.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<TransferEntity, String> {

    Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);
}
