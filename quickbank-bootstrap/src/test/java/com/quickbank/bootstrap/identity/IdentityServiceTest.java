package com.quickbank.bootstrap.identity;

import com.quickbank.bootstrap.identity.IdentityDirectory.StoredProfile;
import com.quickbank.bootstrap.identity.IdentityDirectory.TokenSet;
import com.quickbank.common.trace.QuickBankSpans;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityServiceTest {

    @Test
    void registerPersistsANewClient() {
        FakeDirectory directory = new FakeDirectory();
        FakeLookup lookup = new FakeLookup();
        IdentityService service = service(directory, lookup);

        String username = service.register("Carol", "Carol@QuickBank.local", "Carol", "Lima", "carol-pass");

        assertEquals("carol", username);
        assertEquals(List.of("carol"), directory.created);
        assertEquals(1, lookup.rows.size());
        assertEquals("carol@quickbank.local", lookup.rows.get(0).email());
        assertEquals("kc-carol", lookup.rows.get(0).keycloakUserId());
    }

    @Test
    void existingEmailIsRejectedBeforeCreate() {
        FakeDirectory directory = new FakeDirectory();
        directory.emails.add("ana@quickbank.local");
        FakeLookup lookup = new FakeLookup();
        lookup.usernames.add("ana");

        EmailAlreadyRegisteredException ex = assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> service(directory, lookup).register("ana", "Ana@quickbank.local", "Ana", "Lima", "ana-pass-1"));

        assertTrue(directory.created.isEmpty());
        assertFalse(ex.getMessage().contains("ana"));
        assertFalse(ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    void existingUsernameStaysGeneric() {
        FakeDirectory directory = new FakeDirectory();
        directory.usernames.add("bruno");
        FakeLookup lookup = new FakeLookup();

        RegistrationRejectedException ex = assertThrows(
                RegistrationRejectedException.class,
                () -> service(directory, lookup).register("Bruno", "bruno2@quickbank.local", "Bruno", "Costa", "bruno-pass"));

        assertTrue(directory.created.isEmpty());
        assertFalse(ex.getMessage().toLowerCase().contains("username"));
        assertFalse(ex.getMessage().contains("Bruno"));
        assertFalse(ex.getMessage().contains("bruno"));
        assertEquals("Registration was not accepted.", ex.getMessage());
    }

    @Test
    void localUsernameConflictDeletesNothingAndDoesNotCreate() {
        FakeLookup lookup = new FakeLookup();
        lookup.usernames.add("carol");
        FakeDirectory directory = new FakeDirectory();

        assertThrows(RegistrationRejectedException.class,
                () -> service(directory, lookup).register("carol", "new@quickbank.local", "Carol", "Lima", "carol-pass"));

        assertTrue(directory.created.isEmpty());
        assertTrue(directory.deleted.isEmpty());
    }

    @Test
    void failedInsertRemovesTheKeycloakUser() {
        FakeDirectory directory = new FakeDirectory();
        FakeLookup lookup = new FakeLookup();
        lookup.failInsert = true;

        assertThrows(RegistrationRejectedException.class,
                () -> service(directory, lookup).register("carol", "carol@quickbank.local", "Carol", "Lima", "carol-pass"));

        assertEquals(List.of("kc-carol"), directory.deleted);
    }

    @Test
    void constraintNamesDoNotLeakTheStoredValue() {
        RuntimeException username = RegistrationFailures.fromConstraint(
                "Duplicate entry 'ana' for key 'id_registrations.uk_id_username'");
        RuntimeException email = RegistrationFailures.fromConstraint(
                "Duplicate entry 'ana@quickbank.local' for key 'id_registrations.uk_id_email'");

        assertTrue(username instanceof RegistrationRejectedException);
        assertFalse(username.getMessage().contains("ana"));
        assertTrue(email instanceof EmailAlreadyRegisteredException);
        assertFalse(email.getMessage().contains("ana@quickbank.local"));
    }

    @Test
    void badLoginIsGeneric() {
        FakeDirectory directory = new FakeDirectory();
        directory.rejectLogin = true;

        SignInFailedException ex = assertThrows(
                SignInFailedException.class,
                () -> service(directory, new FakeLookup()).login("ana", "wrong-pass"));

        assertEquals("The credentials were not accepted.", ex.getMessage());
        assertFalse(ex.getMessage().toLowerCase().contains("ana"));
    }

    @Test
    void profileKeepsADuplicateEmailGeneric() {
        FakeDirectory directory = new FakeDirectory();
        directory.emails.add("bruno@quickbank.local");
        EmailAlreadyRegisteredException ex = assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> service(directory, new FakeLookup()).updateProfile(
                        "user-1", "Bruno@quickbank.local", "Ana", "Lima"));
        assertEquals("Registration was not accepted.", ex.getMessage());
        assertFalse(ex.getMessage().toLowerCase().contains("email"));
        assertTrue(directory.created.isEmpty());
    }

    @Test
    void profileUpdateRewritesTheStoredContact() {
        FakeDirectory directory = new FakeDirectory();
        FakeLookup lookup = new FakeLookup();
        StoredProfile updated = service(directory, lookup).updateProfile(
                "user-1", "Ana.Nova@quickbank.local", "Ana", "Nova");
        assertEquals("ana.nova@quickbank.local", updated.email());
        assertEquals("Ana", updated.firstName());
        assertEquals(1, lookup.rows.size());
        assertEquals(List.of("profile:ana.nova@quickbank.local"), directory.created);
    }

    @Test
    void loginReturnsTheKeycloakToken() {
        TokenSet tokens = service(new FakeDirectory(), new FakeLookup()).login("ana", "ana");

        assertEquals("access-ana", tokens.accessToken());
        assertEquals("refresh-ana", tokens.refreshToken());
    }

    private static IdentityService service(FakeDirectory directory, FakeLookup lookup) {
        return new IdentityService(
                directory,
                lookup,
                new QuickBankSpans(ObservationRegistry.create()),
                Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC));
    }

    private static final class FakeDirectory implements IdentityDirectory {
        private final List<String> emails = new ArrayList<>();
        private final List<String> usernames = new ArrayList<>();
        private final List<String> created = new ArrayList<>();
        private final List<String> deleted = new ArrayList<>();
        private boolean rejectLogin;

        @Override
        public boolean emailExists(String email) {
            return emails.contains(email);
        }

        @Override
        public boolean usernameExists(String username) {
            return usernames.contains(username);
        }

        @Override
        public boolean emailUsedBySomeoneElse(String email, String userId) {
            return emails.contains(email);
        }

        @Override
        public StoredProfile profile(String userId) {
            return new StoredProfile(userId, "ana", "ana@quickbank.local", "Ana", "Lima");
        }

        @Override
        public void updateProfile(String userId, String email, String firstName, String lastName) {
            created.add("profile:" + email);
        }

        @Override
        public String createClient(String username, String email, String firstName, String lastName, String password) {
            created.add(username);
            return "kc-" + username;
        }

        @Override
        public void deleteClient(String keycloakUserId) {
            deleted.add(keycloakUserId);
        }

        @Override
        public TokenSet passwordGrant(String username, String password) {
            if (rejectLogin) {
                throw new SignInFailedException();
            }
            return new TokenSet("access-" + username, "refresh-" + username, 1800);
        }

        @Override
        public TokenSet refresh(String refreshToken) {
            return new TokenSet("access-refreshed", refreshToken, 1800);
        }
    }

    private static final class FakeLookup implements RegistrationLookup {
        private final List<String> emails = new ArrayList<>();
        private final List<String> usernames = new ArrayList<>();
        private final List<RegistrationEntity> rows = new ArrayList<>();
        private boolean failInsert;

        @Override
        public boolean emailTaken(String email) {
            return emails.contains(email);
        }

        @Override
        public boolean emailTakenByOther(String email, String keycloakUserId) {
            return emails.contains(email);
        }

        @Override
        public boolean usernameTaken(String username) {
            return usernames.contains(username);
        }

        @Override
        public void insert(RegistrationEntity entity) {
            if (failInsert) {
                throw new RegistrationRejectedException();
            }
            rows.add(entity);
        }

        @Override
        public void updateContact(String keycloakUserId, String email, String firstName, String lastName) {
            rows.add(new RegistrationEntity("row", "ana", email, firstName, lastName, keycloakUserId, java.time.Instant.EPOCH));
        }
    }
}
