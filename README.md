# Multi-Tenancy: Schema per Tenant (Spring Boot + PostgreSQL + JWT)

A small Spring Boot project that demonstrates **schema-per-tenant multi-tenancy**: every tenant gets its own PostgreSQL schema inside a single database. Users log in, receive a signed JWT, and every request is routed to the tenant schema named in that token. The tenant is **never taken from a client-supplied header**.

> Based on [Hogwai/spring-boot-multi-tenancy](https://github.com/Hogwai/spring-boot-multi-tenancy) (MIT). Extended as a learning project with JWT authentication, Hibernate's multi-tenancy support, a shared connection pool, DTOs and pagination.

## Tech stack

- Java 25
- Spring Boot 3.5.x (Web, Data JPA, Security, OAuth2 Resource Server, Validation)
- Hibernate 6.6 (schema-based multi-tenancy)
- PostgreSQL
- Gradle

## How it works

```
POST /auth/login {tenant, username, password}
        │  credentials checked server-side
        ▼
   signed JWT  (claims: sub, tenant, roles, exp)
        │
        ▼
GET /customers/get  +  Authorization: Bearer <jwt>
        │
        ▼
Spring Security: verify signature, expiry, issuer, known tenant, role
        │
        ▼
TenantIdentifierResolver  ── reads the "tenant" claim from the verified JWT
        │
        ▼
SchemaPerTenantConnectionProvider ── one shared Hikari pool;
        connection.setSchema("apple") on checkout, reset to "public" on release
        │
        ▼
PostgreSQL (schema "apple")
```

1. **Login.** `AuthController` checks the credentials for the requested tenant and issues a JWT. The `tenant` claim is decided by the server, so a user can only ever get a token for a tenant they belong to.
2. **Authentication.** Spring Security verifies every request's token (HS256 signature, expiry, issuer) and rejects tokens with a missing or unknown `tenant` claim.
3. **Tenant resolution.** `TenantIdentifierResolver` hands Hibernate the tenant from the verified token. If there is no tenant, the request fails with an exception; there is no default tenant and no fallback to `public`.
4. **Schema switching.** `SchemaPerTenantConnectionProvider` borrows a connection from a single shared pool and points it at the tenant's schema (`setSchema`). On release it is reset to `public` before going back to the pool.

The entity, repository and service code never mention a tenant. Tenant awareness lives in the security layer and the connection provider.

## Design decisions

| Decision                                                  | Why                                                                                                                                               |
| --------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| Tenant comes from the signed JWT, not a header            | A header is chosen by the client, so it proves nothing. A signed claim set by the server cannot be forged without the secret.                     |
| No default datasource / no fallback to `public`           | A missing tenant should fail loudly, not silently read shared data.                                                                               |
| One shared connection pool instead of one pool per tenant | Connection count stays fixed (`maximum-pool-size`) no matter how many tenants exist.                                                              |
| `ddl-auto=none`                                           | Hibernate would otherwise create tables in `public` at startup, when no tenant is set. Tenant schemas are created with SQL (or Flyway/Liquibase). |
| `hibernate.boot.allow_jdbc_metadata_access=false`         | No tenant exists at startup, so Hibernate must not open a connection just to inspect metadata.                                                    |
| DTOs (`CustomerRecord`) and `PageResponse`                | The API contract is independent of the JPA entity, and results are paged.                                                                         |
| CSRF disabled                                             | The API is stateless and uses bearer tokens, with no cookies or sessions.                                                                         |

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

# JWT signing secret: read from the environment, at least 32 characters
app.jwt.secret=${JWT_SECRET}
```

Adjust the host, port, and credentials for your PostgreSQL install. Don't commit real passwords or secrets.

**Set the JWT secret before starting.** The app refuses to start without it:

```bash
# macOS / Linux
export JWT_SECRET=$(openssl rand -base64 48)
```

```powershell
# Windows PowerShell (use any random string of 32+ characters)
$env:JWT_SECRET = "replace-with-a-long-random-string-of-32+-chars"
```

Anyone who knows this secret can mint tokens for any tenant, so treat it like a password.

### 3. Run

```bash
./gradlew bootRun        # macOS / Linux
.\gradlew bootRun        # Windows PowerShell
```

The API starts on `http://localhost:8081`.

## Authentication

### Demo users

The users below are defined in `application.properties` (`app.auth.users[...]`) with BCrypt-hashed passwords. **They are for demonstration only**; replace them with a real user store or identity provider.

| Tenant   | Username | Password             | Roles           |
| -------- | -------- | -------------------- | --------------- |
| `apple`  | `alice`  | `Apple-Admin-2026!`  | `USER`, `ADMIN` |
| `apple`  | `bob`    | `Apple-User-2026!`   | `USER`          |
| `google` | `carol`  | `Google-Admin-2026!` | `USER`, `ADMIN` |

### Login

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenant":"apple","username":"alice","password":"Apple-Admin-2026!"}'
```

Response:

```json
{ "accessToken": "<jwt>", "tokenType": "Bearer", "expiresInSeconds": 1800 }
```

Send the token on every other request as `Authorization: Bearer <jwt>`. Tokens expire after 30 minutes (`app.jwt.ttl`).

## API

| Method | Endpoint                             | Access            | Description                                          |
| ------ | ------------------------------------ | ----------------- | ---------------------------------------------------- |
| `POST` | `/auth/login`                        | public            | Exchange credentials for a JWT                       |
| `GET`  | `/customers/get?page=0&size=20`      | `USER` or `ADMIN` | Paged list of the tenant's customers (`size` 1-100)  |
| `POST` | `/customers/create?customerNumber=N` | `ADMIN`           | Insert `N` random customers (1-1000) into the tenant |

Paged response shape:

```json
{
  "content": [
    {
      "id": 1,
      "firstName": "...",
      "lastName": "...",
      "country": "...",
      "address": "...",
      "city": "...",
      "creationDate": "...",
      "updateDate": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 10,
  "totalPages": 1
}
```

### Examples

macOS / Linux (requires `jq`):

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login -H "Content-Type: application/json" \
  -d '{"tenant":"apple","username":"alice","password":"Apple-Admin-2026!"}' | jq -r .accessToken)

curl -X POST "http://localhost:8081/customers/create?customerNumber=10" -H "Authorization: Bearer $TOKEN"
curl "http://localhost:8081/customers/get?page=0&size=20" -H "Authorization: Bearer $TOKEN"
```

Windows PowerShell:

```powershell
$token = (Invoke-RestMethod -Method Post -Uri http://localhost:8081/auth/login `
  -ContentType "application/json" `
  -Body '{"tenant":"apple","username":"alice","password":"Apple-Admin-2026!"}').accessToken

Invoke-RestMethod -Method Post "http://localhost:8081/customers/create?customerNumber=10" -Headers @{Authorization="Bearer $token"}
Invoke-RestMethod "http://localhost:8081/customers/get" -Headers @{Authorization="Bearer $token"}
```

Each tenant has its own sequence, so ids start at 1 independently in every schema. Rows created for `apple` never show up for `google`.

## Verifying the security behaviour

These checks all pass against the demo data:

| Request                                                                            | Expected                                                    |
| ---------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| `GET /customers/get` with no token                                                 | `401`                                                       |
| `GET /customers/get` with a garbage token                                          | `401`                                                       |
| `POST /customers/create` as `bob` (USER only)                                      | `403`                                                       |
| `GET /customers/get?size=1000`                                                     | `400`                                                       |
| `POST /customers/create?customerNumber=0`                                          | `400`                                                       |
| Login with a wrong password                                                        | `401`                                                       |
| Login as `alice` against tenant `google`                                           | `401`                                                       |
| Alice's token with the payload edited to `"tenant":"google"` (signature unchanged) | `401`                                                       |
| Same `GET /customers/get` with Alice's token vs Carol's token                      | Different `totalElements` (each sees only their own schema) |

## Adding a new tenant

1. Create the schema, table, and sequence in PostgreSQL (see step 1 above).
2. Add the schema name to `app.schemas` (lowercase letters, digits and underscores only).
3. Add at least one user for the tenant under `app.auth.users[...]`.
4. Restart the application. Tenants are registered at startup.

## Troubleshooting

| Symptom                                                                   | Likely cause                                                                                                        |
| ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| App fails to start: `Could not resolve placeholder 'JWT_SECRET'`          | The `JWT_SECRET` environment variable is not set                                                                    |
| App fails to start: `app.jwt.secret ... must be at least 32 bytes long`   | The secret is too short                                                                                             |
| `401 Unauthorized`                                                        | Missing, expired, malformed or forged token, or the token's tenant is not in `app.schemas`                          |
| `403 Forbidden`                                                           | Valid token, but the user's role is not allowed (e.g. a `USER` calling `/customers/create`)                         |
| `400 Bad Request`                                                         | A parameter is out of range (`size` over 100, `customerNumber` outside 1-1000) or the login body is missing a field |
| `No tenant in the security context`                                       | A database call ran without a verified JWT (e.g. on a background thread)                                            |
| `relation "cust_seq" does not exist`                                      | The sequence is missing from that tenant's schema                                                                   |
| `relation "customer" does not exist`                                      | The table is missing from that tenant's schema                                                                      |
| `schema "x" does not exist`                                               | The schema is listed in `app.schemas` but wasn't created in PostgreSQL                                              |
| `0x81 in encoding "WIN1252" ... UTF8` when pasting SQL in psql on Windows | Non-ASCII characters in the pasted SQL; use ASCII-only values or run `\encoding WIN1252` first                      |

## Multi-tenancy strategies at a glance

| Strategy                                                                     | Isolation | Cost    | This project |
| ---------------------------------------------------------------------------- | --------- | ------- | ------------ |
| Database per tenant                                                          | Strongest | Highest | no           |
| **Schema per tenant**                                                        | Strong    | Medium  | **yes**      |
| Shared tables with a tenant column (e.g. with PostgreSQL Row Level Security) | Weakest   | Lowest  | no           |

## Known limitations

- **Demo users live in `application.properties`.** A real deployment needs a user store (for example a users table per tenant) or an external identity provider.
- **No login rate limiting, refresh tokens or token revocation.**
- **One database role for all tenants.** Isolation is enforced by the application, not by PostgreSQL. For stronger guarantees, use per-tenant roles or Row Level Security as a second layer.
- **Tenant context is thread-bound.** It comes from Spring Security's `SecurityContext`, so it is not inherited by `@Async` or `CompletableFuture` threads unless you propagate it.
- **Tenants are loaded at startup.** Adding one requires a restart.

## Possible next steps

- Load tenants dynamically instead of requiring a restart.
- Manage tenant schemas with Flyway or Liquibase instead of manual SQL.
- Back authentication with a database or an OIDC provider (the resource-server setup already supports switching the decoder).
- Return JSON error bodies with `@RestControllerAdvice`.
- Replace the random-data endpoint with a proper JSON `POST /customers` endpoint.
- Add integration tests (Testcontainers + `spring-security-test`) covering tenant isolation.

## License

MIT. See [LICENSE](LICENSE). Original work copyright its author; see the upstream project linked above.
