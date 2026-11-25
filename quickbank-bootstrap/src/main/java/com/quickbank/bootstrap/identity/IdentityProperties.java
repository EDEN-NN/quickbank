package com.quickbank.bootstrap.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "quickbank.identity")
public record IdentityProperties(
        URI keycloakBaseUrl,
        String realm,
        String tokenClientId,
        String tokenClientSecret,
        String adminUsername,
        String adminPassword
) {
}
