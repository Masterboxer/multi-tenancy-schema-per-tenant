package com.masterboxer.springbootmultitenancy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * The single source of truth for which tenant schemas exist.
 */
@Component
public class TenantRegistry {

    // Defense in depth: schema names are only ever lowercase identifiers.
    private static final Pattern SAFE_SCHEMA_NAME = Pattern.compile("[a-z_][a-z0-9_]{0,62}");

    private final Set<String> schemas;

    public TenantRegistry(@Value("${app.schemas}") Set<String> schemas) {
        schemas.forEach(schema -> {
            if (!SAFE_SCHEMA_NAME.matcher(schema).matches()) {
                throw new IllegalStateException("Invalid tenant schema name in app.schemas: " + schema);
            }
        });
        this.schemas = Set.copyOf(schemas);
    }

    public boolean isAllowed(String tenant) {
        return tenant != null && schemas.contains(tenant);
    }
}
