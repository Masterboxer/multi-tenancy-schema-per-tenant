# Multi-Tenancy: Schema per Tenant (Spring Boot + PostgreSQL)

A small Spring Boot project that demonstrates **schema-per-tenant multi-tenancy**: every tenant gets its own PostgreSQL schema inside a single database, and each request is routed to the right schema based on an HTTP header.

> Based on [Hogwai/spring-boot-multi-tenancy](https://github.com/Hogwai/spring-boot-multi-tenancy) (MIT). Extended as a learning project.

## Tech stack

- Java 25
- Spring Boot 3.5.x (Web, Data JPA)
- Hibernate 6
- PostgreSQL
- Gradle

## How it works

```
Client ──(X-Tenant-Schema: apple)──▶ SchemaFilter ──▶ SchemaContextHolder (ThreadLocal)
                                                              │
                                                              ▼
                                   SchemaRoutingDataSource.determineCurrentLookupKey()
                                                              │
                                                              ▼
                                         datasource for schema "apple" ──▶ PostgreSQL
```

1. `SchemaFilter` reads the `X-Tenant-Schema` header, checks it against the allowed list in `app.schemas`, and stores it in a `ThreadLocal` (`SchemaContextHolder`). Missing or unknown values get a `400 Bad Request`.
2. `SchemaRoutingDataSource` (a Spring `AbstractRoutingDataSource`) returns that value as the lookup key, so JPA talks to the matching tenant's schema.
3. The filter clears the `ThreadLocal` after every request, so tenants never leak between requests on the same thread.

The entity and repository code never mention a tenant. Tenant awareness lives in the filter and the routing datasource.

## Why the filter rejects unknown tenants

Spring's `AbstractRoutingDataSource` silently falls back to the default datasource when the lookup key is missing or unregistered. Without validation, a typo like `X-Tenant-Schema: doesnotexist` would read from the default schema (`public`) instead of failing. The whitelist check in `SchemaFilter` prevents that.

## Getting started

### 1. Create the database and tenant schemas

Each tenant needs its own schema, `customer` table, and `cust_seq` sequence. The table structure comes from the `Customer` entity (the app runs with auto-DDL off).

```sql
CREATE DATABASE "multi-tenancy";
\c "multi-tenancy"

CREATE SCHEMA apple;
CREATE TABLE apple.customer (
    id            BIGINT PRIMARY KEY,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    country       VARCHAR(255),
    address       VARCHAR(255),
    city          VARCHAR(255),
    creation_date TIMESTAMP,
    update_date   TIMESTAMP
);
CREATE SEQUENCE apple.cust_seq START WITH 1 INCREMENT BY 1;
```

Repeat for every tenant, or clone from an existing one:

```sql
CREATE SCHEMA google;
CREATE TABLE google.customer (LIKE apple.customer INCLUDING ALL);
CREATE SEQUENCE google.cust_seq START WITH 1 INCREMENT BY 1;
```

Do not keep a `customer` table or `cust_seq` sequence in `public`; tenants should fail loudly, not fall back to shared data.

### 2. Configure the app

Edit `src/main/resources/application.properties`:

```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/multi-tenancy
spring.datasource.username=postgres
spring.datasource.password=<your-password>

# Allowed tenants (must match the schema names in PostgreSQL)
app.schemas=apple,google,amazon
```

Adjust the host, port, and credentials for your PostgreSQL install. Don't commit real passwords.

### 3. Run

```bash
./gradlew bootRun        # macOS / Linux
.\gradlew bootRun        # Windows PowerShell
```

The API starts on `http://localhost:8081`.

## API

All requests must include the tenant header:

```
X-Tenant-Schema: <schema-name>
```

| Method | Endpoint                             | Description                                             |
| ------ | ------------------------------------ | ------------------------------------------------------- |
| `GET`  | `/customers/get`                     | List all customers of the tenant                        |
| `POST` | `/customers/create?customerNumber=N` | Insert `N` randomly generated customers into the tenant |

### Examples

On Windows PowerShell use `curl.exe` (plain `curl` is an alias for `Invoke-WebRequest`):

```powershell
curl.exe -X POST "http://localhost:8081/customers/create?customerNumber=10" -H "X-Tenant-Schema: apple"
curl.exe "http://localhost:8081/customers/get" -H "X-Tenant-Schema: apple"
curl.exe "http://localhost:8081/customers/get" -H "X-Tenant-Schema: google"
```

On macOS / Linux, use `curl` instead of `curl.exe`.

Each tenant has its own sequence, so ids start at 1 independently in every schema. Rows created for `apple` never show up for `google`.

## Adding a new tenant

1. Create the schema, table, and sequence in PostgreSQL (see step 1 above).
2. Add the schema name to `app.schemas`.
3. Restart the application. Tenants are registered at startup.

## Troubleshooting

| Symptom                                                                   | Likely cause                                                                                               |
| ------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `400 Bad Request`                                                         | Header missing, or the schema isn't listed in `app.schemas` (or the app wasn't restarted after editing it) |
| `relation "cust_seq" does not exist`                                      | The sequence is missing from that tenant's schema                                                          |
| `relation "customer" does not exist`                                      | The table is missing from that tenant's schema                                                             |
| `schema "x" does not exist`                                               | The schema is listed in `app.schemas` but wasn't created in PostgreSQL                                     |
| `0x81 in encoding "WIN1252" ... UTF8` when pasting SQL in psql on Windows | Non-ASCII characters in the pasted SQL; use ASCII-only values or run `\encoding WIN1252` first             |

## Multi-tenancy strategies at a glance

| Strategy                                                                     | Isolation | Cost    | This project |
| ---------------------------------------------------------------------------- | --------- | ------- | ------------ |
| Database per tenant                                                          | Strongest | Highest | no           |
| **Schema per tenant**                                                        | Strong    | Medium  | **yes**      |
| Shared tables with a tenant column (e.g. with PostgreSQL Row Level Security) | Weakest   | Lowest  | no           |

## Possible next steps

- Reject unknown tenants even earlier, and return a JSON error body instead of the default error page.
- Load tenants dynamically instead of requiring a restart.
- Manage tenant schemas with Flyway or Liquibase instead of manual SQL.
- Use a restricted PostgreSQL role for a genuinely read-only tenant.
- Replace the random-data endpoint with a proper JSON `POST /customers` endpoint.

## License

MIT. See [LICENSE](LICENSE). Original work copyright its author; see the upstream project linked above.
