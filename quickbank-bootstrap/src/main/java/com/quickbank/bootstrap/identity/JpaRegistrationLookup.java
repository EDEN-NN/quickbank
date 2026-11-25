package com.quickbank.bootstrap.identity;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaRegistrationLookup implements RegistrationLookup {

    private final RegistrationRepository registrations;

    JpaRegistrationLookup(RegistrationRepository registrations) {
        this.registrations = registrations;
    }

    @Override
    public boolean emailTaken(String email) {
        return registrations.existsByEmail(email);
    }

    @Override
    public boolean emailTakenByOther(String email, String keycloakUserId) {
        return registrations.existsByEmailAndKeycloakUserIdNot(email, keycloakUserId);
    }

    @Override
    public boolean usernameTaken(String username) {
        return registrations.existsByUsername(username);
    }

    @Override
    @Transactional
    public void insert(RegistrationEntity entity) {
        try {
            registrations.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            throw RegistrationFailures.fromConstraint(String.valueOf(ex.getMostSpecificCause().getMessage()));
        }
    }

    @Override
    @Transactional
    public void updateContact(String keycloakUserId, String email, String firstName, String lastName) {
        registrations.findByKeycloakUserId(keycloakUserId).ifPresent(entity -> {
            entity.rename(email, firstName, lastName);
            try {
                registrations.saveAndFlush(entity);
            } catch (DataIntegrityViolationException ex) {
                throw RegistrationFailures.fromConstraint(String.valueOf(ex.getMostSpecificCause().getMessage()));
            }
        });
    }
}
