package com.masterboxer.springbootmultitenancy.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class SchemaRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return SchemaContextHolder.getCurrentSchema();
    }
}
