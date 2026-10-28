package com.masterboxer.springbootmultitenancy.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which tenant the current request belongs to. The tenant comes
 * ONLY from the verified JWT's "tenant" claim, never from a client-supplied
 * header. No default tenant: a missing tenant fails loudly instead of falling
 * back to "public".
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    public static final String TENANT_CLAIM = "tenant";

    @Override
    public String resolveCurrentTenantIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String tenant = jwtAuth.getToken().getClaimAsString(TENANT_CLAIM);
            if (tenant != null && !tenant.isBlank()) {
                return tenant;
            }
        }
        throw new IllegalStateException("No tenant in the security context: refusing to access the database");
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
