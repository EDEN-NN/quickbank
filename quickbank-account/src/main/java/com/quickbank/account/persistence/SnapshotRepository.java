package com.quickbank.account.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotRepository extends JpaRepository<SnapshotEntity, String> {
}
