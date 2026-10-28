package com.masterboxer.springbootmultitenancy.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret,
        @DefaultValue("multi-tenancy-app")
        String issuer,
        @DefaultValue("30m")
        Duration ttl) {

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret (JWT_SECRET) must be at least 32 bytes long");
        }
    }

    public SecretKey signingKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", ttl=" + ttl + "]"; // never print the secret
    }
}
