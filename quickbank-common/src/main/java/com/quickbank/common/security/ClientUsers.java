package com.quickbank.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

public final class ClientUsers {

    private ClientUsers() {
    }

    public static ClientUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No signed-in QuickBank user.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return from(oidcUser);
        }
        if (principal instanceof Jwt jwt) {
            return from(jwt);
        }
        throw new IllegalStateException("Unsupported principal: " + principal.getClass().getName());
    }

    public static ClientUser from(Jwt jwt) {
        String name = first(jwt.getClaimAsString("preferred_username"), jwt.getClaimAsString("name"), jwt.getSubject());
        String email = first(jwt.getClaimAsString("email"), name + "@quickbank.local");
        return new ClientUser(jwt.getSubject(), email, name);
    }

    public static ClientUser from(OidcUser user) {
        String name = first(user.getPreferredUsername(), user.getFullName(), user.getSubject());
        String email = first(user.getEmail(), name + "@quickbank.local");
        return new ClientUser(user.getSubject(), email, name);
    }

    private static String first(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "client";
    }
}
