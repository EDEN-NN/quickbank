package com.quickbank.bootstrap.identity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<RegistrationEntity, String> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
