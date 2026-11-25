package com.quickbank.ledger.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostingRepository extends JpaRepository<PostingEntity, Long> {
}
