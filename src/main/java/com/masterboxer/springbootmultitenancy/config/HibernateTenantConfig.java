package com.masterboxer.springbootmultitenancy.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the tenant resolver and connection provider with Hibernate. Spring
 * Boot keeps auto-configuring the (single) DataSource from spring.datasource.*.
 */
@Configuration
public class HibernateTenantConfig {

    @Bean
    public HibernatePropertiesCustomizer multiTenancyCustomizer(SchemaPerTenantConnectionProvider connectionProvider,
            TenantIdentifierResolver tenantResolver) {
        return properties -> {
            properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
        };
    }
}
