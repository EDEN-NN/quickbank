package com.quickbank.bootstrap.identity;

import com.quickbank.bootstrap.identity.IdentityDirectory.StoredProfile;
import com.quickbank.bootstrap.identity.IdentityDirectory.TokenSet;
import com.quickbank.common.trace.QuickBankSpans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class IdentityService {

    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]{3,64}");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final IdentityDirectory directory;
    private final RegistrationLookup registrations;
    private final QuickBankSpans spans;
    private final Clock clock;

    public IdentityService(
            IdentityDirectory directory,
            RegistrationLookup registrations,
            QuickBankSpans spans,
            Clock clock
    ) {
        this.directory = directory;
        this.registrations = registrations;
        this.spans = spans;
        this.clock = clock;
    }

    public String register(String rawUsername, String rawEmail, String rawFirstName, String rawLastName, String password) {
        return spans.run(
                "quickbank.identity.register",
                Map.of("module", "identity", "operation", "register"),
                Map.of(),
                bag -> {
                    String username = normalize(rawUsername);
                    String email = normalize(rawEmail);
                    String firstName = personName(rawFirstName);
                    String lastName = personName(rawLastName);
                    if (!USERNAME.matcher(username).matches() || email.length() > 255 || !EMAIL.matcher(email).matches()
                            || !validName(firstName) || !validName(lastName)
                            || password == null || password.length() < 8 || password.length() > 72) {
                        log.info("registration rejected reason=invalid");
                        bag.attribute("registration.result", "rejected");
                        throw new InvalidRegistrationException();
                    }
                    if (registrations.emailTaken(email) || directory.emailExists(email)) {
                        log.info("registration rejected reason=email_exists");
                        bag.attribute("registration.result", "rejected");
                        throw new EmailAlreadyRegisteredException();
                    }
                    if (registrations.usernameTaken(username) || directory.usernameExists(username)) {
                        log.info("registration rejected reason=username_exists");
                        bag.attribute("registration.result", "rejected");
                        throw new RegistrationRejectedException();
                    }
                    String keycloakUserId = directory.createClient(username, email, firstName, lastName, password);
                    try {
                        registrations.insert(new RegistrationEntity(
                                UUID.randomUUID().toString(),
                                username,
                                email,
                                firstName,
                                lastName,
                                keycloakUserId,
                                clock.instant()));
                    } catch (RuntimeException ex) {
                        directory.deleteClient(keycloakUserId);
                        bag.attribute("registration.result", "rejected");
                        throw ex;
                    }
                    log.info("registration accepted username={}", username);
                    bag.attribute("registration.result", "accepted");
                    return username;
                });
    }

    public StoredProfile profile(String userId) {
        return spans.run(
                "quickbank.identity.profile",
                Map.of("module", "identity", "operation", "profile-read"),
                Map.of(),
                () -> directory.profile(userId));
    }

    public StoredProfile updateProfile(String userId, String rawEmail, String rawFirstName, String rawLastName) {
        return spans.run(
                "quickbank.identity.profile-update",
                Map.of("module", "identity", "operation", "profile-update"),
                Map.of(),
                bag -> {
                    String email = normalize(rawEmail);
                    String firstName = personName(rawFirstName);
                    String lastName = personName(rawLastName);
                    if (email.length() > 255 || !EMAIL.matcher(email).matches() || !validName(firstName) || !validName(lastName)) {
                        log.info("profile rejected reason=invalid");
                        bag.attribute("profile.result", "rejected");
                        throw new InvalidRegistrationException();
                    }
                    StoredProfile current = directory.profile(userId);
                    if (!email.equals(normalize(current.email()))
                            && (registrations.emailTakenByOther(email, userId) || directory.emailUsedBySomeoneElse(email, userId))) {
                        log.info("profile rejected reason=email_exists");
                        bag.attribute("profile.result", "rejected");
                        throw new EmailAlreadyRegisteredException();
                    }
                    directory.updateProfile(userId, email, firstName, lastName);
                    registrations.updateContact(userId, email, firstName, lastName);
                    log.info("profile updated username={}", current.username());
                    bag.attribute("profile.result", "accepted");
                    return new StoredProfile(current.id(), current.username(), email, firstName, lastName);
                });
    }

    public TokenSet login(String rawUsername, String password) {
        return spans.run(
                "quickbank.identity.sign-in",
                Map.of("module", "identity", "operation", "sign-in"),
                Map.of(),
                bag -> {
                    String username = rawUsername == null ? "" : rawUsername.trim();
                    if (username.isEmpty() || password == null || password.isEmpty()) {
                        log.info("sign-in rejected");
                        bag.attribute("auth.result", "rejected");
                        throw new SignInFailedException();
                    }
                    try {
                        TokenSet tokens = directory.passwordGrant(username, password);
                        log.info("sign-in accepted username={}", username);
                        bag.attribute("auth.result", "accepted");
                        return tokens;
                    } catch (SignInFailedException ex) {
                        log.info("sign-in rejected");
                        bag.attribute("auth.result", "rejected");
                        throw ex;
                    }
                });
    }

    public TokenSet refresh(String refreshToken) {
        return spans.run(
                "quickbank.identity.refresh",
                Map.of("module", "identity", "operation", "refresh"),
                Map.of(),
                () -> directory.refresh(refreshToken));
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String personName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    private static boolean validName(String name) {
        return !name.isEmpty() && name.length() <= 80 && name.codePoints().allMatch(cp -> cp > 31);
    }
}
