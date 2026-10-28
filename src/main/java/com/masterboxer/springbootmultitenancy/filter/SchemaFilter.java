package com.masterboxer.springbootmultitenancy.filter;

import com.masterboxer.springbootmultitenancy.config.SchemaContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Set;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;

@Component
public class SchemaFilter extends OncePerRequestFilter {

    public static final String TENANT_SCHEMA_HEADER = "X-Tenant-Schema";
    private final Set<String> allowedSchemas;

    public SchemaFilter(@Value("${app.schemas}") Set<String> allowedSchemas) {
        this.allowedSchemas = allowedSchemas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String schema = request.getHeader(TENANT_SCHEMA_HEADER);
        if (schema == null || !allowedSchemas.contains(schema)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing or unknown " + TENANT_SCHEMA_HEADER);
            return;
        }
        try {
            SchemaContextHolder.setCurrentSchema(schema);
            filterChain.doFilter(request, response);
        } finally {
            SchemaContextHolder.clear();
        }
    }
}
