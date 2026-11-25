package com.quickbank.bootstrap.identity;

interface RegistrationLookup {

    boolean emailTaken(String email);

    boolean usernameTaken(String username);

    void insert(RegistrationEntity entity);
}
