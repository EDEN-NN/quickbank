package com.quickbank.bootstrap.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Component
class KeycloakIdentityClient implements IdentityDirectory {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityClient.class);
    private static final String ADMIN_REALM = "master";
    private static final String CLIENT_ROLE = "CLIENT";

    private final IdentityProperties properties;
    private final Clock clock;
    private final RestClient http;
    private final Object adminLock = new Object();
    private String adminAccess;
    private Instant adminExpiry = Instant.EPOCH;
    private volatile boolean basicScopeReady;

    KeycloakIdentityClient(IdentityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        String base = properties.keycloakBaseUrl().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.http = RestClient.builder().baseUrl(base).build();
    }

    @Override
    public boolean emailExists(String email) {
        for (UserHit hit : search("email", email)) {
            if (hit.email() != null && hit.email().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean usernameExists(String username) {
        for (UserHit hit : search("username", username)) {
            if (hit.username() != null && hit.username().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String createClient(String username, String email, String firstName, String lastName, String password) {
        NewUser body = new NewUser(
                username,
                email,
                firstName,
                lastName,
                true,
                true,
                List.of(new Credential("password", password, false)));
        try {
            var response = withAdmin(token -> http.post()
                    .uri("/admin/realms/{realm}/users", properties.realm())
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity());
            var location = response.getHeaders().getLocation();
            if (location == null) {
                log.warn("keycloak create user returned no location");
                throw new RegistrationRejectedException();
            }
            String path = location.getPath();
            String id = path.substring(path.lastIndexOf('/') + 1);
            assignClientRole(id);
            return id;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                String text = ex.getResponseBodyAsString().toLowerCase(Locale.ROOT);
                if (text.contains("email")) {
                    throw new EmailAlreadyRegisteredException();
                }
                throw new RegistrationRejectedException();
            }
            log.warn("keycloak create user failed status={}", ex.getStatusCode().value());
            throw ex;
        }
    }

    @Override
    public void deleteClient(String keycloakUserId) {
        try {
            withAdmin(token -> http.delete()
                    .uri("/admin/realms/{realm}/users/{id}", properties.realm(), keycloakUserId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity());
        } catch (RestClientResponseException ex) {
            log.warn("keycloak delete user failed status={}", ex.getStatusCode().value());
        } catch (RuntimeException ex) {
            log.warn("keycloak delete user failed");
        }
    }

    @Override
    public TokenSet passwordGrant(String username, String password) {
        ensureBasicScope();
        MultiValueMap<String, String> form = clientForm();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        form.add("scope", "openid profile email");
        return tokenOrReject(form);
    }

    @Override
    public TokenSet refresh(String refreshToken) {
        MultiValueMap<String, String> form = clientForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return tokenOrReject(form);
    }

    private void ensureBasicScope() {
        if (basicScopeReady) {
            return;
        }
        synchronized (adminLock) {
            if (basicScopeReady) {
                return;
            }
            try {
                NamedId[] clients = withAdmin(token -> http.get()
                        .uri(uri -> uri.path("/admin/realms/{realm}/clients")
                                .queryParam("clientId", properties.tokenClientId())
                                .build(properties.realm()))
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .body(NamedId[].class));
                NamedId[] scopes = withAdmin(token -> http.get()
                        .uri("/admin/realms/{realm}/client-scopes", properties.realm())
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .body(NamedId[].class));
                if (clients == null || clients.length == 0 || clients[0].id() == null || scopes == null) {
                    log.warn("keycloak token client or basic scope was not found");
                    return;
                }
                String basicId = null;
                for (NamedId scope : scopes) {
                    if ("basic".equals(scope.name())) {
                        basicId = scope.id();
                    }
                }
                if (basicId == null) {
                    log.warn("keycloak basic client scope is missing");
                    return;
                }
                String clientUuid = clients[0].id();
                NamedId[] assigned = withAdmin(token -> http.get()
                        .uri("/admin/realms/{realm}/clients/{id}/default-client-scopes", properties.realm(), clientUuid)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .body(NamedId[].class));
                if (assigned != null) {
                    for (NamedId scope : assigned) {
                        if (basicId.equals(scope.id())) {
                            basicScopeReady = true;
                            return;
                        }
                    }
                }
                String scopeId = basicId;
                withAdmin(token -> http.put()
                        .uri("/admin/realms/{realm}/clients/{id}/default-client-scopes/{scopeId}",
                                properties.realm(), clientUuid, scopeId)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .toBodilessEntity());
                log.info("keycloak basic scope attached client={}", properties.tokenClientId());
                basicScopeReady = true;
            } catch (RuntimeException ex) {
                log.warn("keycloak basic scope was not attached");
            }
        }
    }

    private TokenSet tokenOrReject(MultiValueMap<String, String> form) {
        try {
            TokenResponse token = postToken(properties.realm(), form);
            if (token.accessToken() == null || token.accessToken().isBlank()) {
                throw new SignInFailedException();
            }
            long expires = token.expiresIn() == null ? 300 : token.expiresIn();
            return new TokenSet(token.accessToken(), token.refreshToken(), expires);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 400 || status == 401) {
                throw new SignInFailedException();
            }
            log.warn("keycloak token request failed status={}", status);
            throw ex;
        }
    }

    private void assignClientRole(String userId) {
        try {
            RoleRepresentation role = withAdmin(token -> http.get()
                    .uri("/admin/realms/{realm}/roles/{role}", properties.realm(), CLIENT_ROLE)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(RoleRepresentation.class));
            if (role == null || role.id() == null) {
                throw new IllegalStateException("CLIENT role is missing");
            }
            withAdmin(token -> http.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", properties.realm(), userId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity());
        } catch (RuntimeException ex) {
            deleteClient(userId);
            log.warn("keycloak role assignment failed");
            throw ex;
        }
    }

    private UserHit[] search(String field, String value) {
        UserHit[] hits = withAdmin(token -> http.get()
                .uri(uri -> uri.path("/admin/realms/{realm}/users")
                        .queryParam(field, value)
                        .queryParam("exact", "true")
                        .build(properties.realm()))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(UserHit[].class));
        return hits == null ? new UserHit[0] : hits;
    }

    private <T> T withAdmin(AdminCall<T> call) {
        try {
            return call.apply(adminToken(false));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() != 401) {
                throw ex;
            }
            return call.apply(adminToken(true));
        }
    }

    private String adminToken(boolean force) {
        synchronized (adminLock) {
            if (!force && adminAccess != null && clock.instant().isBefore(adminExpiry)) {
                return adminAccess;
            }
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", "admin-cli");
            form.add("username", properties.adminUsername());
            form.add("password", properties.adminPassword());
            TokenResponse token = postToken(ADMIN_REALM, form);
            if (token.accessToken() == null || token.accessToken().isBlank()) {
                throw new IllegalStateException("Keycloak admin token is missing");
            }
            long seconds = token.expiresIn() == null ? 30 : Math.max(5, token.expiresIn() - 15);
            adminAccess = token.accessToken();
            adminExpiry = clock.instant().plusSeconds(seconds);
            return adminAccess;
        }
    }

    private TokenResponse postToken(String realm, MultiValueMap<String, String> form) {
        TokenResponse body = http.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (body == null) {
            throw new IllegalStateException("Keycloak token response is empty");
        }
        return body;
    }

    private MultiValueMap<String, String> clientForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.tokenClientId());
        form.add("client_secret", properties.tokenClientSecret());
        return form;
    }

    @FunctionalInterface
    private interface AdminCall<T> {
        T apply(String token);
    }

    private record NewUser(
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            boolean emailVerified,
            List<Credential> credentials
    ) {
    }

    private record Credential(String type, String value, boolean temporary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserHit(String id, String username, String email) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NamedId(String id, String name, String clientId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RoleRepresentation(String id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn
    ) {
    }
}
