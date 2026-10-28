package com.masterboxer.springbootmultitenancy.config;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * One shared connection pool for every tenant. On checkout the connection is
 * pointed at the tenant's schema (PostgreSQL search_path); on release it is
 * pointed back at "public" before returning to the pool. This replaces the old
 * "one HikariCP pool per tenant" design, so the number of DB connections no
 * longer grows with the number of tenants.
 */
@Component
public class SchemaPerTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final String DEFAULT_SCHEMA = "public";

    private final DataSource dataSource;
    private final TenantRegistry tenants;

    public SchemaPerTenantConnectionProvider(DataSource dataSource, TenantRegistry tenants) {
        this.dataSource = dataSource;
        this.tenants = tenants;
    }

    // Only used by Hibernate for bootstrap/metadata work, never for request traffic
    // (and disabled by hibernate.boot.allow_jdbc_metadata_access=false).
    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        if (!tenants.isAllowed(tenantIdentifier)) {
            throw new IllegalArgumentException("Unknown tenant: " + tenantIdentifier);
        }
        Connection connection = dataSource.getConnection();
        try {
            connection.setSchema(tenantIdentifier);
            return connection;
        } catch (SQLException | RuntimeException e) {
            connection.close();
            throw e;
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            connection.setSchema(DEFAULT_SCHEMA);
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnknownUnwrapTypeException(unwrapType);
    }
}
