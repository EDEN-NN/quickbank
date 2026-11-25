package com.quickbank.bootstrap.identity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<RegistrationEntity, String> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndKeycloakUserIdNot(String email, String keycloakUserId);

    boolean existsByUsername(String username);

    java.util.Optional<RegistrationEntity> findByKeycloakUserId(String keycloakUserId);
}
