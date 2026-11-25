package com.quickbank.bootstrap.identity;

interface IdentityDirectory {

    boolean emailExists(String email);

    boolean usernameExists(String username);

    String createClient(String username, String email, String firstName, String lastName, String password);

    void deleteClient(String keycloakUserId);

    TokenSet passwordGrant(String username, String password);

    TokenSet refresh(String refreshToken);

    record TokenSet(String accessToken, String refreshToken, long expiresIn) {
    }
}
