package com.masterboxer.springbootmultitenancy.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * DEMO user store loaded from application.properties. Replace with a real store
 * (a users table per tenant schema) or an external identity provider.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(List<UserEntry> users) {

    public AuthProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    public record UserEntry(String tenant, String username, String passwordHash, List<String> roles) {

    }
}
