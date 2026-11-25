package com.quickbank.bootstrap.identity;

interface IdentityDirectory {

    boolean emailExists(String email);

    boolean usernameExists(String username);

    boolean emailUsedBySomeoneElse(String email, String userId);

    StoredProfile profile(String userId);

    void updateProfile(String userId, String email, String firstName, String lastName);

    String createClient(String username, String email, String firstName, String lastName, String password);

    void deleteClient(String keycloakUserId);

    TokenSet passwordGrant(String username, String password);

    TokenSet refresh(String refreshToken);

    record TokenSet(String accessToken, String refreshToken, long expiresIn) {
    }

    record StoredProfile(String id, String username, String email, String firstName, String lastName) {
    }
}
