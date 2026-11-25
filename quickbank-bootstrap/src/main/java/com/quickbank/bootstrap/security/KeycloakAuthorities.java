package com.quickbank.bootstrap.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KeycloakAuthorities {

    private KeycloakAuthorities() {
    }

    public static Collection<GrantedAuthority> fromJwt(Jwt jwt) {
        return fromRealmAccess(jwt.getClaim("realm_access"));
    }

    public static Collection<GrantedAuthority> fromAccessToken(String token) {
        try {
            var parsed = com.nimbusds.jwt.JWTParser.parse(token);
            return fromRealmAccess(parsed.getJWTClaimsSet().getClaim("realm_access"));
        } catch (ParseException ex) {
            return List.of();
        }
    }

    static Collection<GrantedAuthority> fromRealmAccess(Object realmAccess) {
        if (!(realmAccess instanceof Map<?, ?> map)) {
            return List.of();
        }
        Object roles = map.get("roles");
        if (!(roles instanceof Collection<?> collection)) {
            return List.of();
        }
        Set<GrantedAuthority> granted = new HashSet<>();
        for (Object role : collection) {
            if (role instanceof String value && !value.isBlank()) {
                granted.add(new SimpleGrantedAuthority(value.startsWith("ROLE_") ? value : "ROLE_" + value));
            }
        }
        return granted;
    }
}
