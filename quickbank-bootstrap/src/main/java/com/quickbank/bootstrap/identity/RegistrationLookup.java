package com.quickbank.bootstrap.identity;

interface RegistrationLookup {

    boolean emailTaken(String email);

    boolean emailTakenByOther(String email, String keycloakUserId);

    boolean usernameTaken(String username);

    void insert(RegistrationEntity entity);

    void updateContact(String keycloakUserId, String email, String firstName, String lastName);
}
