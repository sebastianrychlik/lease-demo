# M4 — Persistence & Lease Domain

M4 introduces durable business persistence to LeaseDemo and, over its
sub-milestones, the LeaseDemo domain model itself (customers, lease
applications, etc.).

This is a **living document** for the entire M4 milestone. Each
sub-milestone extends this same file instead of creating a new
document. Sections are explicitly marked as **IMPLEMENTED** or
**PLANNED / FUTURE ARCHITECTURE** — never assume a "PLANNED" section
already exists in code.

Current implemented scope: **M4.0, M4.1, M4.1.1, M4.1.2, M4.1.3, M4.2, M4.3,
and M4.4** (persistence foundation, the initial `Customer` domain, OpenAPI/
Swagger UI documentation, JWT-bound customer identity, a local-only mock
Customer data seeder, the frontend design-system foundation, the
role-aware Angular Admin/Customer application shell, and the first real
ADMIN business feature — server-side paged/sorted/searched Customer
management).

Future planned scope: the `LeaseApplication` domain, beyond M4.4.

---

## M4.0 — PostgreSQL Persistence Foundation — IMPLEMENTED

### 1. Objective

M4.0 wires the application to a real relational database before any
business domain exists, so that later milestones can focus purely on
domain modeling instead of infrastructure plumbing. It introduces:

- **PostgreSQL** — the durable relational database engine.
- **Spring Data JPA** — repository abstraction over JPA.
- **Hibernate** — the JPA provider that maps entities to tables.
- **PostgreSQL JDBC Driver** — the wire-protocol driver Hibernate/JDBC use to talk to PostgreSQL.
- **HikariCP** — the connection pool Spring Boot wires in by default.
- **Flyway** — versioned schema migration tooling.

M4.0 intentionally contains **no `@Entity` classes and no business
tables**. It only proves that the full persistence stack (pool →
driver → database, and migration → validation) boots correctly.

### 2. Current Local Architecture

```
Angular
    |
    v
Spring Boot
    |
    | JDBC
    v
HikariCP
    |
    v
PostgreSQL JDBC Driver
    |
    v
localhost:5433
    |
    | Docker port mapping
    v
container:5432
    |
    v
PostgreSQL 17
    |
    v
lease_demo
    |
    v
Docker Volume: lease-demo-postgres-data
```

Host port **5433** is used because this development machine already
runs a native Windows PostgreSQL 16 service permanently bound to
`localhost:5432`. This is a **local development environment decision
only** — it has no bearing on production, where PostgreSQL is not
addressed via a hardcoded host port at all (see §8).

### 3. PostgreSQL Docker Persistence

| Item              | Value                      |
|-------------------|-----------------------------|
| Container         | `lease-demo-postgres`      |
| Volume            | `lease-demo-postgres-data` |
| Database          | `lease_demo`               |
| Local host port   | `5433`                     |
| Container port    | `5432`                     |
| Local user        | `leasedemo`                |

**Docker container ≠ database data.** The container is a disposable
process; the actual PostgreSQL data files live in the named volume
`lease-demo-postgres-data`, mounted at
`/var/lib/postgresql/data` inside the container.

Consequences:

- container stop → data survives
- container restart → data survives
- container recreation with the same volume attached → data survives
- volume deletion → data is lost

This Docker volume is a **local development convenience**, not the
production persistence architecture (see §11 for production).

### 4. Spring Data JPA vs Hibernate vs JDBC

| Layer                    | Responsibility |
|---------------------------|----------------|
| Spring Data JPA           | Repository abstractions (e.g. `JpaRepository`), reduces persistence boilerplate. |
| JPA                        | The Java persistence *specification* — defines entity mapping and persistence concepts, not an implementation. |
| Hibernate                  | The JPA *implementation* Spring Boot uses by default. |
| JDBC                       | The lower-level Java database communication API used underneath Hibernate. |
| PostgreSQL JDBC Driver     | Implements the PostgreSQL wire protocol for JDBC. |

Actual flow, now that `Customer` exists (M4.1):

```
Customer Java object
        |
        v
CustomerRepository (JpaRepository<Customer, UUID>)
        |
        v
JPA
        |
        v
Hibernate
        |
        v
JDBC
        |
        v
PostgreSQL Driver
        |
        v
PostgreSQL
```

### 5. HikariCP

HikariCP is the JDBC **connection pool** Spring Boot auto-configures
by default. It is not a database, not an ORM, and not a cache.

Without pooling, every unit of work pays the cost of opening/closing a
physical connection:

```
HTTP request
    |
    v
open DB connection
    |
    v
authenticate/connect
    |
    v
SQL
    |
    v
close connection
```

With HikariCP, Spring Boot maintains a pool of already-authenticated,
reusable connections:

```
Spring Boot
    |
    v
pool of reusable DB connections
    |
    +-- connection
    +-- connection
    +-- connection
```

Requests borrow a connection from the pool and return it afterward,
avoiding the overhead of establishing a new physical connection per
operation.

### 6. Flyway

Flyway is used so that database schema evolution is **versioned,
repeatable, and reviewable** the same way application code is.

- Migration location: `backend/src/main/resources/db/migration/`
- Naming convention: `V1__description.sql`, `V2__description.sql`, `V3__description.sql`, ...
- Flyway records every applied migration in the `flyway_schema_history` table.

Conceptual relationship:

```
Repository:              Database (flyway_schema_history):
V1                        V1  success
V2                        V2  success
V3                        V3  success
V4  <- pending            (not yet present)
```

On startup, Flyway compares the migration files present on the
classpath against `flyway_schema_history`, detects any pending
migrations (e.g. `V4`), executes them in order, and records the
result.

**M4.0 state at the time PostgreSQL wiring was first verified:**
`flyway_schema_history` existed with 0 migrations applied and no
business tables — M4.0 intentionally shipped with an empty
`db/migration` directory to prove the persistence stack alone.

**Current state (as of M4.1):** `V1__create_customer_table.sql` has
been applied — see §10 and the M4.1 section below for the verified,
current schema.

### 7. Flyway vs Hibernate Schema Ownership

**Architectural invariant: Flyway owns database schema evolution.**
Hibernate only maps Java entities onto a schema that already exists.

`application-local.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

We deliberately avoid `create`, `create-drop`, and `update` because
those modes let Hibernate silently mutate the schema based on entity
state — acceptable for throwaway prototypes, unacceptable for an
application whose schema history must be auditable and reproducible
across environments.

In M4.0, `ddl-auto: validate` succeeded trivially (zero `@Entity`
classes). Since M4.1, it is a **meaningful check**: Hibernate compares
the `Customer` entity's mapped columns/types against the actual
`customers` table created by `V1__create_customer_table.sql`, and
application startup fails if they diverge — see §19 for the full
startup sequence.

### 8. Local vs Production Datasource

**Local:**

```
Spring Boot
    |
    v
jdbc:postgresql://localhost:5433/lease_demo
    |
    v
Docker PostgreSQL
```

**Production:**

```
Spring Boot
    |
    | JDBC/TLS
    v
Managed PostgreSQL
```

Production datasource values are environment-driven, never hardcoded:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Production must **not** assume host port `5433` — that value is a
local artifact of a port conflict on this development machine and has
no meaning outside it. Credentials are never committed to source
control because doing so would permanently leak them into git history,
give anyone with repository read access production database access,
and prevent rotating credentials without a code change.

### 9. Test Strategy

`backend/src/test/resources/application-local.yml` overrides the
`local` profile for the Maven test classpath (test resources take
precedence over main resources). It excludes persistence
auto-configuration entirely:

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
      - org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
```

Rationale: forcing every `@SpringBootTest`/`@WebMvcTest` in the general
suite to require a live PostgreSQL server would add an environment
dependency (and CI fragility) for little test value. Since M4.1 added
the first repository (`CustomerRepository`), the exclusion list also
disables `JpaRepositoriesAutoConfiguration`, and
`LeaseDemoApplicationTests` mocks `CustomerRepository` with `@MockBean`
purely to satisfy `CustomerService`'s constructor dependency during
this configuration smoke test — no real repository behavior is
exercised by that test.

**Current limitation:** there is still no repository-level test that
exercises `CustomerRepository` against a real (or embedded) database
engine — persistence behavior (constraints, unique violations, actual
SQL) has so far only been verified **manually** against the local
Docker PostgreSQL instance (see the table below and §23). Likely
future approach (not implemented yet): `@DataJpaTest` combined with
Testcontainers-backed PostgreSQL, so repository tests run against a
real PostgreSQL engine without depending on a developer's local Docker
container being up.

### 10. Current Database State (verified)

| Item                        | Value |
|------------------------------|-------|
| Database                     | `lease_demo` |
| Schema                       | `public` |
| Current tables                | `flyway_schema_history`, `customers` |
| Flyway migrations applied     | `V1` (`create customer table`) — SUCCESS |
| Business tables               | `customers` |
| Hibernate-created tables       | none (Hibernate only validates; Flyway created `customers`) |

---

## Production Data Architecture — Future Direction

> **PLANNED / NOT IMPLEMENTED IN M4.0.** Everything in this section
> (§11–§16) describes architectural intent for a future production
> deployment. None of it exists in the codebase today.

### 11. Managed PostgreSQL

Production PostgreSQL should be a durable **managed database service**
(e.g. a cloud provider's managed PostgreSQL offering) rather than data
stored inside an ephemeral application container. Application
containers are stateless and disposable by design — they should be
freely restartable/replaceable without any risk of data loss. Database
infrastructure is stateful and needs its own lifecycle, backup, and
availability guarantees, independent of application deployments.

### 12. Encryption

Future considerations for production, not implemented in M4.0:

- **Encryption in transit** — TLS between the application and
  PostgreSQL.
- **Encryption at rest** — encryption of the database's underlying
  storage.
- **Encrypted backups.**
- **KMS-managed keys** for the above.
- **Application-level field encryption** for particularly sensitive
  business fields (e.g. PESEL), independent of storage-level
  encryption.

Password hashing (one-way, for verifying credentials) and field
encryption (reversible, for protecting data that must later be read
back) are different concepts serving different purposes and must not
be conflated.

### 13. Data Residency

Regulatory/geographic data residency requirements must consider more
than just the primary database. Business data can also exist in:

- the primary database
- replicas
- backups
- database logs
- exported datasets

Production infrastructure must be configured so that whatever
residency rules apply are satisfied across the *entire* persistence
lifecycle, not only the primary database's location. (No specific
legal/compliance claim is made here — this is an architectural
awareness note.)

### 14. Primary and Replicas

- **Primary** — the authoritative database instance that accepts
  writes.
- **Replica** — a database instance that receives replicated changes
  from the primary.

Typical purposes: high availability, failover, disaster recovery, and
read scaling.

**Replication lag** is the delay between a write committing on the
primary and that write becoming visible on a replica. Because of this
lag, a read immediately following a write can return **stale data**
if routed to a replica that has not yet caught up.

In managed PostgreSQL, replication is typically handled by the
database/cloud infrastructure itself. Application-level read/write
routing across primary/replicas is a separate, orthogonal concern.
**No multiple datasources or read/write routing are implemented in
LeaseDemo.**

### 15. Backups and Point-In-Time Recovery

**A backup is not a replica.** A replica mirrors live database state
and will faithfully replicate unwanted changes (e.g. accidental
deletes) just as quickly as wanted ones. A backup provides a
historical recovery point independent of the live state.

**Point-In-Time Recovery (PITR)**, conceptually, uses the database's
write-ahead log (WAL) plus periodic base backups to reconstruct the
database as it existed at an arbitrary past timestamp — useful for
recovering from operational mistakes or corruption.

This is future production architecture; nothing here is implemented.

### 16. Keycloak Database Separation

**Current state:** Keycloak's local development persistence remains
separate and unchanged by M4.0. M4.0 does **not** migrate Keycloak to
PostgreSQL.

Future conceptual production architecture (not implemented):

```
Managed PostgreSQL instance / cluster
        |
        +-- lease_demo database
        |      credentials: LeaseDemo application
        |
        +-- keycloak database
               credentials: Keycloak
```

LeaseDemo business tables and Keycloak's internal tables should remain
logically separated (distinct databases/credentials) even if they
eventually share the same physical PostgreSQL cluster, so that a
compromise or operational issue in one system's credentials/schema
cannot directly reach the other's data.

### 17. Local PostgreSQL Scripts

| Script | State | Purpose |
|--------|-------|---------|
| `scripts/postgres-start.sh` | **Implemented** | Creates the `lease-demo-postgres-data` volume and `lease-demo-postgres` container if missing (host `5433` → container `5432`, image `postgres:17`), or starts the existing container if stopped. |
| `scripts/postgres-stop.sh` | **Implemented** | Stops the container without touching the volume; explicitly logs that data has not been removed. |
| `scripts/postgres-reset.sh` | **Placeholder — not implemented** | No content yet. |
| `scripts/postgres-seed-local-data.sh` | **Placeholder — not implemented** | No content yet. |

`postgres-start.sh` maps `host 5433 -> container 5432` and mounts the
persistent volume `lease-demo-postgres-data`, matching the contract
documented in §2–§3.

---

## M4 Roadmap

| Sub-milestone | Scope | Status |
|----------------|-------|--------|
| M4.0 | PostgreSQL Persistence Foundation | **IMPLEMENTED** |
| M4.1 | Customer Domain | **IMPLEMENTED** |
| M4.2 | Frontend Design System Foundation | **IMPLEMENTED** |
| M4.3 | Role-Aware Angular Application Shell | **IMPLEMENTED** |

(Note: this table originally listed M4.2 as "LeaseApplication Domain"
before that scope was actually built; M4.2 was instead used for the
frontend design-system foundation, and M4.3 for the role-aware
Admin/Customer shell — see their dedicated sections below. The
`LeaseApplication` domain remains future planned scope.)

---

## M4.1 — Customer Domain — IMPLEMENTED

### 1. Objective

M4.1 introduces the first LeaseDemo business entity, `Customer`, together
with application-level field encryption for its PESEL (Polish national
identification number) — the first field in the codebase requiring
protection beyond ordinary storage-level encryption.

Note on history: M4.1 was implemented, then went through one
**correction pass** (schema/security fixes) before this document was
finalized. Everything below describes only the **final, corrected
state** — earlier intermediate mistakes are not documented, since they
no longer exist in the code.

### 2. Customer Table (`V1__create_customer_table.sql`)

Actual current schema, with real column widths from the migration:

| Column | Type | Constraint | Purpose |
|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY` | Surrogate key (see §18 — UUID). |
| `keycloak_user_id` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | Keycloak JWT `sub` — the stable link to the authenticated identity (see §3 below). |
| `first_name` | `VARCHAR(255)` | `NOT NULL` | Customer PII. |
| `last_name` | `VARCHAR(255)` | `NOT NULL` | Customer PII. |
| `email` | `VARCHAR(255)` | `NOT NULL` | Contact PII — **not** the identity key (see below). |
| `phone_number` | `VARCHAR(30)` | nullable | Contact PII, optional. |
| `date_of_birth` | `DATE` | `NOT NULL` | Business data; cross-validated against the PESEL (§13). |
| `gender` | `VARCHAR(10)` | `NOT NULL` | Stored as the `Gender` enum name (`MALE`/`FEMALE`); cross-validated against the PESEL. |
| `pesel_encrypted` | `TEXT` | `NOT NULL` | AES-256-GCM ciphertext of the PESEL — see §6. |
| `pesel_lookup` | `VARCHAR(64)` | `NOT NULL`, `UNIQUE` | HMAC-SHA-256 lookup digest of the PESEL — see §6. |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | Backend-owned audit timestamp (§17). |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | Backend-owned audit timestamp (§17). |

**There is no plaintext `pesel` column.** The raw PESEL is never
persisted in any column, form, or backup path that this application
controls.

**Why `email` is not the identity key:** a person can change their
email address at any time; if `Customer` were looked up or linked by
email, that link would silently break (or worse, silently attach to
the wrong account) whenever email changes. `keycloak_user_id` (the JWT
`sub`) is assigned once by Keycloak and never changes for the lifetime
of that account — it is the stable identity link (see §3).

Migration history note: because M4.1 had not yet been committed when
its correction pass happened, the original `V1` migration file was
edited in place (rather than adding a `V2`), and the disposable local
`lease_demo` database's `customers`/`flyway_schema_history` tables were
dropped and recreated by re-running Flyway. No other database, schema,
or Docker volume was touched. This in-place-edit approach is only
valid **before** a migration has shipped/been committed — once `V1` is
committed and could exist in another environment, any further schema
change must be a new `V2`, never an edit to `V1` (see §19).

### 3. Keycloak ↔ Customer Identity Model

```
Keycloak
   |
authenticates user (owns credentials/passwords)
   |
issues JWT access token
   |
token claim: sub  (stable, unique, assigned once)
   |
   v
customers.keycloak_user_id
```

| Concern | Owner |
|---|---|
| Authentication (login, password) | **Keycloak** |
| Credential storage/hashing | **Keycloak** |
| Roles (`CUSTOMER`/`ADVISOR`/`ADMIN`) | **Keycloak** (`realm_access.roles` in the JWT) |
| Business/customer profile data | **LeaseDemo `Customer`** |

- LeaseDemo never stores a password for a customer — Keycloak is the
  single source of truth for authentication.
- LeaseDemo never duplicates Keycloak roles onto `Customer` — role
  checks are done from the JWT (`SecurityConfig` /
  `KeycloakRealmRoleConverter`), not from a `Customer` column.
- `email` is business contact data, not an identity key (see §2).

**IMPLEMENTED (M4.1.2):** automatic extraction of the JWT `sub` claim
into `keycloak_user_id` during customer creation:

```
validated JWT
   |
   v
sub
   |
   v
Customer.keycloakUserId
   |
   v
customers.keycloak_user_id
```

`CustomerController#createCustomer` obtains the authenticated JWT via
`@AuthenticationPrincipal Jwt jwt` (Spring Security has already
validated the token's signature/issuer/expiry before the controller
runs) and reads `jwt.getSubject()`. This value is passed to
`CustomerService.createCustomer(String keycloakUserId,
CustomerCreateRequest request)` as a separate, trusted parameter — it
is never read from the request body.

**Trust boundary:**

| Source | Trust level | Carried by |
|---|---|---|
| JWT `sub` claim | **Trusted** — authenticated by Spring Security/Keycloak | `CustomerController` → `CustomerService(keycloakUserId, ...)` |
| `CustomerCreateRequest` (name, DOB, gender, PESEL, email, phone) | **Untrusted** — client-supplied business data | validated by Bean Validation + `PeselValidator` |

`CustomerCreateRequest` deliberately has **no `keycloakUserId` field**
— a client cannot choose, override, or spoof another user's identity
by putting a different value in the request body, because there is no
such field to put it in.

If (unexpectedly) the authenticated JWT lacks a usable subject,
`CustomerService` fails fast with `IllegalStateException` rather than
persisting a `Customer` with a null/blank `keycloak_user_id` — this
should never happen for a request that already passed JWT
authentication, so no bespoke exception/status is introduced for it.

**Duplicate identity handling:** `CustomerService` checks
`CustomerRepository.existsByKeycloakUserId(...)` early and throws
`DuplicateCustomerException` (mapped to **409 Conflict**) as a
friendly, fast failure. The `customers.keycloak_user_id` **UNIQUE**
database constraint remains in place and is the authoritative,
concurrency-safe guarantee — the service-level check alone cannot
prevent a race between two concurrent requests for the same subject;
only the database constraint can.

### 4. PII and Data Classification

**PII (Personally Identifiable Information)** is any data that can
identify a specific natural person, alone or combined with other data.

| `Customer` field | PII? | Notes |
|---|---|---|
| `firstName` / `lastName` | Yes | Direct identifiers. |
| `dateOfBirth` | Yes | Direct identifier, also sensitive in combination with name. |
| `gender` | Yes | Personal attribute. |
| `email` | Yes | Direct contact identifier. |
| `phoneNumber` | Yes | Direct contact identifier. |
| PESEL | Yes — and **more sensitive** | A single Polish national identifier that, by itself, can uniquely identify a person and encodes their date of birth and gender. |

**Why PESEL gets stronger, application-level protection than the other
PII columns:** PESEL is a national identifier with a **small,
structured, predictable value space** (11 digits, checksum, encoded
DOB/gender — see §13). A leaked PESEL is far more dangerous than a
leaked first name: it can be used for identity-fraud purposes specific
to the Polish administrative/financial system, and its structure makes
brute-force/dictionary-style attacks against a naive hash meaningfully
easier (see §8). No other `Customer` column has this specific risk
profile today.

**M4.1 does NOT application-encrypt every PII column** — only PESEL.
The other PII fields (`firstName`, `lastName`, `email`, `phoneNumber`,
`dateOfBirth`, `gender`) are stored as plain columns and rely on
**defense in depth** instead of field-level encryption:

- HTTPS/TLS in transit (§5A)
- authentication/authorization (only authenticated requests reach
  `/api/**` — see `SecurityConfig`)
- database/storage encryption at rest — **FUTURE, production only** (§5B, §12)
- access minimization (least privilege on who/what can query the DB)
- safe logging (§16)
- encrypted backups — **FUTURE, production only**
- other infrastructure controls — **FUTURE, production only**

None of the "FUTURE" items above are deployed today; they are the
intended production direction, not a current guarantee.

### 5. Three Different Encryption/Protection Layers

A recurring interview point: these three concepts protect *different*
things and are not interchangeable.

**A. Encryption in transit (HTTPS/TLS)**

```
Browser  --- HTTPS/TLS --->  Spring Boot
```

- A developer can still see `{"pesel":"..."}` in their **own** browser
  DevTools — DevTools inspects the request *before* TLS encrypts it on
  the way out / *after* TLS decrypts it on the way in. TLS never hides
  data from the two legitimate endpoints of the connection.
- A **passive network sniffer** (e.g. on shared Wi-Fi) should see only
  opaque encrypted TLS bytes, not the JSON plaintext — provided HTTPS
  is correctly configured and neither endpoint/device is compromised.

**B. Encryption at rest (FUTURE — production only, not implemented today)**

Encrypting the PostgreSQL storage volume/backups so raw disk or
backup-media access does not expose plaintext. This protects the
**storage/infrastructure layer** — it does **not**, by itself, stop the
application (or anyone with a valid, sufficiently privileged database
connection) from reading plaintext columns; the database still
decrypts transparently for authorized queries.

**C. Application-level field encryption (implemented — PESEL only)**

```
plaintext PESEL
      |
      v
AES-256-GCM
      |
      v
pesel_encrypted
      |
      v
PostgreSQL
```

PESEL is encrypted **before** it is ever handed to Hibernate/JDBC — the
database only ever stores/sees ciphertext for this field. This is the
only layer that also protects PESEL from someone with legitimate read
access to the `customers` table but no access to the AES key.

### 6. PESEL Security Architecture

```
                         PESEL
                           |
                 validation/parsing (§13)
                           |
              +------------+------------+
              |                         |
              v                         v
        AES-256-GCM                HMAC-SHA-256
        random nonce                keyed/deterministic
              |                         |
              v                         v
      pesel_encrypted              pesel_lookup
              |                         |
              +------------+------------+
                           |
                           v
                       PostgreSQL
```

**`pesel_encrypted`** — purpose: authorized recovery of the original
PESEL when genuinely required.

- Reversible with the AES key (that's the point — it's encryption, not
  hashing).
- AES-256-GCM, fresh random 96-bit IV/nonce per encryption, 128-bit
  authentication tag.
- Stored as `TEXT` containing `Base64(IV || ciphertext ||
  authentication tag)` — **not `BYTEA`**; this is the agreed storage
  representation for this project.
- Because the IV is random per call, **encrypting the same PESEL twice
  produces different stored ciphertext**.

**`pesel_lookup`** — purpose: deterministic equality lookup and
uniqueness checking without decrypting every stored PESEL.

- HMAC-SHA-256, deterministic for the same PESEL + the same key, keyed
  (see §8 for why a plain hash is not used).
- Non-reversible in normal operation (no known way to recover the
  PESEL from the digest without the key and brute-forcing candidates).
- Stored as a lowercase-hex `VARCHAR(64)` with a `UNIQUE` database
  constraint.

### 7. Why Both Values Are Required

**Why can't `pesel_encrypted` be used for lookup directly?** AES-GCM
solves confidentiality/recovery, but *intentionally* produces different
ciphertext each time (fresh random nonce). So this does **not** work:

```
WHERE pesel_encrypted = encrypt(inputPesel)   -- ALWAYS FALSE
```

— a new encryption of the same input PESEL produces different
ciphertext than what's already stored, so a direct equality check can
never match.

**HMAC solves deterministic lookup instead:**

```
input PESEL --> HMAC key --> lookup value --> WHERE pesel_lookup = ?
```

Because HMAC-SHA-256 with a fixed key is deterministic, hashing the
same PESEL twice always produces the same digest — so `pesel_lookup`
can be indexed and compared with an ordinary `WHERE` clause, while
`pesel_encrypted` remains the (non-comparable, but recoverable)
ciphertext.

### 8. Why HMAC Instead of Plain SHA-256

LeaseDemo does **not** use `SHA-256(PESEL)` for the lookup column.

PESEL has only 11 digits and a known, public structure (checksum
formula, encoded date-of-birth, encoded gender — see §13). An attacker
who steals a table of plain `SHA-256(PESEL)` hashes could generate
candidate PESEL values **offline** (respecting the checksum/date rules
to narrow the search space) and compare their SHA-256 hashes against
the stolen ones, with no need to touch LeaseDemo's systems again.

With `HMAC-SHA-256(secretKey, PESEL)`, the attacker additionally needs
the secret HMAC key to reproduce the stored lookup values — knowing
the PESEL structure alone is no longer enough.

This is **not** a claim that HMAC makes PESEL mathematically
unbreakable; it is a meaningful **defense-in-depth improvement** over a
plain hash, conditional on the HMAC key itself staying secret (§10).

### 9. AES-GCM Details

- **AES** — the symmetric block cipher LeaseDemo uses for `pesel_encrypted`.
- **GCM (Galois/Counter Mode)** — an AES mode of operation providing
  **authenticated encryption**: confidentiality *and*
  integrity/authenticity of the ciphertext, in one operation.
- **256-bit key** — `AesGcmEncryptionService` requires exactly 32
  decoded bytes.
- **96-bit nonce/IV** — the standard recommended nonce size for GCM;
  generated fresh per encryption via `SecureRandom`.
- **128-bit authentication tag** — appended after the ciphertext,
  verified on decryption.

**What "authenticated" buys you:** if stored ciphertext is tampered
with (even one bit), authentication-tag verification fails and
`Cipher.doFinal(...)` throws during decryption — corrupted data is
rejected rather than silently decrypted into garbage.

**Why nonce reuse is dangerous:** reusing the same (key, nonce) pair
for two different AES-GCM encryptions breaks GCM's confidentiality and
authentication guarantees. This is exactly why LeaseDemo generates a
fresh `SecureRandom` 96-bit nonce for **every** `encrypt(...)` call,
rather than reusing one nonce per key.

### 10. Key Separation

LeaseDemo uses **two separate keys**, never one key for both purposes:

| Env var | Used by | Purpose |
|---|---|---|
| `CRYPTO_AES_KEY` | `AesGcmEncryptionService` | Encryption/decryption of `pesel_encrypted`. |
| `CRYPTO_HMAC_KEY` | `HmacLookupHashService` | Deterministic derivation of `pesel_lookup`. |

Reusing one cryptographic key across two different algorithms/purposes
is a well-known anti-pattern — it couples unrelated concerns (e.g.
rotating the lookup key would also break decryption of existing data
if it were the same key).

**Fail-fast configuration validation** — `AesGcmEncryptionService` and
`HmacLookupHashService` validate their configured keys at construction
time (Spring bean creation, i.e. **application startup**) and throw
`IllegalStateException` if:

- the key is missing/blank,
- the key is not valid Base64, or
- the decoded key length is wrong (AES: not exactly 32 bytes / 256
  bits; HMAC: fewer than 32 bytes — the same 256-bit minimum is
  enforced for symmetry and clarity in this project).

The application refuses to start rather than failing unpredictably on
the first customer request. No key material is ever included in these
exception messages — only the property name and observed byte length.

### 11. Local Key Management — LOCAL DEVELOPMENT

**No fallback/default key values are committed to source control**,
including in `application-local.yml`:

```yaml
application:
  crypto:
    aes-key: ${CRYPTO_AES_KEY}
    hmac-key: ${CRYPTO_HMAC_KEY}
```

#### M4.1.3 CORRECTION — persistent local crypto keys

Earlier iterations of `scripts/deploy-local.sh` generated **ephemeral,
session-only** keys with `openssl rand -base64 32` whenever
`CRYPTO_AES_KEY`/`CRYPTO_HMAC_KEY` were not already present in the
shell environment. This was flagged above as acceptable only for
disposable data — and once the local PostgreSQL database became
**persistent** (surviving backend restarts, populated via
`scripts/postgres-seed-local-data.sh`), it turned into an active bug:

- restarting the backend generated new random keys;
- existing `pesel_encrypted` values could no longer be decrypted with
  the new AES key;
- the same PESEL now produced a **different** `pesel_lookup` under the
  new HMAC key, breaking deterministic duplicate-detection and
  `UNIQUE(pesel_lookup)` correctness across restarts;
- the seeder script ran as a **separate OS process**, so it could
  never see keys `export`-ed only inside `deploy-local.sh`'s shell —
  making the inconsistency immediately visible.

**Fix:** a small, gitignored, root-level file, **`.env.local`**, now
holds persistent LOCAL-ONLY key material:

```
CRYPTO_AES_KEY=<Base64 32-byte key>
CRYPTO_HMAC_KEY=<Base64 32-byte key>
```

A shared shell helper, `scripts/lib/local-crypto-keys.sh`
(`load_local_crypto_keys`), is `source`d by both
`scripts/deploy-local.sh` and `scripts/postgres-seed-local-data.sh`
before Spring Boot starts. It:

- **first run:** if `.env.local` does not exist, generates one AES key
  and one independent HMAC key with `openssl rand -base64 32` each,
  writes them to `.env.local`, and prints only a safe informational
  message (`Created persistent local crypto keys in .env.local`) —
  never the key values;
- **subsequent runs:** if `.env.local` already exists, loads the
  existing values **without regenerating them**, so the same keys are
  reused by every local process (backend and seeder alike) across
  restarts;
- **validates** after loading that both variables are present,
  non-blank, valid Base64, that the decoded AES key is exactly 32
  bytes, and the decoded HMAC key is at least 32 bytes — mirroring the
  same fail-fast checks `AesGcmEncryptionService`/
  `HmacLookupHashService` perform in Java (§10);
- **fails fast** (non-zero exit, clear error message, no
  regeneration) if `.env.local` exists but is missing an entry or
  contains invalid Base64/wrong-length key material. Silently
  regenerating in that situation could make existing encrypted
  database rows permanently unreadable, so the helper deliberately
  refuses and asks the developer to fix or recover the file instead.

`.env.local` is listed in `.gitignore` (verified with
`git check-ignore .env.local`) and is never printed, logged, or
committed. An optional `.env.local.example` documents the expected
shape using **placeholders only** — never real generated key values.

`application-local.yml` is unchanged by this correction — it still
references `${CRYPTO_AES_KEY}`/`${CRYPTO_HMAC_KEY}` with no hardcoded
defaults; only the *source* of those environment variables when
running locally has changed, from "regenerated every run" to
"generated once, persisted in `.env.local`, reused thereafter."

IMPLEMENTED by this correction:
- persistent local development crypto keys shared by
  `deploy-local.sh` and `postgres-seed-local-data.sh`.

FUTURE / NOT covered by this correction:
- production Secret Manager/KMS integration (see §12 — unchanged and
  still not implemented);
- formal cryptographic key rotation / re-encryption tooling;
- production backup/recovery procedures for key material.

This remains a **local-development-only** mechanism and has no effect
on the production configuration/model described in §12.

### 12. Production Key Management — FUTURE / NOT IMPLEMENTED

```
Spring Boot
    |
    v
Secret Manager / Vault / KMS   <-- FUTURE, not built
    |
    v
cryptographic key management
    |
    v
PESEL crypto services
```

- Production keys are never committed to Git (already true today via
  `${CRYPTO_AES_KEY}`/`${CRYPTO_HMAC_KEY}`).
- Environment variables are an **interface/configuration mechanism**,
  not necessarily the ultimate secret store — in production they would
  typically be populated from a secret manager/KMS at deploy/runtime,
  not typed in by hand.
- A production secret manager/KMS should control **access** to the
  keys (who/what can read them), not just their storage.
- **Key rotation** must be designed deliberately:
  - rotating the AES key requires a re-encryption/key-versioning
    strategy for existing `pesel_encrypted` values (old ciphertext
    can't be decrypted with a new key without also tracking which key
    version encrypted it),
  - rotating the HMAC key changes every future `pesel_lookup` value,
    which affects both uniqueness enforcement and lookup correctness
    against existing rows.

None of this (Secret Manager, Vault, KMS, or a rotation mechanism) is
implemented today — LeaseDemo currently relies solely on
environment-variable-supplied keys in every environment, including
production.

### 13. PESEL Validation

The implemented validation pipeline (`PeselValidator`):

```
PESEL input
    |
11 numeric digits
    |
checksum
    |
date decode
    |
gender decode
    |
compare declared DOB
    |
compare declared gender
    |
valid
```

- **Checksum weights** (applied to the first 10 digits): `1, 3, 7, 9,
  1, 3, 7, 9, 1, 3`. The 11th digit must equal `(10 - (sum % 10)) %
  10`.
- **Century/month encoding** — the month digits encode both the month
  and century via an offset:

  | Month offset | Century |
  |---|---|
  | 0 | 1900s |
  | 20 | 2000s |
  | 40 | 2100s |
  | 60 | 2200s |
  | 80 | 1800s |

- **Gender digit** — the 10th PESEL digit (1-based): even → `FEMALE`,
  odd → `MALE`.
- **Cross-field validation** — the PESEL-encoded date of birth and
  gender must match the *declared* `dateOfBirth`/`gender` fields on the
  request. Example (illustrative, not a real PESEL/person):
  - PESEL encodes `2002-07-08`, declared DOB is `2002-07-09` → **REJECT**.
  - PESEL encodes `MALE`, declared gender is `FEMALE` → **REJECT**.

LeaseDemo does **not** silently overwrite the declared values with
whatever the PESEL encodes (or vice versa) — the goal is to verify
**consistency** between two independently supplied pieces of business
data, not to pick a "winner".

### 14. Plaintext PESEL Lifetime

Plaintext PESEL **may temporarily exist** in:

- the Angular input field / in-memory form state,
- the `CustomerCreateRequest` DTO,
- Spring Boot process memory during `PeselValidator` validation,
- Spring Boot process memory during AES/HMAC computation.

Plaintext PESEL **must never** be persisted or exposed in:

- a PostgreSQL plaintext column (there is none — §2),
- application logs or metrics,
- exception messages,
- URLs or query parameters,
- browser `localStorage`/`sessionStorage`,
- `CustomerResponse`,
- `Customer`/DTO `toString()` output,
- audit log payloads.

**TLS does not mean plaintext never exists in application memory** —
TLS protects the transport *between* endpoints; once a request reaches
the Spring Boot process, the JSON body (including plaintext PESEL) is
decrypted and exists as ordinary Java objects/strings in memory for as
long as normal request processing requires.

### 15. REST Security Boundary

A `POST /api/customers` endpoint (`CustomerController`/`CustomerService`)
already exists, ahead of M4.1's originally planned scope. This section
documents its **actual** security boundary as implemented today,
without expanding it further:

- `CustomerCreateRequest` **may** contain plaintext PESEL — the backend
  must receive it in order to validate and protect it; there is no
  other way to accept it as input.
- `CustomerResponse` must **not** contain the raw PESEL,
  `peselEncrypted`, or `peselLookup` — verified: it does not.
- Encrypted/hashed PESEL values should also generally stay internal:
  **ciphertext is protected data, not public application data** — even
  though it isn't the raw PESEL, exposing it externally adds
  unnecessary attack surface (e.g. facilitates offline HMAC-key
  brute-force attempts against `pesel_lookup` if it ever leaked
  through a response).
- No log statement or exception message in `PeselValidator`,
  `CustomerService`, `AesGcmEncryptionService`, `HmacLookupHashService`,
  or `GlobalExceptionHandler` includes the PESEL value — only generic,
  non-identifying descriptions (e.g. "PESEL checksum is invalid").
- The PESEL is submitted only in the JSON request body of a `POST`
  request; it never appears in a URL or query parameter.
- The endpoint requires an authenticated JWT (`/api/**` →
  `authenticated()` in `SecurityConfig`), consistent with the rest of
  the API.
- No other `Customer` endpoint (read, update, delete, search) exists in
  the code today — none is documented here.

### 16. Safe Logging Policy

**Never log:**

- PESEL plaintext
- `pesel_encrypted`
- `pesel_lookup`
- the AES key
- the HMAC key

Avoid whole-object logging of sensitive request/entity objects (e.g.
logging the entire `CustomerCreateRequest` or `Customer` object) —
a future field addition could silently start leaking sensitive data
through an existing, unreviewed log statement.

Even the HMAC digest/ciphertext should not casually appear in logs:
logs frequently have **different retention, replication, and access
policies** than the primary database — a value acceptably protected in
PostgreSQL may be copied into a log aggregation system with broader
access or longer retention than intended.

### 17. Customer Timestamp Lifecycle

`Customer` uses JPA lifecycle callbacks, not client-supplied values:

```java
@PrePersist
void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
}

@PreUpdate
void onUpdate() {
    this.updatedAt = Instant.now();
}
```

- `createdAt` — set once, when the entity is first persisted.
- `updatedAt` — set on initial persistence, then refreshed on every
  subsequent update.
- **The backend owns both timestamps** — a client cannot supply or
  override them (there is no corresponding field on
  `CustomerCreateRequest`).
- `TIMESTAMPTZ` (PostgreSQL) + `Instant` (Java) is the appropriate pair
  for backend audit timestamps: `TIMESTAMPTZ` stores an unambiguous
  point in time (normalized internally, independent of session time
  zone), and `Instant` is Java's corresponding time-zone-independent
  instant type — avoiding the ambiguity of naive local date-times
  across services/servers in different zones.

### 18. UUID

`Customer.id` is a `UUID`, generated via `GenerationType.UUID`.

Interview rationale:

- Globally unique without needing a centrally coordinated numeric
  sequence — useful if `Customer` rows are ever created across
  multiple services/instances.
- Carries no business meaning (unlike an incrementing integer, which
  might hint at total customer count or registration order).
- Safer to expose than sequential integer IDs **from an enumeration
  standpoint** (harder to guess `id-1`/`id+1`) — but a UUID is **not,
  by itself, an authorization mechanism**: knowing a valid UUID does
  not imply the caller is authorized to access that resource; the API
  must still enforce authorization independently.
- Useful in distributed systems where IDs may need to be generated
  without a round-trip to a central authority.

### 19. Flyway vs Hibernate — Startup Sequence

**Flyway owns database schema evolution. Hibernate/JPA only maps Java
objects onto a schema that already exists** (`ddl-auto: validate`).

```
Spring Boot starts
      |
      v
DataSource / HikariCP
      |
      v
Flyway
      |
      v
apply missing migrations
      |
      v
Hibernate
      |
      v
validate entity <-> schema
      |
      v
application ready
```

**Why not `ddl-auto=update`?** Because production schema changes must
be explicit, versioned, reviewable, and repeatable across environments
— `update` lets Hibernate silently infer and apply schema changes from
entity state, which is unauditable and unsafe outside a throwaway
prototype.

**`flyway_schema_history`:** Flyway's own bookkeeping table. A `V1
SUCCESS` row means Flyway knows `V1` has already been applied and will
**not** execute it again on subsequent startups — it will only look for
and apply any newer, still-pending migration (`V2`, `V3`, ...).

### 20. Local PostgreSQL Persistence

**Container lifecycle ≠ volume lifecycle** — see §3 of the M4.0 section
above for the full container/volume contract
(`lease-demo-postgres` / `lease-demo-postgres-data`). In short:

- the `lease-demo-postgres` **container** is disposable — it can be
  stopped, removed, and recreated,
- the actual PostgreSQL data files live in the named **volume**
  `lease-demo-postgres-data`, mounted into the container,
- therefore "docker container gone" does **not** imply "database data
  gone" — only removing the volume itself does that.

This differs fundamentally from placing a production PostgreSQL
database **inside** a stateless application container (e.g. a Cloud
Run instance): stateless application containers are expected to be
freely killed/replaced/scaled at any time with zero data-loss risk,
which is only safe if the container holds no durable state at all.
Running a real database inside such a container would mean losing data
on every restart/redeploy/scale event — this is why production
PostgreSQL must be a separately managed, stateful service (§21), never
bundled into the stateless application container.

### 21. Production Database Architecture — FUTURE

> Extends the "Production Data Architecture" discussion started in the
> M4.0 section above (§11–§16). **Nothing in this section is deployed
> today.**

```
                         REGION / DATA RESIDENCY
                                  |
                  +---------------+---------------+
                  |                               |
                  v                               v
          Spring Boot instances          Managed PostgreSQL
                                                |
                                      +---------+---------+
                                      |                   |
                                   Primary            Read replica(s)
                                      |
                                   writes
```

- **Primary** — the authoritative instance that accepts reads/writes.
- **Read replica(s)** — kept in sync by the managed database platform
  (synchronously or asynchronously depending on the
  product/configuration) — not by LeaseDemo application code.
- The application **may** use separate read/write datasource routing
  in a future architecture if read scaling is needed — **not
  implemented today**; LeaseDemo currently uses a single datasource.
- **Backups are not replicas.** Replicas exist for
  availability/read-scaling and faithfully replicate mistakes just as
  quickly as good writes. Backups/PITR exist to recover from deletion,
  corruption, or human error — a fundamentally different purpose (see
  §15 of the M4.0 section for the fuller backup/PITR discussion).

Additional production design considerations (**not currently
deployed**): encryption at rest, TLS connections to the database,
backup encryption, PITR, restricted network access, least-privilege
database credentials, and regional/data-residency requirements (§22).

### 22. Data Residency

Financial systems in particular may require:

- the primary database hosted in an approved region,
- backups stored in approved locations,
- controlled/approved replication geography,
- data-residency consideration for Keycloak/identity data as well as
  business data,
- data-residency consideration for logging/observability data.

Selecting, e.g., a Polish/EU region for these components is an
**infrastructure/compliance deployment decision** — it is not
something JPA, Hibernate, or application code implements or enforces
by itself. No specific legal/compliance claim is made here; this is an
architectural awareness note, and no such region selection is
implemented today.

### 23. What Is Explicitly Not Implemented Yet (M4.1)

- Automatic extraction of `keycloak_user_id` from the authenticated JWT's
  `sub` claim — it is currently a plain request field (§3).
- Any additional `Customer` REST operations beyond create (read, update,
  delete, search).
- Production-grade key management (secret manager/KMS, rotation) — see §12.
- Repository-level tests against a real database engine — see §24.

### 24. Current M4.1 Testing

Verified state: `mvn clean verify` → **41 tests, 41 passed, BUILD
SUCCESS.**

| Area | Coverage |
|---|---|
| PESEL checksum | valid/invalid checksum cases |
| PESEL DOB decoding + cross-check | 1900s/2000s century offsets, DOB mismatch rejected |
| PESEL gender decoding + cross-check | gender mismatch rejected |
| PESEL malformed input | wrong length, non-digit characters |
| AES-256-GCM | encrypt/decrypt round-trip; repeated encryption of the same input produces different ciphertext |
| HMAC-SHA-256 lookup | deterministic for same input; different inputs produce different digests; correct digest length/format |
| Crypto fail-fast validation | missing key, invalid Base64, wrong key length (both AES and HMAC) |
| `CustomerService` | valid creation flow, PESEL-validation failure path, duplicate-PESEL rejection path (mocked collaborators) |
| Existing security tests | `KeycloakRealmRoleConverterTest`, `SecurityIntegrationTest` (unaffected by M4.1) |

**Not currently tested:** GCM authentication-tag tampering/rejection is
not exercised by an explicit unit test today (only round-trip and
different-ciphertext behavior are). There is also no Testcontainers
integration test — see the current limitation noted in §9 (Test
Strategy) and §23.

**Verified manually against the live local PostgreSQL instance**
(`localhost:5433`, container `lease-demo-postgres`) — not via an
automated test:

- Flyway `V1` → `SUCCESS`.
- Hibernate `ddl-auto: validate` → SUCCESS (application started
  cleanly against the migrated schema).
- `customers` table exists with all 12 agreed columns.
- `UNIQUE` constraint present on `keycloak_user_id`.
- `UNIQUE` constraint present on `pesel_lookup`.
- No plaintext `pesel` column exists.

### 25. Security Threat / Control Table

| Threat / Risk | LeaseDemo control |
|---|---|
| Network sniffing | TLS/HTTPS (§5A) |
| Database storage theft | Storage encryption — **FUTURE** — plus PESEL application-level encryption (implemented) |
| Database dump containing PESEL | AES-GCM ciphertext instead of plaintext (§6) |
| Offline guessing of a deterministic PESEL lookup | Keyed HMAC instead of plain SHA-256 (§8) |
| Ciphertext tampering | GCM authentication tag (§9) |
| Duplicate PESEL | `UNIQUE` constraint on `pesel_lookup` (§2); also checked in `CustomerService` before insert |
| Wrong DOB/gender submitted | PESEL cross-field validation (§13) |
| Leaked logs | Sensitive-data logging policy (§16) |
| **Compromised application with key access** | **Application-level encryption alone cannot fully protect data in this scenario** — least privilege, key-access restrictions, and KMS/secret-manager controls are required (§12, FUTURE) |

The last row matters: application-level encryption protects PESEL from
someone who can read the database but not the AES/HMAC keys. It does
**not** protect PESEL from an attacker who has fully compromised the
running application process itself (which necessarily has access to
the keys in memory to do its job).

### 26. Defense in Depth

```
                    PESEL protection

                    Authorization
                         |
                       TLS
                         |
              Application validation
                         |
                  AES-256-GCM
                         |
                 HMAC lookup
                         |
             PostgreSQL constraints
                         |
             Encryption at rest
                   [FUTURE]
                         |
             Backup encryption
                   [FUTURE]
                         |
                 IAM / KMS
                   [FUTURE]
```

No single mechanism in this stack makes the system secure by itself.
Security comes from **multiple independent layers**, so that a failure
or gap in one layer (e.g. a misconfigured TLS termination, or a leaked
log line) does not automatically mean full compromise of PESEL data —
the remaining layers still apply.

---

## M4.1.1 — OpenAPI / Swagger UI — IMPLEMENTED

### 1. Objective

Add interactive REST API documentation to the backend using
**springdoc-openapi**, without changing Customer business behavior or
weakening `/api/**` security. Purely developer/interview tooling.

### 2. What Was Added

- Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`
  (springdoc-openapi 2.x targets Spring Boot 3 / Jakarta EE 9+; 2.6.0
  is compatible with the project's Spring Boot 3.3.4 / Spring
  Framework 6.1.x without upgrading Boot or Java).
- `config/OpenApiConfig.java` — registers OpenAPI metadata (title
  `LeaseDemo API`, description, version `0.1.0`) and an HTTP Bearer
  security scheme named `bearerAuth` (`type: http`, `scheme: bearer`,
  `bearerFormat: JWT`).
- `SecurityConfig` — whitelisted `/swagger-ui/**` and
  `/v3/api-docs/**` as `permitAll()`, placed before the actuator/API
  rules and before the final `anyRequest().denyAll()`. No other rule
  changed; `/api/**` remains `authenticated()`.
- `CustomerController` — annotated with `@SecurityRequirement(name =
  "bearerAuth")` (so only real, existing operations are documented as
  protected) plus a concise `@Operation`/`@ApiResponses` for
  `POST /api/customers` (201 / 400 / 401 / 409).
- `CustomerCreateRequest.pesel` — annotated with `@Schema(description =
  ...)` explaining that PESEL is accepted only in the request body and
  never persisted in plaintext. No example PESEL value was added.

### 3. URLs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 4. Public Documentation vs. Protected API

Making the documentation endpoints public does **not** make the
LeaseDemo API public:

```
/swagger-ui/**   -> permitAll   (documentation UI)
/v3/api-docs/**  -> permitAll   (OpenAPI JSON)
/api/health      -> permitAll   (unchanged)
/api/**          -> authenticated   (unchanged — includes /api/customers)
```

`GET /api/health` still returns 200 without a JWT; `POST
/api/customers` still returns 401 without a JWT. Verified locally
against the running backend (Postgres + Keycloak containers already
up), and via `SecurityIntegrationTest` (`@WebMvcTest` + real
`SecurityConfig`, mocked `JwtDecoder`).

### 5. Bearer JWT Authorize Support

Swagger UI displays an **Authorize** button backed by the `bearerAuth`
HTTP bearer scheme. A developer pastes a raw Keycloak access token
(no `Bearer ` prefix needed) and Swagger sends
`Authorization: Bearer <token>` on subsequent "Try it out" calls —
standard OpenAPI HTTP-bearer behavior. This is a manual
developer/testing convenience; Angular remains the only component
performing the real Keycloak Authorization Code + PKCE browser login.
No OAuth2 flow is configured inside Swagger.

### 6. Verified (local)

- `GET /swagger-ui/index.html` → 200, loads without authentication.
- `GET /v3/api-docs` → 200, valid OpenAPI JSON containing
  `info.title = "LeaseDemo API"` and
  `components.securitySchemes.bearerAuth` (`type: http`, `scheme:
  bearer`, `bearerFormat: JWT`).
- `paths./api/customers.post.security` = `[{ "bearerAuth": [] }]`.
- `POST /api/customers` without a JWT → 401.
- `GET /api/health` without a JWT → 200.
- `components.schemas.CustomerResponse` /
  `CustomerCreateRequest` contain no `peselEncrypted`,
  `peselLookup`, or crypto key fields.
- `mvn clean verify`: **43/43 tests pass**, `BUILD SUCCESS`.

### 7. Production Note

For this demo milestone, Swagger UI and the OpenAPI document are
publicly readable. In a real financial production environment,
whether to expose interactive API documentation at all — versus
restricting it to internal networks, requiring auth, or disabling it
— is an explicit deployment/security decision made per environment.
No profile-specific toggle was added here to keep this milestone
focused on demo/interview usability.

### 8. Interview Notes — M4.1.1

1. **What is OpenAPI?** A language-agnostic specification (JSON/YAML)
   describing a REST API's paths, request/response schemas, and
   security requirements.
2. **What is Swagger UI?** An interactive web UI that renders an
   OpenAPI document, letting a developer browse and execute API calls
   from the browser.
3. **Is Swagger the same as OpenAPI?** No — OpenAPI is the
   specification/format; "Swagger" (now Swagger UI/Swagger tools) is
   tooling built around that specification. springdoc-openapi
   generates the OpenAPI document and serves Swagger UI.
4. **Why can Swagger UI be public while the API remains protected?**
   Documentation describes the API's shape; it grants no access by
   itself. `SecurityConfig` still enforces `authenticated()` on
   `/api/**` independently of whether its description is public.
5. **How does Swagger send a JWT?** Via the configured `bearerAuth`
   HTTP bearer scheme — pasting a token into "Authorize" makes Swagger
   attach `Authorization: Bearer <token>` to subsequent requests.
6. **Does Swagger authenticate the user with Keycloak here?** No —
   Swagger only lets a developer manually paste an already-obtained
   access token. The real Authorization Code + PKCE login flow is
   performed by Angular, not Swagger.
7. **Why might Swagger be disabled/restricted in production?** Public
   API documentation can reveal internal endpoint shapes/fields to
   attackers, aiding reconnaissance; some organizations restrict it to
   internal networks or disable it in production entirely.
8. **Difference between API documentation security and API endpoint
   security?** Documentation security controls who can *read the
   description* of the API (e.g. Swagger UI access); endpoint security
   controls who can *actually call* the API (Spring Security's
   authentication/authorization rules) — the two are independent, and
   this milestone deliberately keeps the latter unchanged.

---

## Interview Notes

1. **What is JPA?** A Java specification (API) that defines how Java
   objects are mapped to relational database tables and how they are
   persisted/queried — it is a spec, not an implementation.
2. **What is Hibernate?** The JPA implementation used by Spring Boot;
   it actually performs entity-to-table mapping and generates SQL.
3. **What is Spring Data JPA?** A Spring abstraction layer on top of
   JPA that provides repository interfaces (e.g. `JpaRepository`) to
   remove persistence boilerplate.
4. **What is JDBC?** The low-level Java API for connecting to and
   executing SQL against relational databases; Hibernate uses it
   underneath.
5. **What is HikariCP?** The JDBC connection pool Spring Boot
   auto-configures by default, managing a pool of reusable database
   connections.
6. **Why use a connection pool?** Opening a physical DB connection is
   expensive; pooling lets requests borrow/return already-established
   connections instead of paying that cost per operation.
7. **What is Flyway?** A database migration tool that applies
   versioned, ordered SQL scripts and tracks which ones have run.
8. **Why use Flyway instead of Hibernate `ddl-auto=update`?** Flyway
   gives reviewable, versioned, repeatable schema changes; `update`
   lets Hibernate silently and implicitly mutate production schema
   based on entity state, which is unsafe and unauditable.
9. **Where does Flyway remember executed migrations?** In the
   `flyway_schema_history` table it creates in the target schema.
10. **What is a PostgreSQL schema?** A namespace inside a database
    that groups tables/objects (PostgreSQL's default schema is
    `public`).
11. **Difference between a database and a schema?** A database is a
    top-level, fully isolated storage/connection unit; a schema is a
    namespace *within* a database that can hold multiple sets of
    tables.
12. **Why is PostgreSQL a separate service from Spring Boot?** So the
    application and database have independent lifecycles, scaling,
    and failure domains — the app container is stateless/disposable,
    the database is stateful and durable.
13. **Why does the Docker volume matter?** Because container
    filesystems are ephemeral; the volume is where actual PostgreSQL
    data files persist across container stop/restart/recreation.
14. **What is a primary database?** The authoritative instance that
    accepts writes.
15. **What is a replica?** A database instance that continuously
    receives and applies changes replicated from the primary.
16. **Who synchronizes managed PostgreSQL replicas?** Typically the
    managed database/cloud provider's infrastructure, not the
    application.
17. **What is replication lag?** The delay between a write committing
    on the primary and that change becoming visible on a replica.
18. **Why can replica reads be stale?** Because of replication lag —
    a read routed to a replica right after a write may not yet reflect
    that write.
19. **Difference between a replica and a backup?** A replica mirrors
    live state (including unwanted changes); a backup is a historical
    snapshot usable for recovery independent of current live state.
20. **What is Point-In-Time Recovery?** The ability to restore a
    database to its exact state at an arbitrary past timestamp,
    conceptually using WAL/logs plus base backups.
21. **Why must production DB credentials not live in `application.yml`?**
    Committing them to source control permanently exposes them in git
    history to anyone with repo access and prevents easy rotation;
    they must be injected via environment variables/secret stores.
22. **What does encryption in transit mean?** Encrypting data (e.g.
    via TLS) while it travels over the network between the application
    and the database.
23. **What does encryption at rest mean?** Encrypting the data as it
    is stored on disk, so raw storage access does not expose plaintext
    data.
24. **What does data residency include beyond the primary database?**
    Replicas, backups, database logs, and exported datasets — anywhere
    the data may physically reside.
25. **Why should Keycloak and LeaseDemo use logically separated
    databases?** So that a credential compromise or operational issue
    in one system cannot directly reach the other system's schema/data,
    even if they share the same physical database cluster.
26. **What does `JpaRepository` provide?** CRUD methods
    (`save`, `findById`, `existsBy...`, etc.) and query-derivation from
    method names (e.g. `findByPeselLookup`) without hand-written SQL.
27. **What is a database migration?** A versioned, ordered script that
    applies an incremental, reviewable change to a database schema.
28. **Why use UUID for `Customer.id`?** No central sequence
    coordination needed, no business meaning, and it's harder to
    enumerate than a sequential integer — though it is not itself an
    authorization control.
29. **What is PII?** Personally Identifiable Information — any data
    that can identify a specific natural person, alone or combined with
    other data.
30. **Is PESEL PII?** Yes, and more sensitive than most other
    `Customer` PII fields, because it's a single national identifier
    with a small, predictable, structured value space.
31. **What is encryption in transit?** Encrypting data while it moves
    over the network, e.g. HTTPS/TLS between the browser and Spring
    Boot.
32. **What is encryption at rest?** Encrypting stored data on disk
    (e.g. the database volume/backups) so raw storage-media access
    doesn't expose plaintext — **future/production only** in LeaseDemo.
33. **What is application-level encryption?** Encrypting a specific
    sensitive field (PESEL) inside the application, before it is ever
    handed to the database — independent of transport or storage
    encryption.
34. **Why can plaintext JSON still be visible in browser DevTools when
    HTTPS is used?** DevTools inspects requests before TLS encrypts
    them outbound and after TLS decrypts them inbound — TLS never
    hides data from the two legitimate endpoints of the connection.
35. **Can someone sniffing Wi-Fi normally read HTTPS JSON payloads?**
    No — assuming HTTPS/TLS is correctly configured and neither
    endpoint is compromised, a passive sniffer sees only encrypted
    bytes.
36. **Why encrypt PESEL at application level if PostgreSQL storage is
    encrypted?** Storage encryption doesn't stop the application (or a
    sufficiently privileged DB connection) from reading plaintext
    columns — application-level encryption is the only layer that also
    protects against that case.
37. **Why store `pesel_encrypted`?** To allow authorized recovery of
    the original PESEL when genuinely required, without ever storing
    it in plaintext.
38. **Why store `pesel_lookup`?** To allow deterministic equality
    lookup/uniqueness checking without decrypting every stored PESEL.
39. **Why do we need both?** AES-GCM ciphertext is non-deterministic
    (random nonce), so it can't be used for equality lookup; HMAC is
    deterministic but non-reversible, so it can't recover the original
    value. Each solves exactly one of the two problems.
40. **Why can't AES-GCM ciphertext be used directly for equality
    lookup?** Because a fresh random nonce is used per encryption, so
    encrypting the same PESEL twice produces different ciphertext —
    `WHERE pesel_encrypted = encrypt(input)` never matches.
41. **Why HMAC-SHA-256 instead of plain SHA-256?** PESEL has a small,
    known structure (checksum, encoded DOB/gender), so plain
    `SHA-256(PESEL)` hashes could be attacked with offline candidate
    generation; HMAC additionally requires the secret key to reproduce
    the digest.
42. **What is the difference between encryption and hashing?**
    Encryption is reversible with the right key (confidentiality +
    recovery); hashing/HMAC is one-way and used for verification or
    lookup, not recovery.
43. **What is HMAC?** A keyed hashing construction that combines a
    secret key with the input to produce a deterministic digest —
    reproducible only by someone with the key.
44. **Is HMAC encryption?** No — it's a one-way keyed digest, not a
    reversible cipher; you cannot "decrypt" an HMAC output.
45. **Can HMAC normally be reversed?** No — recovering the input from
    an HMAC digest is not feasible without brute-forcing candidate
    inputs.
46. **Why use AES-GCM?** It provides authenticated encryption —
    confidentiality and integrity/authenticity in a single, well
    understood, widely supported construction.
47. **What does authenticated encryption mean?** The cipher provides
    both confidentiality (ciphertext hides the plaintext) and
    integrity/authenticity (tampering with the ciphertext is
    detectable).
48. **What does the GCM authentication tag protect?** It lets
    decryption detect whether the ciphertext was tampered with —
    verification fails and decryption is rejected if it was.
49. **Why does AES-GCM need a fresh nonce?** Reusing a (key, nonce)
    pair across two different encryptions breaks GCM's confidentiality
    and authentication guarantees.
50. **Why are two encryptions of the same PESEL different?** Because a
    fresh random 96-bit nonce is generated per encryption call and
    prepended to the stored value.
51. **Why use separate AES and HMAC keys?** They serve different
    purposes (recovery vs. deterministic lookup); reusing one key for
    both couples unrelated concerns and complicates rotation.
52. **Why fail application startup for an invalid crypto key?** So a
    misconfiguration is caught immediately and clearly at deploy time,
    instead of failing unpredictably on the first customer request.
53. **Why must cryptographic keys not be committed to Git?** Git
    history is effectively permanent and often broadly readable —
    committed keys can't be un-leaked and must be rotated immediately
    if ever committed.
54. **Where should production keys live?** Outside source control,
    injected via environment variables that are themselves populated
    from a secret manager/KMS at deploy/runtime (future — not
    implemented today).
55. **What is key rotation?** Replacing a cryptographic key with a new
    one over time, typically to limit the impact of a potential key
    compromise.
56. **Why is HMAC key rotation particularly relevant to
    `pesel_lookup`?** Rotating the HMAC key changes every future
    `pesel_lookup` digest, which affects both uniqueness enforcement
    and lookup correctness against existing rows.
57. **Why is ciphertext still sensitive?** It's still protected data,
    not public application data — unnecessarily exposing it increases
    attack surface even though it isn't the raw PESEL.
58. **Why should encrypted PESEL not normally appear in REST
    responses?** There's no legitimate client need for it, and
    exposing it externally adds unnecessary attack surface for no
    benefit.
59. **Why is plaintext PESEL allowed in `CustomerCreateRequest`?**
    The backend must receive it in order to validate and protect it —
    there's no other way to accept it as input.
60. **Why must PESEL not be logged?** Logs often have different
    retention, replication, and access policies than the primary
    database, so a value acceptably protected in PostgreSQL could be
    unintentionally exposed more broadly through logs.
61. **Why shouldn't PESEL be put in a URL?** URLs are commonly logged
    by proxies, browsers, and web servers outside the application's
    own logging policy, and may appear in browser history.
62. **Why link `Customer` to Keycloak using JWT `sub`?** `sub` is
    assigned once and never changes for the lifetime of the account —
    unlike email, it's a stable identity link.
63. **Why not use email as the stable identity key?** Email addresses
    can change; using email as the link would break (or misattach) the
    identity link whenever it does.
64. **Why doesn't `Customer` store a password?** Keycloak is the sole
    source of truth for authentication and credential storage —
    duplicating credentials would create a second, likely
    inconsistent, source of truth.
65. **Why doesn't `Customer` duplicate Keycloak roles?** Roles are
    read from the JWT (`realm_access.roles` via
    `KeycloakRealmRoleConverter`) at request time — duplicating them
    onto `Customer` risks staleness/inconsistency with Keycloak.
66. **What are `@PrePersist` and `@PreUpdate`?** JPA entity lifecycle
    callback annotations — methods so annotated run automatically
    immediately before the entity is first inserted / before it is
    updated, respectively.
67. **Why use `Instant`/`TIMESTAMPTZ` for audit timestamps?**
    `TIMESTAMPTZ` stores an unambiguous point in time regardless of
    session time zone, and `Instant` is Java's matching
    time-zone-independent type — avoiding ambiguity across
    servers/services in different zones.
68. **What is a `UNIQUE` database constraint?** A constraint that
    guarantees no two rows can share the same value in the constrained
    column(s), enforced by the database itself.
69. **Why is `pesel_lookup` `UNIQUE`?** To prevent two `Customer` rows
    for the same PESEL, enforced at the database level regardless of
    what the application layer already checked.
70. **What happens when two users try to insert the same PESEL
    concurrently?** Both may pass the application-level
    `existsByPeselLookup` check before either has committed; the
    database's `UNIQUE` constraint on `pesel_lookup` is what actually
    prevents both inserts from succeeding.
71. **Why should the database constraint still exist if the service
    checks duplicates first?** The application-level check alone is
    vulnerable to a race condition between "check" and "insert" — only
    a database constraint is atomic and authoritative.
72. **What is a race condition in a check-then-insert flow?** A timing
    window where two concurrent operations both pass a "does this
    already exist?" check before either has committed its insert,
    allowing both to proceed and violate an invariant the check was
    meant to enforce.
73. **What is defense in depth?** A security strategy that layers
    multiple independent controls, so a failure in any single layer
    doesn't automatically mean full compromise.
74. **Does application-level encryption protect data if the
    application runtime and keys are fully compromised?** No — the
    running application necessarily has access to the keys to do its
    job, so a full application-runtime compromise defeats
    application-level encryption too; other controls (least privilege,
    KMS access restrictions) are needed for that scenario.
75. **What is the difference between a database replica and a
    backup?** A replica mirrors live state (including unwanted
    changes) for availability/read-scaling; a backup is a historical,
    independent recovery point.
76. **What is a primary database?** The authoritative instance that
    accepts writes.
77. **Who keeps managed PostgreSQL replicas synchronized?** The
    managed database/cloud platform's own infrastructure, not
    application code.
78. **Why might an application have separate read/write
    datasources?** To route read-heavy traffic to replicas for
    scaling, while all writes go to the primary — not implemented in
    LeaseDemo today.
79. **What is PITR?** Point-In-Time Recovery — restoring a database to
    its exact state at an arbitrary past timestamp, conceptually using
    write-ahead logs plus periodic base backups.
80. **Why shouldn't production PostgreSQL live inside a stateless
    Cloud Run container?** Stateless containers are expected to be
    freely killed/replaced with zero data-loss risk; a real database
    needs its own durable, stateful lifecycle independent of
    application deploys.
81. **Why does a Docker volume preserve local PostgreSQL data when the
    container is recreated?** Because the actual data files live in
    the named volume, mounted into the container — the container
    filesystem itself is disposable, the volume is not.
82. **What does data residency mean?** Requirements/constraints on
    which physical region(s) data (including replicas, backups, and
    logs) is allowed to reside in.
83. **Why can database region matter in financial systems?**
    Regulatory/compliance requirements may mandate that customer data
    (and its backups/replicas) stay within an approved
    jurisdiction/region.
84. **Why use Testcontainers for PostgreSQL tests instead of H2?**
    Testcontainers runs tests against a real PostgreSQL engine (same
    behavior/SQL dialect/constraints as production), whereas H2's
    PostgreSQL-compatibility mode can diverge on edge cases. **Not
    currently implemented in LeaseDemo** — this is a planned future
    testing improvement (see §9, §24).
85. **Why should `keycloakUserId` not come from the request body
    (M4.1.2)?** A request body is client-controlled input; if the
    client could supply its own `keycloakUserId`, it could impersonate
    or overwrite another authenticated user's identity. The identity
    must instead come exclusively from the already-validated JWT.
86. **Why use JWT `sub` instead of an email claim for the identity
    binding?** `sub` is assigned once by Keycloak and never changes;
    email is mutable business/contact data (see §62–63) and is
    deliberately kept out of the identity-binding decision in this
    milestone (no scope expansion to cross-check/derive email from the
    JWT).
87. **Does the backend trust JWT claims blindly?** No — Spring
    Security's OAuth2 resource server support validates the token's
    signature (against Keycloak's published JWK set), issuer, and
    expiry *before* the request ever reaches `CustomerController`. Only
    after that validation does the controller read `jwt.getSubject()`.
88. **Who validates the JWT before the controller sees it?** Spring
    Security's `oauth2ResourceServer(...).jwt(...)` filter chain
    (configured in `SecurityConfig`), using the `JwtDecoder`
    auto-configured from `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
89. **Why retain `UNIQUE(keycloak_user_id)` if the service already
    checks `existsByKeycloakUserId` first?** Same rationale as
    `pesel_lookup` (see §70–71): the service-level check is a friendly,
    early failure for the common case, but it is vulnerable to a
    check-then-insert race between two concurrent requests for the same
    subject. Only the database `UNIQUE` constraint is atomic and
    therefore authoritative.

---

## M4.1.3 — Local Mock Customer Seeder — IMPLEMENTED

### 1. Objective

M4.1.3 adds a **LOCAL-DEVELOPMENT-ONLY** mechanism to populate the local
PostgreSQL database with realistic, synthetic `Customer` records for
development/demo purposes — without weakening any part of the production
trust boundary established in M4.1/M4.1.2.

```
scripts/postgres-seed-local-data.sh (mvn -Plocal-seed ...)
        |
        v
scripts/generate-customer-mock-data.py   (synthetic business data + valid PESEL)
        |
        | JSON (tmp/mock-customers.json — gitignored, disposable)
        v
CustomerSeedRunner (compiled only under -Plocal-seed; local profile + explicit flag)
        |
        v
CustomerService.createCustomer(mock-user-NNNNNN, request)
        |
        +--> PeselValidator
        +--> AesGcmEncryptionService
        +--> HmacLookupHashService
        |
        v
CustomerRepository -> PostgreSQL
```

> **Security correction (post-implementation):** the initial version of
> this milestone placed `CustomerSeedRunner`/`CustomerSeedProperties`/
> `CustomerSeedRecord` under the normal `src/main/java` source set,
> protected only by `@Profile("local")` +
> `application.seed.customers.enabled=true`. An audit of the packaged
> artifact (`jar tf target/lease-demo-0.1.0.jar | grep -i seed`) confirmed
> that all three `.class` files were present in the **normal production
> JAR** even though they could never be *activated* under a production
> Spring profile. Runtime-only guards are a real safeguard, but they are
> not the strongest available one for sensitive development tooling, so
> §5 below adds **build-time/artifact-level isolation** as an additional,
> stronger layer. See §5a.

### 2. Python Generator (`scripts/generate-customer-mock-data.py`)

- Standard-library only (`argparse`, `json`, `random`, `unittest`,
  `dataclasses`, `datetime`) — no third-party dependencies.
- Generates a JSON array of Customer seed records:
  `keycloakUserId`, `firstName`, `lastName`, `email`, `phoneNumber`,
  `dateOfBirth`, `gender`, `pesel`.
- `--count N` (default 50), `--output PATH`, `--seed N` (reproducibility),
  `--self-test` (runs the built-in `unittest` suite).
- Small built-in Polish first-name/surname lists; emails use the reserved
  `example.test` domain — **no real personal data**.

### 3. PESEL Generation — Derived, Never Independent

Every PESEL is generated **from** `dateOfBirth` and `gender`, never the
reverse, mirroring `PeselValidator` exactly:

- Century/month encoding: 1800s +80, 1900s +0, 2000s +20, 2100s +40,
  2200s +60 added to the calendar month.
- Digits 7–9: 3-digit ordinal serial, derived from a per-record index and
  probed forward on collision to guarantee batch uniqueness.
- Digit 10 (gender): odd = MALE, even = FEMALE — nudged to the correct
  parity from the serial source.
- Digit 11 (checksum): weights `1,3,7,9,1,3,7,9,1,3` over digits 1–10,
  `(10 - (sum % 10)) % 10`.
- **Self-validation (`self_validate_batch`)** re-derives DOB/gender from
  every generated PESEL and re-checks the checksum before the batch is
  ever written to disk — a generator bug fails fast and loudly, it is
  never silently handed to Spring. `PeselValidator` remains the
  authoritative application-level validator regardless.
- Covered by 11 standard-library `unittest` cases: male/female parity,
  checksum, DOB round-trip, all five century offsets, and batch
  uniqueness (PESEL/`keycloakUserId`/email) — run via
  `python scripts/generate-customer-mock-data.py --self-test`.

### 4. Uniqueness

Within a generated batch: PESEL (probed on collision), `keycloakUserId`
(`mock-user-000001`, `mock-user-000002`, …), and email are all guaranteed
unique by construction, not random chance.

### 5a. Build-Time / Artifact Isolation (Layer 1 — strongest)

`CustomerSeedRunner`, `CustomerSeedProperties`, and `CustomerSeedRecord`
live under a dedicated source root, **not** `src/main/java`/`src/test/java`:

```
backend/src/local-seed/java/com/leasedemo/config/CustomerSeedRunner.java
backend/src/local-seed/java/com/leasedemo/config/CustomerSeedProperties.java
backend/src/local-seed/java/com/leasedemo/dto/CustomerSeedRecord.java
backend/src/local-seed/test/java/com/leasedemo/config/CustomerSeedRunnerTest.java
```

`backend/pom.xml` defines a `local-seed` Maven profile that uses
`build-helper-maven-plugin` to add `src/local-seed/java`/`src/local-seed/test`
as extra source roots — **only when that profile is explicitly
activated** (`mvn -Plocal-seed ...`). Normal builds
(`mvn clean package`, `mvn clean verify`, and CI) never activate this
profile, so `javac` never even sees these files: the resulting
`.class` files are **not compiled, not packaged, and not present** in
the ordinary Spring Boot fat JAR — regardless of
`SPRING_PROFILES_ACTIVE`, environment variables, or Spring properties
supplied at runtime, because there is no code there to activate.

This is a categorically stronger guarantee than a Spring profile: a
profile is a *runtime* switch evaluated by an already-running JVM that
already contains the class on its classpath; this mechanism removes the
class from the classpath entirely for any binary built the normal way.

### 5. Spring Runtime Seeding Guards (Layers 2-4, defense in depth)

- `CustomerSeedRunner` (`ApplicationRunner`) is annotated
  `@Profile("local")` **and** `@ConditionalOnProperty(name =
  "application.seed.customers.enabled", havingValue = "true")` — both
  conditions are required; neither alone is sufficient. There is no
  `local` profile (and thus no seeding capability at all) in any deployed
  environment.
- `application.seed.customers.enabled` defaults to `false`
  (`application-local.yml`) — a normal local startup never unexpectedly
  inserts mock data.
- No HTTP endpoint is exposed for seeding — this is deliberately a
  non-HTTP, CLI-invoked mechanism (`spring-boot:run` with seed
  properties), not a REST backdoor.
- Reads `application.seed.customers.file`, a path to the JSON produced by
  the Python generator, and deserializes it with the standard Jackson
  `ObjectMapper` already on the classpath.

### 6. `CustomerService` Reuse / Trust Boundary

`CustomerSeedRunner` calls the exact same
`CustomerService.createCustomer(String keycloakUserId,
CustomerCreateRequest request)` used by `CustomerController` — the seed
record's `keycloakUserId` is passed as a separate, out-of-band trusted
argument, precisely mirroring how the controller passes the verified JWT
`sub`. `CustomerSeedRecord.toCreateRequest()` deliberately excludes
`keycloakUserId` from the resulting `CustomerCreateRequest` — it is
**not**, and must never be, a field on that record; a production HTTP
client still cannot supply it. Every generated record passes through the
real `PeselValidator` → `AesGcmEncryptionService` →
`HmacLookupHashService` → `CustomerRepository` pipeline; no raw SQL
`INSERT` is used, and no field is written directly.

No fake JWTs are minted, `SecurityConfig` is untouched, and no Keycloak
users/Admin API calls are involved — `mock-user-NNNNNN` values are purely
local, synthetic identities understood only by the seeder and
`CustomerService`, never authenticated against Keycloak.

### 7. Idempotency / Rerun Behavior

Before calling `CustomerService`, the runner checks
`customerRepository.existsByKeycloakUserId(...)` and skips already-seeded
identities. `DuplicateCustomerException` (e.g. a PESEL collision against
existing data) is also caught per-record and counted as skipped, so one
already-present record never aborts the batch. A generator-side
`InvalidPeselException` is caught, counted as failed, and logged without
the PESEL value. A final summary is logged:
`Requested: N, Created: C, Skipped: S, Failed: F`. Re-running the seeder
with the same generated identities is safe and non-destructive; it never
overwrites existing rows.

### 8. Plaintext PESEL Handling

The generated JSON contains synthetic plaintext PESEL values only
transiently, in `tmp/mock-customers.json` — gitignored (`.gitignore` now
excludes `tmp/`, `scripts/tmp/`, `*.mock-customers.json`) and removed by
`scripts/postgres-seed-local-data.sh` on exit via a `trap`. Neither the
Python script's console output nor `CustomerSeedRunner` ever prints/logs
a PESEL value; only non-sensitive counts and file paths are logged.

### 9. `scripts/postgres-seed-local-data.sh`

```
./scripts/postgres-seed-local-data.sh        # 50 customers
./scripts/postgres-seed-local-data.sh 100    # custom count
```

Verifies `python`/`mvn` are on `PATH`, loads persistent
`CRYPTO_AES_KEY`/`CRYPTO_HMAC_KEY` via the shared
`scripts/lib/local-crypto-keys.sh` helper (see the M4.1.3 correction in
§11 above — no manual export required, and the same keys used by
`deploy-local.sh` are reused), generates the mock JSON into `tmp/`, invokes
`mvn -Plocal-seed spring-boot:run` (the explicit local-seed Maven profile
— see §5a) with `application.seed.customers.enabled=true` and
`application.seed.customers.file=<path>` under the `local` Spring profile,
prints the Created/Skipped/Failed summary from the application log, and
cleans up the temporary JSON on exit. Does not print PESEL values or
crypto keys. Does not reset/drop the database — that remains
`postgres-reset.sh`'s separate responsibility.

### 10. No Flyway / Schema Impact

No migration was added or modified. Flyway defines schema; mock data is
runtime-inserted application data, never a migration concern.

### 11. Real Local Validation (executed)

Ran against the local `lease-demo-postgres` container, after the §5a
build-time isolation change:

- Normal artifact audit: `mvn clean package` (no profile) →
  `jar tf target/lease-demo-0.1.0.jar | grep -i seed` → **zero matches**.
  `CustomerSeedRunner.class`, `CustomerSeedProperties.class`, and
  `CustomerSeedRecord.class` are absent.
- Local-seed artifact check: `mvn -Plocal-seed clean package` →
  the same `grep` **does** list all three classes under
  `BOOT-INF/classes/...` — confirming the profile correctly gates
  compilation/packaging in both directions.
- Runtime confirmation: the *normal* JAR was started directly
  (`java -jar lease-demo-0.1.0.jar --spring.profiles.active=local
  --application.seed.customers.enabled=true ...`) — it started
  successfully with no seeding-related log line at all (no bean of that
  type exists to be conditionally created), proving the profile/flag
  combination is powerless against the normal artifact.
- `./scripts/postgres-seed-local-data.sh 5` (uses `-Plocal-seed`
  internally): `Requested: 5, Created: 5, Skipped: 0, Failed: 0`;
  `SELECT COUNT(*) FROM customers;` → `5`; every row has non-null
  `pesel_encrypted`/`pesel_lookup`, no plaintext `pesel` column.
- Re-running the same command: `Requested: 5, Created: 0, Skipped: 5,
  Failed: 0`; row count remained `5` — idempotent, non-destructive rerun
  behavior is unaffected by the source-set relocation.

### 12. Interview Notes (M4.1.3)

90. **Why not seed through the normal authenticated REST endpoint?**
    That would require minting real (or fake) JWTs / provisioning dozens
    of throwaway Keycloak users purely for local demo data — extra
    infrastructure and a route to accidentally weakening
    authentication tooling. Calling `CustomerService` directly, in
    process, under an explicit local-only guard, reuses all business
    validation without touching the authentication boundary at all.
91. **Why not duplicate the AES/HMAC logic in Python?** Duplicated crypto
    implementations drift and are a classic source of subtle security
    bugs; the single source of truth for encryption/hashing must remain
    `AesGcmEncryptionService`/`HmacLookupHashService`. Python's only job
    is to produce plausible, structurally-valid business input.
92. **Why should seed data never be a Flyway migration?** Flyway
    migrations are permanent, versioned, and applied to every environment
    that runs them (including, potentially, non-local ones by mistake).
    Demo data is disposable, environment-specific, and must never become
    part of the permanent schema history.
93. **Why must the local seeder be both profile- and flag-guarded?**
    Defense in depth: a stray `local` profile activation, or a stray
    property flip alone, should still not be sufficient to trigger mock
    data insertion. Requiring both makes accidental activation
    significantly less likely, and neither condition can be true in a
    deployed environment (no `local` profile is ever configured there).
94. **Why not rely only on a Spring `local` profile for dangerous
    development tooling?** Because a profile is a *runtime* activation
    control — the code is still compiled and shipped inside the artifact;
    a misconfigured deployment could theoretically set
    `SPRING_PROFILES_ACTIVE=local` and the matching property. For
    sensitive development tooling like `CustomerSeedRunner`, build-time
    separation (`src/local-seed` + an explicit `local-seed` Maven profile,
    §5a) provides stronger defense in depth by ensuring the code is
    physically absent from the normal production artifact — there is
    nothing for a stray runtime flag to activate.

---

## Implemented vs Future Matrix

| Capability | Status |
|---|---|
| PostgreSQL local Docker | **IMPLEMENTED** |
| Persistent Docker volume | **IMPLEMENTED** |
| Spring Data JPA | **IMPLEMENTED** |
| Hibernate `ddl-auto=validate` | **IMPLEMENTED** |
| Flyway | **IMPLEMENTED** |
| `V1` — `customers` table | **IMPLEMENTED** |
| `Customer` entity | **IMPLEMENTED** |
| PESEL checksum validation | **IMPLEMENTED** |
| PESEL DOB validation | **IMPLEMENTED** |
| PESEL gender validation | **IMPLEMENTED** |
| AES-256-GCM (`pesel_encrypted`) | **IMPLEMENTED** |
| HMAC-SHA-256 lookup (`pesel_lookup`) | **IMPLEMENTED** |
| Separate AES/HMAC crypto keys | **IMPLEMENTED** |
| No committed crypto keys | **IMPLEMENTED** |
| Crypto key fail-fast validation | **IMPLEMENTED** |
| `Customer` create REST (`POST /api/customers`) | **IMPLEMENTED** (ahead of original M4.1 scope; not expanded further) |
| JWT `sub` automatic extraction into `keycloak_user_id` | **IMPLEMENTED (M4.1.2)** — via `@AuthenticationPrincipal Jwt` in `CustomerController`; no client-supplied identity field exists |
| Duplicate Keycloak identity rejection (409) | **IMPLEMENTED (M4.1.2)** — `existsByKeycloakUserId` early check + `keycloak_user_id` UNIQUE constraint |
| Local-only mock Customer seeder (M4.1.3) | **IMPLEMENTED** — build-time isolated under `src/local-seed` (`-Plocal-seed` required to compile/package) + `local` profile + `application.seed.customers.enabled=true`, reuses `CustomerService`, absent from normal production JAR |
| Testcontainers-based repository tests | **NOT IMPLEMENTED** |
| Managed PostgreSQL (production) | **FUTURE** |
| Production KMS/Secret Manager | **FUTURE** |
| Read replicas | **FUTURE** |
| PITR | **FUTURE** |
| BIK/KRD integration | **FUTURE** |
| Credit scoring | **FUTURE** |
| PDF generation | **FUTURE** |
| E-signature | **FUTURE** |

---

## M4.2 — Frontend Design System Foundation — IMPLEMENTED

### 1. Objective

Before implementing either the Admin or Customer application shells, M4.2
establishes a single, permanent LeaseDemo frontend visual/component
foundation: Angular Material (primitives/accessibility) + Tailwind CSS
(layout/utility styling) + a small `shared/ui` control library + a
permanent `/ux-demo` living component catalog. This milestone deliberately
does **not** implement AdminLayout, CustomerLayout, role-based shell
selection, or any real Customer list/table UI — those are explicitly
deferred to a later milestone.

### 2. Dependencies Added

| Package | Version | Rationale |
|---|---|---|
| `@angular/material` | `18.2.14` (exact) | Matches the project's existing Angular 18.x line exactly (peer-dependency compatible); newest 18.x patch at time of installation. |
| `@angular/cdk` | `18.2.14` (exact) | Required peer of `@angular/material`; kept in lockstep. |
| `tailwindcss` | `3.4.17` (dev) | Tailwind v3 uses the mature PostCSS pipeline that Angular CLI's `@angular-devkit/build-angular:application` builder auto-detects out of the box (via a root `tailwind.config.js`). Tailwind v4's new engine targets a different build integration model not demonstrated as a drop-in fit for this Angular CLI version, so v3 was chosen deliberately over "latest" per the milestone's compatibility-first instruction. |
| `postcss` | `8.4.49` (dev) | Required by Tailwind v3/Angular CLI's PostCSS pipeline. |
| `autoprefixer` | `10.4.20` (dev) | Standard Tailwind v3 companion for vendor-prefixing. |

No Angular, TypeScript, or RxJS version changes were made. No other UI/CSS
framework, icon package, or Storybook was introduced.

### 3. Responsibility Split (permanent project rule)

```
Angular Material  →  primitive/behavior/accessibility layer
                      (buttons, form fields, inputs, dialogs, tables,
                       sorting, pagination, snackbars, progress indicators)

Tailwind CSS      →  layout / spacing / sizing / alignment / responsive
                      breakpoints / simple visual utilities

SCSS              →  Material theme integration, and the rare cases where
                      a small component-level rule is clearer than a long
                      Tailwind utility chain (e.g. Material CSS custom
                      property overrides)

shared/ui         →  LeaseDemo's own reusable, application-level UI API
                      (app-button, app-card, app-page-header, app-input)

/ux-demo          →  living visual catalog that CONSUMES shared/ui; never
                      a second, duplicate implementation of those controls
```

Features are expected to consume `<app-button>`, `<app-card>`, etc. when
LeaseDemo wants a stable, reusable convention. Raw Material usage directly
in a one-off feature template remains acceptable where a shared wrapper
would add no value — not every Material primitive is wrapped.

### 4. Design Tokens — Single Source of Truth

`frontend/src/styles/_tokens.scss` is the **one** place LeaseDemo's core
visual tokens are declared (primary/primary-hover, accent, surface/
surface-muted, border, text-primary/secondary/on-primary, success/warning/
danger, radius, shadow, font family). Values were chosen for a
conservative corporate banking/leasing character: dark slate/navy primary
(`#1e293b`), white/near-white surfaces, a single restrained blue accent,
subtle borders/shadows, small-to-moderate radius — no neon, gradients, or
glassmorphism.

These SCSS variables are simultaneously emitted as CSS custom properties
(`--ld-color-primary`, etc.) on `:root`. Both other styling mechanisms
consume this same bridge rather than maintaining independent copies:

- **Tailwind** (`frontend/tailwind.config.js`) maps utility color/radius/
  shadow names (`bg-surface`, `text-secondary`, `bg-danger`, `rounded-sm`,
  …) to `var(--ld-color-*)` / `var(--ld-radius-*)` / `var(--ld-shadow-*)` —
  it never re-declares a raw hex value.
- **Angular Material** (`frontend/src/styles/_material-theme.scss`) builds
  its M3 theme (`mat.define-theme`) from the closest stock Material
  palettes for internal chrome/state-layers, then overrides the small set
  of visible M3/MDC CSS custom properties (`--mat-sys-primary`,
  `--mdc-filled-button-container-color`, …) with the same `tokens.$ld-*`
  SCSS variables, so Material's visible primary/surface/text colors are
  byte-identical to LeaseDemo's canonical tokens.

Changing a brand color therefore only ever requires editing
`_tokens.scss`.

### 5. Global Styles

`frontend/src/styles.scss` stays small and intentional: it only
`@use`s `styles/tokens` and `styles/material-theme`, emits the three
`@tailwind` layer directives, and keeps a minimal box-sizing/root
reset. Component-specific styling lives with each component.

Tailwind's `preflight` base-reset layer is explicitly disabled
(`corePlugins: { preflight: false }`) because Angular Material ships its
own well-tested component resets; running both reset layers risked
visibly fighting each other (e.g. default `<button>`/`<table>`
appearance). LeaseDemo's own minimal reset in `styles.scss` covers the
gap instead.

### 6. `shared/ui` — Implemented Controls

```
frontend/src/app/shared/ui/
  button/button.component.{ts,scss,spec.ts}
  card/card.component.{ts,scss,spec.ts}
  page-header/page-header.component.{ts,scss,spec.ts}
  input/input.component.{ts,scss,spec.ts}
  index.ts
```

All four are standalone, `ChangeDetectionStrategy.OnPush`, selector
prefix `app`. No NgModule was introduced for this library. No
speculative controls (data-table, paginator, dialog, badge, select,
empty/loading state) were added — those will be introduced when a real
feature actually needs them.

- **`app-button`** — wraps `mat-flat-button` (real `<button>` semantics,
  ripple, focus, native `disabled`). Inputs: `variant`
  (`'primary' | 'secondary' | 'danger'`, default `'primary'`), `disabled`
  (`boolean`, default `false`). Content is projected
  (`<app-button>Save customer</app-button>`). No configuration explosion —
  exactly the two inputs the milestone asked for.
- **`app-card`** — a zero-input surface/container (`background`, `border`,
  `border-radius`, subtle `box-shadow`, padding, all from tokens). Content
  is projected; layout of that content is left to the caller via
  Tailwind utility classes on the projected markup.
- **`app-page-header`** — inputs `title` (`input.required<string>()`,
  rendered as a semantic `<h1>`) and optional `subtitle`
  (`string | undefined`). A projected actions area (typically
  `<app-button>`s) renders trailing-aligned; hidden via `:empty` CSS when
  nothing is projected.
- **`app-input`** — wraps `mat-form-field` + `matInput` with a custom
  `ErrorStateMatcher` driven by an explicit `invalid` input, and
  implements `ControlValueAccessor` so it works transparently with both
  `[(ngModel)]` and reactive `[formControl]`/`formControlName` — it does
  **not** reimplement or replace Angular Forms. Inputs: `label`,
  `placeholder`, `disabled`, `invalid`, `errorMessage`.

`frontend/src/app/shared/ui/index.ts` is a small barrel re-exporting all
four components (and the `AppButtonVariant` type) so features/UX Demo
import from a single, discoverable path.


### 7. `/ux-demo` — Living Design-System Catalog

```
frontend/src/app/features/ux-demo/
  ux-demo.routes.ts
  pages/ux-demo-page/ux-demo-page.component.{ts,html,scss,spec.ts}
  data/ux-demo.data.ts
```

Routed at `/ux-demo` (registered in `app.routes.ts`, lazy-loaded via
`loadChildren`). The page imports and composes the **real**
`ButtonComponent`, `CardComponent`, `PageHeaderComponent`, and
`InputComponent` from `shared/ui` — it contains zero duplicate
implementations of those controls. Sections:

- **Typography** — page title, section heading, body text, secondary/
  helper text, field label, all using LeaseDemo's typography scale.
- **Buttons** — every actually-supported `app-button` variant/state:
  primary, secondary, danger, disabled. No invented variants.
- **Cards** — three fictional lease/customer cards (`ux-demo.data.ts`)
  rendered via `app-card`, with a small inline status pill.
- **Page headers** — title-only, title+subtitle, and title+subtitle+
  projected-action examples, matching the milestone's target usage
  pattern.
- **Inputs** — normal, placeholder, disabled, and an error/invalid state
  with `errorMessage`, all via `app-input`.
- **Material integration** — a themed `mat-progress-spinner` proving the
  LeaseDemo primary color flows correctly into Material's own components.

`ux-demo.data.ts` contains a small, static, frontend-only, clearly
fictional dataset (`DEMO_LEASE_CUSTOMERS`) — no backend/Customer API call,
no PostgreSQL, no Keycloak dependency, no real personal information.

**Permanent convention (documented here for future milestones):**
whenever a meaningful new reusable control is added to `shared/ui/` (e.g.
a future `app-badge`, `app-data-table`, `app-paginator`, `app-dialog`,
`app-select`, or loading/empty/error state control), a representative
example should normally also be added to `/ux-demo`, keeping it a living
catalog synchronized with the real component library.

### 8. Routing / Future Security

`/ux-demo` currently sits behind the existing `authGuard` (any
authenticated session), exactly like `/dashboard` and `/exchange-rates` —
no new authentication/authorization mechanism was introduced.

**FUTURE:** once role-aware Admin/Customer application shells exist,
`/ux-demo` must be reachable **only** from Admin/developer-facing
navigation and **never** exposed in Customer-facing navigation. This
milestone does not implement that navigation restriction because doing so
would require pulling AdminLayout/CustomerLayout into scope, which is
explicitly deferred.

### 9. Accessibility

- `app-button` renders a real `<button type="button">`; `disabled` sets
  the native `disabled` attribute (semantic, not a CSS-only style).
- `app-page-header` uses a semantic `<h1>` for its title.
- `app-input` keeps Material's label/`mat-error`/focus-ring accessibility
  behavior intact and wires a real `ControlValueAccessor` so screen
  readers and Angular Forms validation continue to work normally.
- No clickable `<div>`s were introduced in place of buttons/links.

### 10. Responsive Foundation

No fixed-width assumptions were baked in; `/ux-demo` uses Tailwind's
responsive grid utilities (e.g. `sm:grid-cols-2 lg:grid-cols-3`) so its
card section reflows at tablet width, and `app-page-header` wraps its
actions area via `flex-wrap`. Full Admin/Customer sidebar responsive
behavior remains future work (§ M4.2 explicitly excludes those shells).


### 11. Tests

Focused Jasmine/Karma unit tests were added for every new shared control
and for the UX Demo page (`ng test --no-watch --browsers=ChromeHeadlessCI`
→ **45/45 passing**, 0 failing), covering:

- `app-button`: projected content, variant reflection (default + explicit),
  native `disabled` semantics.
- `app-page-header`: title rendering, optional subtitle presence/absence,
  projected action rendering.
- `app-card`: projected content renders inside the card surface.
- `app-input`: label, placeholder, disabled state, `mat-error` rendering
  when invalid, and `ngModel` round-trip via a host component.
- `UxDemoPageComponent`: page renders, and composes the real
  `app-page-header`/`app-button`/`app-card`/`app-input` controls (not
  duplicate markup).

No Angular Material internals, CSS-framework implementation details, or
snapshot tests were added.

### 12. Validation Performed

- `npm run build` (development configuration) — succeeded, no errors.
- `npm run build:prod` (production configuration) — succeeded, no errors.
  Initial bundle: **390.20 kB raw / 100.60 kB estimated transfer**,
  comfortably under the existing `500kB` warning / `1MB` error initial
  budget; no `anyComponentStyle` budget warnings were reported.
  **Budgets in `angular.json` were left untouched** — no artificial
  increase was needed.
- `npm run test:ci` — **45/45 tests passing**.
- `npm run lint` — fails with `Could not find the '@angular-eslint/builder:lint' builder's node package`. This is a **pre-existing** condition (no ESLint packages are present in `package.json`/`devDependencies` prior to this milestone) and was not introduced or repaired by M4.2, per the instruction not to fix unrelated historical failures.
- Manual verification: `ng serve` dev server responded `200` for
  `GET /ux-demo`; inspection of the compiled `styles-*.css` confirmed
  both a Tailwind utility (`flex-wrap:wrap`) and Material component
  classes (`mat-mdc-*`) are present in the same stylesheet, and that
  `--mat-sys-primary` resolves to LeaseDemo's canonical token value
  (`#1e293b`), proving the Material/Tailwind/token integration works
  end-to-end rather than merely compiling.

### 13. Explicit Non-Goals (deferred)

- `AdminLayout` — **NOT implemented**.
- `CustomerLayout` — **NOT implemented**.
- Role-based shell/navigation selection — **NOT implemented**.
- Any real Customer list/table/pagination UI or Customer API call from
  the frontend — **NOT implemented**.
- Storybook or any second UI/CSS/component framework — **NOT introduced**.
- Backend, Spring Security, Keycloak configuration, Flyway, or the
  `Customer` domain — **untouched**.

### 14. Interview-Ready Notes

**Q: Why combine Angular Material and Tailwind?**
A: Material provides robust, accessible, behavior-rich primitives (focus
management, ARIA, ripple, keyboard interaction), while Tailwind provides
efficient layout and application-specific visual composition without
writing large amounts of bespoke CSS for every spacing/alignment need.

**Q: Why wrap some Material components in shared UI controls?**
A: To expose a stable LeaseDemo-level UI API and centralize recurring
visual and behavioral conventions (variants, tokens, disabled semantics)
without coupling every feature directly to Material implementation
details.

**Q: Why not wrap every Material component?**
A: Unnecessary wrappers create abstraction without value. Shared controls
are introduced only where reuse, consistency, or encapsulation actually
justifies them — e.g. a one-off Material usage in a highly specific
feature does not need a shared wrapper.

**Q: Why create the design system before Admin/Customer layouts?**
A: Both role-specific areas should consume one visual/component system
rather than independently inventing their own styling conventions,
avoiding visual drift between Admin and Customer experiences.

**Q: Why have UX Demo if Storybook exists (as a concept)?**
A: LeaseDemo only needs a lightweight in-application living catalog at
this stage. It provides fast visual verification without adding
Storybook's dependency/configuration/build overhead, while still fully
demonstrating the real, working components.

**Q: What is the difference between shared/ui and UX Demo?**
A: `shared/ui` **implements** reusable controls. `/ux-demo` **consumes**
and demonstrates them — it is a consumer of the design system, never a
second design system.

---

## M4.3 — Role-Aware Angular Application Shell — IMPLEMENTED

### 1. Objective

M4.3 builds the permanent role-aware LeaseDemo application shell on top of
M4.2's design system. After successful Keycloak authentication, Angular
determines the user's LeaseDemo application role and routes/renders the
appropriate area — the user never manually chooses between Admin and
Customer.

```
Keycloak → authenticated → JWT → Angular auth state → role resolution
                                                          |
                                              +-----------+-----------+
                                              |                       |
                                            ADMIN                 CUSTOMER
                                              |                       |
                                         AdminLayout            CustomerLayout
```

**Permanent security principle** (unchanged from before, reaffirmed here):
Angular role guards are UX/navigation controls, not the authoritative
security boundary. Spring Security (JWT `ROLE_*` authorization on
`/api/**`) remains the sole trust boundary for backend data and operations.
Hiding an Admin menu item does not, by itself, secure any backend endpoint.

### 2. Existing Auth Architecture Reused

M4.3 did not introduce a second authentication abstraction. It builds
directly on the existing, working pieces:

| Existing piece | File | Reused as |
|---|---|---|
| Keycloak adapter bootstrap | `core/auth/keycloak.factory.ts` | Unchanged |
| `AuthService` | `core/services/auth.service.ts` | Unchanged — still the only place that touches the Keycloak adapter and `keycloak.tokenParsed`; already exposed `roles()` as a signal |
| `authGuard` | `core/guards/auth.guard.ts` | Unchanged — still the first guard on every protected route |

No JWT is manually decoded anywhere in M4.3 — `AuthService.roles()` (already
populated from `keycloak.tokenParsed['realm_access'].roles` in M4.1-era
code) is the only role source consulted.

### 3. Actual Keycloak Role Names

Inspected directly from `infrastructure/keycloak/lease-demo-realm.json`
(untouched by this milestone): the realm defines exactly three
non-default roles — `ADMIN`, `CUSTOMER`, `ADVISOR` — plus Keycloak's own
`default-roles-lease-demo` composite (`offline_access`,
`uma_authorization`). `ADVISOR` has no corresponding application area in
this milestone.

### 4. Application-Level Role Representation

`core/auth/models/app-role.model.ts`:

```ts
export enum AppRole { Admin = 'ADMIN', Customer = 'CUSTOMER' }

export function mapKeycloakRolesToAppRoles(
  keycloakRoles: readonly string[]
): AppRole[]
```

This is the **single, centralized** place that compares against raw
Keycloak role strings. `ADVISOR`/`offline_access`/`uma_authorization` are
silently ignored (contribute no `AppRole`) — no other file performs
`roles.includes('ADMIN')`-style checks.

### 5. Centralized Role Resolution

`core/auth/services/role.service.ts` (`RoleService`, `providedIn: 'root'`)
exposes:

- `appRoles` — computed signal, `AppRole[]` from `AuthService.roles()`.
- `isAdmin` / `isCustomer` — computed boolean signals.
- `hasNoRecognizedRole` — computed boolean signal (fail-closed detector).
- `resolveLandingRoute(): string | null` — `/admin/dashboard`,
  `/customer/dashboard`, or `null` (no recognized role).

Every guard/layout consults `RoleService` — none independently parses
tokens or `AuthService.roles()`.

### 6. Multi-Role Precedence (documented rule)

**A user with BOTH `ADMIN` and `CUSTOMER` lands in the Admin area by
default (`ADMIN` wins).** This is a deliberate, deterministic demo-scope
rule — no role-switcher UI exists or is planned in this milestone. It only
governs the *default landing route*; it does not by itself grant or deny
access to `/customer/**` (see § 8).

### 7. Unknown-Role (Fail-Closed) Behavior

An authenticated user whose Keycloak roles map to no `AppRole` (e.g.
`ADVISOR`-only) is granted **neither** Admin nor Customer access.
`RoleService.resolveLandingRoute()` returns `null`, and every guard that
would otherwise need a role match instead redirects to `/access-denied`.
No silent fallback to either area is possible.

### 8. Route Structure

```
/                → rootRedirectGuard → /admin/dashboard | /customer/dashboard | /access-denied
/admin/**        → AdminLayout    (adminAreaGuard: requires ADMIN)
  /admin/dashboard
  /admin/customers   (placeholder)
  /admin/leases      (placeholder)
/customer/**     → CustomerLayout (customerAreaGuard: requires CUSTOMER)
  /customer/dashboard
  /customer/leases    (placeholder)
  /customer/documents (placeholder)
  /customer/profile   (placeholder)
/ux-demo         → adminAreaGuard (ADMIN-only; URL preserved, not nested under /admin)
/access-denied   → authGuard only (any authenticated user)
/exchange-rates  → authGuard only (unchanged from earlier milestones)
**               → NotFoundComponent (unprotected, unchanged)
```

All feature routes remain lazily loaded (`loadChildren`/`loadComponent`).

**Customer-area policy** (deliberately strict, per instruction): the
Customer area requires the `CUSTOMER` role outright. An ADMIN who also
holds `CUSTOMER` is naturally allowed into `/customer/**`; an ADMIN-only
user is denied and redirected to `/access-denied`.

### 9. Root Redirect Behavior

`core/auth/guards/root-redirect.guard.ts` (`rootRedirectGuard`) runs on `/`
after `authGuard` guarantees an authenticated session, and always issues a
`UrlTree` redirect (`router.parseUrl(...)`) to one of `/admin/dashboard`,
`/customer/dashboard`, or `/access-denied` — never `true`. No redirect loop
is possible because none of those three destinations depends on
`rootRedirectGuard` again.

### 10. Guards Created

`core/auth/guards/role.guard.ts`:

- `adminAreaGuard` — allows navigation only when `RoleService.isAdmin()`;
  otherwise redirects to `/access-denied`.
- `customerAreaGuard` — allows navigation only when
  `RoleService.isCustomer()`; otherwise redirects to `/access-denied`.

Both guards assume `authGuard` already ran on the same route array
(`canActivate: [authGuard, adminAreaGuard]`, etc.) and only add the role
check on top — they do not duplicate authentication logic.

### 11. AdminLayout

`layout/admin-layout/admin-layout.component.{ts,html,scss}`:

- Composes `AppHeaderComponent` + `SidebarComponent` + `<router-outlet>`.
- Navigation: **Dashboard, Customers, Leases**, then a visually separated
  secondary group: **UX Demo**.
- Standalone, `ChangeDetectionStrategy.OnPush`.

### 12. CustomerLayout

`layout/customer-layout/customer-layout.component.{ts,html,scss}`:

- Same header/sidebar/`router-outlet` composition as AdminLayout (same
  design tokens, same `.app-shell`/`.app-shell__sidebar`/
  `.app-shell__content` structure) — the two shells visibly belong to one
  product.
- Navigation: **Dashboard, My leases, Documents, My profile**. No UX Demo,
  no Customers, no other Admin-facing item.

### 13. Shared Shell Components

`layout/components/`:

- `app-header/` (`AppHeaderComponent`) — LeaseDemo brand, authenticated
  username (`AuthService.username()`), Logout button delegating to the
  existing `AuthService.logout()` (full Keycloak session termination — not
  reimplemented). No raw JWT/token data is rendered.
- `sidebar/` (`SidebarComponent`) — renders a typed `NavigationItem[]`
  (`layout/models/navigation-item.model.ts`: `{ label, route, icon? }`)
  plus an optional visually-separated `secondaryItems` group. Active route
  indicated via `routerLinkActive`. AdminLayout and CustomerLayout each own
  their own navigation arrays and pass different data into this same
  component — no per-item role logic lives inside the sidebar itself.

These are **layout-level** components (not `shared/ui`) per the milestone
guidance that sidebar/header normally belong to `layout/`, not `shared/ui`
— so no new UX Demo catalog entry was added for them.

### 14. UX Demo Authorization Policy

`/ux-demo` is now guarded by `adminAreaGuard` (previously only `authGuard`).
The URL is preserved exactly (`path: 'ux-demo'`, not nested under
`/admin/ux-demo`), avoiding a route duplication while satisfying: ADMIN can
reach `/ux-demo`; CUSTOMER is redirected to `/access-denied`. AdminLayout's
secondary navigation group links to it directly.

### 15. Admin Dashboard / Customer Dashboard

`features/admin/dashboard/admin-dashboard-page.component.{ts,html}` and
`features/customer/dashboard/customer-dashboard-page.component.{ts,html}`:
static shell-validation pages composed entirely from `shared/ui`
(`app-page-header`, `app-card`) plus Tailwind utility classes for the card
grid. No backend calls, no charts, no fabricated metrics — exactly the
copy specified in the milestone brief ("Administration — Manage customers,
leases and operational data." / "Welcome — View your leases, documents and
account information.").

### 16. Placeholder Feature Pages

`shared/components/feature-placeholder/feature-placeholder.component.ts`
— one small, reusable placeholder (`app-page-header` + `app-card`, a
`title` input bound from route `data.title` via the existing
`withComponentInputBinding()` router feature) used by `/admin/customers`,
`/admin/leases`, `/customer/leases`, `/customer/documents`, and
`/customer/profile`. No CRUD, forms, or tables were added anywhere.

### 17. Access Denied Page

`features/access-denied/access-denied-page.component.{ts,html}` — built
from `shared/ui` (`app-page-header`, `app-card`, `app-button`). Explains,
in plain language, that the account does not have access to the requested
area, and offers a Logout action delegating to the existing
`AuthService.logout()`. No internal authorization detail or JWT claim is
rendered.

### 18. Responsive Behavior

`.app-shell__sidebar` collapses from `15rem` to `3.75rem` at `max-width:
900px`, and `.app-shell__content` padding reduces at `max-width: 600px`.
Desktop/laptop remains the primary target; this establishes the minimum
sensible narrower-screen behavior requested (sidebar does not permanently
consume excessive width; content padding still fits; header remains a
fixed, always-visible bar). Full mobile-specific UX (e.g. an off-canvas
Material `mat-drawer`) remains future work, as scoped.

### 19. Root `AppComponent` Change

`AppComponent`'s pre-existing minimal Login/Logout status bar is now shown
**only** for routes outside `/admin/**` and `/customer/**`
(`isInsideApplicationShell` signal, updated on `Router` `NavigationEnd`),
since those two areas render their own `AppHeaderComponent` via
AdminLayout/CustomerLayout. This avoids stacking two headers; it does not
change authentication behavior.

### 20. shared/ui / Design-System Reuse

No new `shared/ui` component was introduced in this milestone — every new
page (dashboards, placeholders, access-denied) composes the existing
`app-page-header`/`app-card`/`app-button` controls from M4.2, and every new
SCSS file `@use`s the same `styles/tokens` source of truth as the rest of
the application. No second visual language, no duplicated token palette.

### 21. Tests Added

| File | Coverage |
|---|---|
| `core/auth/models/app-role.model.spec.ts` | Keycloak → AppRole mapping, including ADVISOR/offline_access being ignored |
| `core/auth/services/role.service.spec.ts` | ADMIN → Admin area, CUSTOMER → Customer area, ADMIN-wins precedence, fail-closed (unrecognized role and no roles) |
| `core/auth/guards/role.guard.spec.ts` | `adminAreaGuard` allows ADMIN/denies CUSTOMER-only; `customerAreaGuard` allows CUSTOMER/denies ADMIN-only/allows ADMIN+CUSTOMER |
| `app.routes.spec.ts` | `/admin`, `/customer`, `/ux-demo` are wired to the correct guard function (route-config-level check, no Router exercised) |
| `layout/admin-layout/admin-layout.component.spec.ts` | Admin navigation renders, UX Demo renders, Customer-only labels absent |
| `layout/customer-layout/customer-layout.component.spec.ts` | Customer navigation renders, UX Demo absent, Admin "Customers" absent |
| `layout/components/app-header/app-header.component.spec.ts` | Username renders, brand renders, Logout delegates to `AuthService.logout()` |

Per instruction, no test exercises Angular Router or Material internals.

### 22. Validation Performed

- `npm run build` — succeeded, no errors.
- `npm run build:prod` — succeeded, no errors. Initial bundle: **395.13 kB
  raw / 101.95 kB estimated transfer**, comfortably under the existing
  `500kB` warning / `1MB` error initial budget (unchanged from M4.2 —
  `angular.json` budgets were not modified). New feature/route lazy chunks
  are all small (largest new one: `admin-routes`/`customer-routes` at
  ~2.8 kB raw each; `admin-dashboard-page-component`/
  `customer-dashboard-page-component` ~1.2 kB raw each).
- `npm run test:ci` — **72/72 tests passing** (45 pre-existing + 27 new).
- `npm run lint` — still fails with `Could not find the
  '@angular-eslint/builder:lint' builder's node package`. This is the same
  **pre-existing** condition reported in M4.2 (no ESLint packages present
  in `package.json`) and was not introduced or repaired by M4.3.
- Manual verification: performed against the project's existing single
  test identity available without Keycloak realm modification (per
  instruction not to create Keycloak users merely for testing). The
  ADMIN-vs-CUSTOMER-vs-unknown-role matrix beyond that single account is
  covered by the unit tests in § 21 (mocked `RoleService`/`AuthService`
  states), which exercise every branch (ADMIN, CUSTOMER, ADMIN+CUSTOMER,
  unrecognized role, no roles) that a live second account would otherwise
  demonstrate.

### 23. Explicit Non-Goals (deferred)

- Backend, Spring Security authorization rules, Flyway, and Keycloak
  realm/client configuration — **untouched**.
- Any Customer query API, Customer table/list/pagination UI, real lease or
  document data — **NOT implemented**.
- A role-switcher UI — **intentionally not built** (§ 6 explains why).
- Full mobile/phone-specific navigation (off-canvas drawer) — **deferred**;
  only the minimum tablet-width sidebar-collapse behavior was implemented.
- Repairing the pre-existing `ng lint` infrastructure gap — **out of
  scope**, reported as-is.

### 24. Interview-Ready Notes

**Q: Why have role guards if backend authorization is authoritative?**
A: Frontend guards provide correct navigation and UX, while backend
security remains the actual trust boundary protecting data and operations.

**Q: Why centralize role mapping?**
A: It prevents Keycloak-specific role strings from leaking throughout the
UI and provides one deterministic application-level interpretation.

**Q: What happens when a user has no recognized role?**
A: The application fails closed and displays Access Denied rather than
defaulting to a more privileged or arbitrary area.

**Q: Why separate AdminLayout and CustomerLayout?**
A: The two user types have different navigation and workflows while still
sharing one product design system and reusable shell primitives.

**Q: Why does ADMIN win when both roles exist?**
A: It provides deterministic default navigation for the demo without
introducing unnecessary role-switching complexity.

---

## M4.4 — Admin Customer Management: Server-Side Query + Pagination + Search — IMPLEMENTED

### 1. Objective

M4.4 implements the first real ADMIN business feature on top of M4.3's
role-aware shell: `Admin → Customers`, a server-side paged, sorted, and
searchable Customer list backed by the existing PostgreSQL `customers`
table.

```
Admin Customers Page → HTTP → GET /api/customers (ROLE_ADMIN)
    → CustomerController → CustomerService → JpaSpecificationExecutor
    → CustomerRepository → PostgreSQL → Page<Customer> → CustomerListItemResponse
    → PageResponse<T> → Angular CustomerService → app-data-table + mat-paginator
```

### 2. GET /api/customers Contract

```
GET /api/customers?page=0&size=20&search=kowalski&sortField=lastName&sortDirection=asc
```

| Param          | Default    | Notes                                              |
|----------------|------------|-----------------------------------------------------|
| `page`         | `0`        | zero-based; negative values clamp to 0              |
| `size`         | `20`       | clamped to `[1, 100]` — no unbounded row retrieval  |
| `search`       | none       | free-text over firstName/lastName/email             |
| `sortField`    | none       | allow-listed; default sort applies when omitted     |
| `sortDirection`| `asc`      | `asc` or `desc`                                     |

Response body is the application-owned `PageResponse<CustomerListItemResponse>`
(see § 5/6 below) — not Spring Data's own `Page` JSON shape.

### 3. Authorization — Spring Security, Not the Controller

`SecurityConfig` declares an explicit rule ahead of the general `/api/**`
authenticated rule:

```java
.requestMatchers(HttpMethod.GET, "/api/customers").hasRole("ADMIN")
```

Behavior:
- **no JWT** → 401 Unauthorized (rejected before reaching the controller)
- **ROLE_CUSTOMER** → 403 Forbidden
- **ROLE_ADMIN** → 200 OK

`CustomerController` performs no manual role/claim inspection — Spring
Security's declarative `hasRole("ADMIN")` is the authoritative boundary,
consistent with the existing `/actuator/**` ADMIN rule. This is a direct
continuation of M4.3's stated principle: Angular's `adminAreaGuard` on
`/admin/customers` is UX/navigation only; Spring Security is the real trust
boundary for the underlying data.

### 4. Pagination

Standard Spring Data `Pageable`/`PageRequest`, built in `CustomerService`
from validated/clamped `page`/`size` values (default `page=0`, `size=20`,
max `size=100`). Pagination is executed by PostgreSQL via the generated
`LIMIT`/`OFFSET` SQL — Angular never receives more than one page of rows.

introducing unnecessary role-switching complexity.

### 5. `PageResponse<T>` — Application-Owned Envelope

```java
public record PageResponse<T>(
        List<T> content, int page, int size,
        long totalElements, int totalPages, boolean first, boolean last) {
    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) { ... }
}
```

Built from Spring Data's `Page<Customer>` via `PageResponse.from(page, mapper)`.
Deliberately small and decoupled from Spring Data's own `Page` JSON
serialization, so the external API contract does not shift if Spring Data's
internal `Page` shape changes across versions.

### 6. `CustomerListItemResponse` — List DTO

```java
public record CustomerListItemResponse(
        UUID id, String firstName, String lastName, String email,
        String phoneNumber, LocalDate dateOfBirth, Gender gender, Instant createdAt) {
}
```

Mapped from `Customer` via a new `CustomerMapper.toListItemResponse(...)`
MapStruct method (same mapper used for `CustomerResponse`).

**Explicitly excluded** (never returned by this endpoint):
- `peselEncrypted`
- `peselLookup`
- plaintext PESEL (never persisted anywhere, per M4.1.2/M4.1.3)
- `keycloakUserId` — no concrete Admin UI requirement exists for it in this
  milestone

Verified by `CustomerControllerTest` assertions (`jsonPath(...).doesNotExist()`)
on the ADMIN-role success response.

### 7. Search

`CustomerSpecifications.searchByNameOrEmail(searchTerm)` builds a
`Specification<Customer>` matching `firstName`/`lastName`/`email`
case-insensitively (`cb.like(cb.lower(...), "%term%")`). A null/blank
search term returns a specification matching everything (`cb.conjunction()`),
so the same query path serves both filtered and unfiltered requests. PESEL
(raw, encrypted, or lookup hash) is never part of this search — decrypting
every row to search plaintext PESEL was explicitly out of scope.

### 8. Sorting — Explicit Allow-List

```java
private static final Set<String> SUPPORTED_SORT_FIELDS =
        Set.of("firstName", "lastName", "email", "dateOfBirth", "createdAt");
private static final Sort DEFAULT_SORT =
        Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName"));
```

An unsupported `sortField` throws `InvalidSortFieldException`, translated by
`GlobalExceptionHandler` into `400 Bad Request` (`ProblemDetail`) — consistent
with the existing `InvalidPeselException` handling style. The API owns its
sort contract; client-supplied property names are never passed directly into
persistence sorting.

### 9. Repository / Query Implementation

`CustomerRepository` additionally extends `JpaSpecificationExecutor<Customer>`
— the smallest Spring Data mechanism supporting an optional predicate
composed with `Pageable` in a single query, without introducing QueryDSL or
another persistence dependency. `CustomerService.getCustomers(...)` calls:

```java
customerRepository.findAll(CustomerSpecifications.searchByNameOrEmail(search), pageable);
```

Pagination, sorting, and filtering are all translated into one SQL query
executed by PostgreSQL (`WHERE ... LIKE ... ORDER BY ... LIMIT ... OFFSET ...`)
— there is no in-memory filtering, sorting, or loading of unrelated rows.

### 10. Flyway / Schema — No Migration Added

The existing `V1__create_customer_table.sql` schema was inspected and found
sufficient: `firstName`/`lastName`/`email` are plain, unindexed `VARCHAR`
columns, and with ~200 demo rows a sequential scan for `LIKE` search and
sort is not a genuine performance concern. No `V2` migration was added —
introducing an index without a demonstrated need would have been
speculative schema churn for this milestone's scale. `pesel_encrypted`/
`pesel_lookup` columns/constraints were not touched.

### 11. Angular Feature Structure

```
frontend/src/app/features/admin/customers/
  models/
    customer-list-item.model.ts   — CustomerListItem (mirrors CustomerListItemResponse)
    customer-query.model.ts       — CustomerQuery, CustomerSortField, SortDirection
    page-response.model.ts        — PageResponse<T> (mirrors backend PageResponse<T>)
  services/
    customer.service.ts           — GET /api/customers via ApiService
  pages/customer-list/
    customer-list-page.component.{ts,html,scss,spec.ts}
```

`/admin/customers` (already ADMIN-protected by `adminAreaGuard`, M4.3) now
lazily loads `CustomerListPageComponent` instead of `FeaturePlaceholderComponent`.

### 12. Reactive Query Flow

`CustomerListPageComponent` composes exactly one request pipeline:

```typescript
combineLatest([search$, toObservable(page), toObservable(pageSize),
               toObservable(sortField), toObservable(sortDirection)])
  .pipe(switchMap(([...]) => customerService.getCustomers(query).pipe(catchError(...))))
  .subscribe(...)
```

- `search$` = `valueChanges` → `debounceTime(300ms)` → `distinctUntilChanged()`
- `page`/`pageSize`/`sortField`/`sortDirection` are Signals bridged to
  Observables via `toObservable()` (Angular's `rxjs-interop`), read as field
  initializers (constructor-time injection context) rather than inside
  `ngOnInit` — `toObservable()` requires an injection context.
- `switchMap` guarantees an obsolete in-flight request can never overwrite a
  newer one (e.g. a fast second keystroke's request wins over a slow first).
- A separate `searchControl.valueChanges` subscription resets `page` to 0 on
  every new search term.
- Sort-header interaction (`onSortChange`) also resets `page` to 0.

### 13. `app-data-table` — New `shared/ui` Control

```
frontend/src/app/shared/ui/data-table/
  data-table.model.ts       — DataTableColumn<T>, DataTableSortEvent
  data-table.component.{ts,html,scss,spec.ts}
```

Wraps Angular Material's `mat-table` + `matSort` primitives. Has **no
knowledge of Customer or any other business domain** — it is driven entirely
by caller-supplied `DataTableColumn<T>[]` + `T[]` rows, and reports sort
requests upward via `(sortChange)` without sorting rows itself (server-side
sorting is owned by the consuming feature). Exported from `shared/ui`'s
barrel and demonstrated in `/ux-demo` with static fictional rows (no backend
call) per the M4.2 living-catalog rule.

### 14. Paginator — Raw Material, No Wrapper

`mat-paginator` is used directly in `CustomerListPageComponent` per M4.2's
rule: a wrapper is only justified when it captures LeaseDemo-specific
conventions, and plain Material paginator + `(page)` event handling
(`onPageChange`) required no such convention here. Every page-index/size
change re-issues a server request — Angular never re-paginates an
already-fetched larger page.

### 15. Loading / Empty / Error States

- **Loading**: `app-data-table`'s own `[loading]` input flag + a
  `role="status"` indicator; `viewState()` also drives disabling the
  results area during in-flight requests. Kept feature/component-local per
  M4.2's "don't overabstract" guidance — no `shared/ui/loading-state/` was
  introduced since a plain conditional message met the need.
- **Empty**: two distinct messages — `"No customers yet."` (empty database)
  vs `"No customers match your search."` (filtered search with zero
  matches) — determined by whether `searchControl.value` is non-blank when
  `totalElements === 0`.
- **Error**: a restrained message plus a real `<button>` "Retry" action
  (`retry()` re-emits the current search value, which is part of the
  composed pipeline and therefore re-issues the request). No backend
  stack traces/exception internals are ever shown.

Both empty/error states remained feature-local — no reusable
`shared/ui/empty-state|error-state` control emerged as clearly generalizable
from this single occurrence.

### 16. UX Demo Additions

`/ux-demo`'s "Data table" section renders `app-data-table` against the
existing static `DEMO_LEASE_CUSTOMERS` fictional dataset (unchanged from
M4.2) — no Customer backend call, no PostgreSQL, no Keycloak dependency.

### 17. Responsive / Accessibility

- `app-data-table`'s host scrolls horizontally (`overflow-x: auto`) with a
  `min-width` on the table, preserving all columns rather than silently
  dropping data at narrower widths.
- Sortable headers use Material's `mat-sort-header` (native ARIA sort-state
  announcement); the search `app-input` carries its own accessible
  `<mat-label>`; the error Retry action is a real `<button>`.

### 18. Backend Tests Added

`CustomerControllerTest` (+5): no-JWT → 401; CUSTOMER role → 403; ADMIN role
→ 200 with `peselEncrypted`/`peselLookup`/`keycloakUserId` asserted absent
from the JSON response; unsupported sort field → 400.

`CustomerServiceTest` (+6): default page/size + default sort; page-size
clamped to 100; negative page clamped to 0; supported sort field + `desc`
direction honored; unsupported sort field throws `InvalidSortFieldException`
without querying the repository; `Page<Customer>` content/metadata correctly
mapped into `PageResponse<CustomerListItemResponse>`.

**Backend result**: `mvn clean verify` → `BUILD SUCCESS`, 59/59 tests passing
(19 of which are the M4.4 additions above; the remaining 40 are pre-existing
M4.0–M4.3 suites, unaffected).

### 19. Frontend Tests Added

- `customer.service.spec.ts` — query-parameter construction (minimal vs.
  search+sort), typed `PageResponse<CustomerListItem>` round-trip, using
  `HttpTestingController`.
- `data-table.component.spec.ts` — renders supplied columns/rows, shows a
  loading indicator, emits `sortChange` on a sortable header interaction.
- `customer-list-page.component.spec.ts` — initial load, loading state,
  debounced search resets page to 0, page change issues a new server
  request, sort change issues a new server request and resets page,
  API error surfaces the error state, empty response surfaces the
  no-customers empty state.
- `ux-demo-page.component.spec.ts` — extended to assert the new
  `app-data-table` example renders its 3 static fictional rows.

**Frontend result**: `npm run test:ci` → `TOTAL: 86 SUCCESS` (0 failures).

### 20. Build / Bundle / Lint

- `npm run build` and `npm run build:prod` both succeed; no production
  budget warnings (`customer-list-page-component` lazy chunk: ~100.9 kB raw
  / ~22.6 kB estimated transfer — well within the existing lazy-chunk
  pattern).
- `npm run lint` reproduces the pre-existing, previously documented
  (M4.2/M4.3) infrastructure gap — `Could not find the
  '@angular-eslint/builder:lint' builder's node package.` — unrelated to
  M4.4 code and intentionally left unresolved per scope guardrails.

### 21. Local Validation

Verified via `mvn clean verify` (backend) and `npm run build` / `build:prod`
/ `test:ci` (frontend) rather than a live Keycloak ADMIN session — no
existing live Keycloak identity in this environment was confirmed to carry
the ADMIN realm role, and per the milestone's own scope guard, the Keycloak
realm was not modified to fabricate one. ADMIN/CUSTOMER/unauthenticated
authorization behavior is instead conclusively verified by
`CustomerControllerTest`'s three dedicated authorization tests (401/403/200)
against the real `SecurityConfig` bean via `@WebMvcTest` + `@Import(SecurityConfig.class)`.

### 22. Implemented vs. Deferred

**Implemented**: server-side paged/sorted/searched `GET /api/customers`;
ADMIN-only authorization; `PageResponse<T>`; `CustomerListItemResponse`;
sort allow-list; `app-data-table`; Admin Customer list page; reactive
search/page/sort query flow; UX Demo data-table example.

**Explicitly deferred** (per milestone scope): Customer detail page, editing,
deletion, Admin Customer creation UI, PESEL display, `/me`, lease
management, customer profile editing, advanced filters, CSV export, bulk
actions, caching/Redis, audit trail.

### 23. Interview Q&A

**Q: Why use server-side pagination instead of loading all Customers?**
A: The API and database remain scalable as the dataset grows, and the
browser only receives the rows needed for the current page.

**Q: Why not return Spring Page directly?**
A: An application-owned `PageResponse<T>` keeps the external API contract
stable and avoids coupling clients to Spring Data's own `Page` serialization
shape.

**Q: Why use switchMap for search?**
A: Newer search criteria supersede obsolete in-flight requests, preventing a
stale response from becoming the displayed result.

**Q: Why is the Admin route guard not sufficient security?**
A: Angular guards protect navigation/UX. Spring Security's declarative
`hasRole("ADMIN")` on `GET /api/customers` is the actual trust boundary for
the Customer data itself.

**Q: Why is PESEL absent from the Customer list DTO?**
A: The list view does not need it, and sensitive data should never cross an
API boundary without a concrete, present business requirement.

**Q: Why restrict sortable fields to an allow-list?**
A: The API owns its supported query contract instead of exposing arbitrary,
possibly internal, persistence property names supplied by the client.

**Q: Why JpaSpecificationExecutor instead of a derived query method or
QueryDSL?**
A: It is the smallest Spring Data mechanism that composes an optional
predicate with `Pageable` in one database query, without adding a new
dependency for a single, simple search requirement.

**Q: Why no new Flyway migration?**
A: The existing schema was inspected and found adequate at the current
~200-row demo scale; adding an index without a demonstrated need would have
been speculative, unjustified schema churn.

---

## M4.5 — Customer Onboarding & Self-Service Profile

### 1. Overview

M4.5 delivers the first real CUSTOMER self-service flow: a freshly
authenticated Keycloak CUSTOMER either lands on their existing profile, or
is routed through an onboarding form that persists a new `Customer` row
bound to their Keycloak identity.

```
KEYCLOAK CUSTOMER
       |
     JWT.sub
       |
       v
GET /api/customers/me
   /          \
 404          200
  |            |
  v            v
ONBOARDING   DASHBOARD / PROFILE
  |
PESEL/DOB/GENDER
VALIDATION
  |
  v
POST /customers
  |
  v
AES + HMAC
  |
  v
PostgreSQL
```

### 2. JWT.sub Identity Binding (unchanged, reinforced)

`POST /api/customers` already derived `keycloakUserId` exclusively from the
authenticated JWT `sub` claim (M4.1.2) — `CustomerController` never accepts
it from the request body, and `CustomerCreateRequest` has no such field.
M4.5 reuses this exact mechanism for `GET /api/customers/me`: both
endpoints resolve identity the same way, from
`@AuthenticationPrincipal Jwt jwt` → `jwt.getSubject()`. No customerId,
email, or username is ever used to select "whose" Customer profile is
being read or written.

### 3. GET /api/customers/me

New endpoint, `ROLE_CUSTOMER`-protected via
`@PreAuthorize("hasRole('CUSTOMER')")` on `CustomerController`:

| Condition                                   | Response |
|----------------------------------------------|----------|
| No JWT                                        | 401      |
| JWT present, no CUSTOMER authority            | 403      |
| CUSTOMER + no persisted Customer profile      | 404      |
| CUSTOMER + persisted Customer profile         | 200      |
| ADMIN + CUSTOMER (both authorities)           | 200 (CUSTOMER authority alone is sufficient) |

The lookup is `CustomerRepository.findByKeycloakUserId(String)` — a single,
indexed-by-uniqueness lookup (`customers.keycloak_user_id` is `UNIQUE`),
never an email or username lookup, and never a full-table scan.

A 404 is modeled as an expected condition — `CustomerProfileNotFoundException`
→ `GlobalExceptionHandler` → HTTP 404 with a `ProblemDetail` body — not a
server error. This lets Angular distinguish "authenticated, no profile yet"
(→ onboarding) from any genuine failure.

An `AccessDeniedException` handler was added to `GlobalExceptionHandler` so
that a `@PreAuthorize` denial (e.g. an ADMIN-only identity calling `/me`)
returns 403, not the generic 500 that the pre-existing catch-all
`Exception` handler would otherwise have produced.

### 4. CustomerProfileResponse DTO

A dedicated `CustomerProfileResponse` record (not the JPA entity, not
`CustomerResponse`) is returned by `/me`:

```
id, firstName, lastName, email, phoneNumber, dateOfBirth, gender, createdAt
```

Deliberately excluded:

- `pesel` (never accepted back from persistence in any form)
- `peselEncrypted`
- `peselLookup`
- `keycloakUserId` — the browser already knows its own JWT subject; this
  DTO plays no role in future identity resolution, so it is not echoed back

PESEL reveal is explicitly **FUTURE** — the profile page shows a
restrained `PESEL: Registered` status instead.

### 5. POST /api/customers — preserved, not duplicated

The existing creation endpoint already satisfied M4.5's security
requirements exactly (JWT-derived identity, no client-supplied
`keycloakUserId`/`customerId`, `DuplicateCustomerException` on repeat
identity or repeat PESEL). No second onboarding-specific endpoint was
introduced — Angular's onboarding form calls the same `POST /api/customers`.

### 6. PESEL Structural + Cross-Field Validation — reused, unchanged

`PeselValidator` (M4.1.2) already implemented every rule M4.5 asked for:

- exactly 11 digits
- checksum digit
- century/month decoding → valid calendar date
- cross-check against declared `dateOfBirth`
- cross-check against declared `gender` (10th digit parity: even = FEMALE,
  odd = MALE)

`CustomerService.createCustomer(...)` already called it, in this order:

```
existsByKeycloakUserId  →  peselValidator.validate(...)  →  existsByPeselLookup
    →  encrypt  →  persist
```

Cross-field mismatches throw `InvalidPeselException` before any lookup hash
is computed or any encryption/persistence occurs — invalid data can never
reach the database. No backend changes were required for validation order
or invariants; M4.5 confirmed this by inspection and by the existing
`CustomerServiceTest`/`PeselValidatorTest` suites, which continue to pass
unmodified.

### 7. AES vs HMAC — unchanged

- **AES-256-GCM** (`pesel_encrypted`): reversible confidentiality, for a
  legitimate future need to recover the plaintext PESEL.
- **HMAC-SHA-256** (`pesel_lookup`): deterministic keyed lookup/equality,
  used for uniqueness without ever decrypting existing rows.

M4.5 introduces no new PESEL storage or cryptography — `/me` never returns
either derived value, and onboarding's `POST` flows through the exact same
`CustomerService.createCustomer` path as before.

### 8. Frontend: CurrentCustomerService (single source of truth)

`features/customer/profile/services/current-customer.service.ts` is the
**only** place in the frontend that calls `GET /api/customers/me`. It
exposes a small discriminated-union state:

```ts
type CurrentCustomerState =
  | { status: 'loading' }
  | { status: 'found'; profile: CustomerProfile }
  | { status: 'missing' }
  | { status: 'error'; message: string };
```

Route guards, the dashboard, and the profile page all read `state()` /
`profile()` rather than issuing their own HTTP calls. `createProfile(...)`
(the onboarding `POST`) updates the same cached state on success, so the
dashboard/profile page reflect the newly persisted Customer without a full
page reload. No NgRx — a single `providedIn: 'root'` service with signals
was sufficient.

### 9. Onboarding Routing / Guards

Two small, focused guards in
`features/customer/profile/guards/customer-profile.guard.ts`:

- `requireCustomerProfileGuard` — applied to `dashboard`, `profile`,
  `leases`, `documents`. Redirects to `/customer/onboarding` when the
  profile is `missing`. Passes through on `error` (a transient backend
  hiccup must not trap the user in a redirect they cannot escape).
- `onboardingGuard` — applied to `onboarding` itself. Redirects to
  `/customer/dashboard` when the profile is already `found` (onboarding
  must never be shown twice).

These two guards redirect in strictly opposite, non-overlapping
directions, so no redirect loop is possible by construction.

### 10. Onboarding Page

`/customer/onboarding` — a genuine Reactive Forms page (not
template-driven), built from `shared/ui` (`app-input`, `app-button`,
`app-card`, `app-page-header`) plus Angular Material's `MatSelectModule`
and `MatDatepickerModule` directly (no new shared/ui wrapper was
introduced — a single onboarding-only date/select field did not meet the
"genuine reusable value" bar from M4.2/M4.5 guidance).

Two sections: **Personal information** (first/last name, date of birth,
gender, PESEL) and **Contact information** (email, phone). PESEL is bound
as plain text (`app-input`), never parsed as a number, never logged, never
placed in a URL/query param, and never written to `localStorage`/
`sessionStorage`.

### 11. Frontend PESEL Validator (UX only)

`features/customer/profile/validators/pesel.validator.ts` mirrors the
backend `PeselValidator` algorithm exactly (11-digit format, checksum,
century/month decoding, gender parity) so the user gets instant feedback:

- `peselChecksumValidator()` — control-level: format + checksum.
- `peselCrossFieldValidator(pesel, dateOfBirth, gender)` — group-level:
  PESEL-encoded DOB/gender vs the declared form values.

**This is UX only.** A malicious or buggy client can call
`POST /api/customers` directly, bypassing Angular entirely — `PeselValidator`
(Java) inside `CustomerService` remains the sole authoritative trust
boundary. The onboarding page's doc comment states this explicitly.

User-facing messages are deliberately generic and non-technical:

- "PESEL must be exactly 11 digits."
- "Invalid PESEL checksum."
- "PESEL birth date does not match the selected date of birth."
- "PESEL gender does not match the selected gender."

No message exposes implementation details like "digit index 9 parity".

### 12. Server Error Mapping

The onboarding page maps backend failures into safe, generic messages —
never raw `ProblemDetail`/exception text:

| Backend status | Displayed message |
|-----------------|--------------------|
| 409 (duplicate identity or PESEL) | "A profile already exists for your account, or this PESEL is already registered." |
| 400 (validation, incl. cross-field) | "Some of the information provided is invalid. Please review the form and try again." |
| 401 | "Your session has expired. Please sign in again." |
| other | "Something went wrong while completing your profile. Please try again later." |

Angular validation passing does not guarantee the backend will accept the
request — the 400 path above is exercised in tests even though the same
form-level validators normally prevent submission first.

### 13. Profile Page & Dashboard

`/customer/profile` was upgraded from a placeholder to a real read-only
page rendering `firstName`, `lastName`, `email`, `phoneNumber`,
`dateOfBirth`, `gender`, and a restrained `PESEL: Registered` line —
sourced from the already-loaded `CurrentCustomerService` cache (no
duplicate `/me` call).

The Customer Dashboard now reads the same cached profile to show
`Welcome, <firstName>` when available, falling back to a generic subtitle
otherwise. No lease counts or financial metrics were added — leases remain
unimplemented.

### 14. Subsequent-Login Behavior

Because identity resolution is exclusively `JWT.sub → keycloak_user_id`:

1. CUSTOMER logs in, `GET /me` → 404 → onboarding → `POST /customers` →
   Customer persisted.
2. CUSTOMER logs out, logs back in with the same Keycloak account.
3. `GET /me` → 200, returning the same row — `onboardingGuard` redirects
   away from `/customer/onboarding` immediately, and
   `requireCustomerProfileGuard` allows `/customer/dashboard` /
   `/customer/profile` directly.

No additional Customer row is ever created for the same `sub` — the
`customers.keycloak_user_id UNIQUE` constraint remains the authoritative,
concurrency-safe guarantee (M4.1.2), and `existsByKeycloakUserId` provides
the friendly early `DuplicateCustomerException` (409) if a second `POST` is
attempted.

### 15. Real Customer vs Mock Customer Separation

The real local Keycloak CUSTOMER identity was never attached to any
existing `mock-user-*` row. Onboarding always inserts a brand-new
`Customer` row with `keycloak_user_id` equal to the authenticated
`sub`. The ~200 mock rows are untouched. The Admin Customer list
(`GET /api/customers`, M4.4) naturally shows the new Customer once
persisted — no separate integration code was needed, since both flows
share the same `customers` table.

### 16. FUTURE — PATCH /api/customers/me (NOT implemented)

Documented design only:

- `PATCH /api/customers/me` would allow the authenticated CUSTOMER to
  update selected mutable fields (e.g. phone number, email).
- Ordinary mutable-field updates would **not** require resending the
  PESEL.
- If a future operation needs PESEL re-verification, the supplied
  plaintext PESEL would be HMAC-SHA-256'd with the existing server-held
  HMAC key and compared against the stored `pesel_lookup` — never by
  decrypting AES ciphertext merely to check equality, and never by
  comparing plaintext against the HMAC string directly.
- If `dateOfBirth`/`gender` ever become editable, the backend would need
  to re-validate consistency against the existing PESEL — e.g. by
  decrypting the stored PESEL (`AesGcmEncryptionService`) and re-running
  `PeselValidator`, or requiring a dedicated, explicitly verified
  sensitive-data update flow. This is FUTURE scope only.

### 17. Backend Tests Added (M4.5)

`CustomerControllerTest`:
- `GET /me` — no JWT → 401
- `GET /me` — CUSTOMER + matching record → 200, response excludes
  `peselEncrypted`/`peselLookup`/`pesel`/`keycloakUserId`
- `GET /me` — CUSTOMER without a record → 404
- `GET /me` — ADMIN-only → 403
- `GET /me` — ADMIN+CUSTOMER → 200

`CustomerServiceTest`:
- `getCurrentCustomerProfile` resolves by `keycloakUserId` (JWT.sub)
- `getCurrentCustomerProfile` throws `CustomerProfileNotFoundException`
  when no matching Customer exists

Pre-existing `PeselValidatorTest` and PESEL/DOB/gender cross-field
coverage in `CustomerServiceTest` were reused unmodified — M4.5 did not
duplicate assertions that already existed at the validator/service layer.
All 66 backend tests pass (`mvn clean test`).

### 18. Frontend Tests Added (M4.5)

- `CurrentCustomerService`: initial loading state, 200 → found, 404 →
  missing (not error), other errors → error state, `createProfile` posts
  without `keycloakUserId` and updates cached state
- `pesel.validator`: format/checksum errors, valid PESEL (MALE and
  FEMALE), DOB cross-field match/mismatch, gender cross-field
  match/mismatch, structural errors suppress cross-field evaluation
- `customer-profile.guard`: missing → onboarding redirect, found → allowed,
  error → allowed through; onboarding found → dashboard redirect, missing →
  allowed
- `CustomerOnboardingPageComponent`: required-field validation, email
  format, PESEL format/checksum, DOB match/mismatch, gender
  match/mismatch, invalid form blocks submission, valid form submits
  without `keycloakUserId` and navigates to the dashboard, safe message on
  409/400 backend responses
- `CustomerProfilePageComponent`: renders persisted fields, never renders
  a plaintext PESEL value, fallback message when profile is unavailable
- `CustomerDashboardPageComponent`: greets by first name when a profile is
  loaded, generic subtitle otherwise

All 123 frontend tests pass (`npm run test:ci`).

### 19. Interview Q&A (M4.5)

**Q: Why use /api/customers/me instead of /api/customers/{id} for self-service?**
A: The server derives ownership from the authenticated JWT subject, so the
browser never chooses which Customer identity it is allowed to access.

**Q: Why validate PESEL against dateOfBirth and gender?**
A: A structurally valid PESEL can still contradict other Customer data.
Cross-field validation preserves domain consistency.

**Q: Why validate in Angular and Spring?**
A: Angular validation improves UX, but it can be bypassed. Spring remains
the authoritative trust boundary.

**Q: Why use JWT.sub rather than email?**
A: The subject is the stable identity-provider identifier. Email is
mutable business/profile data and should not define ownership.

**Q: Why store both encrypted PESEL and HMAC lookup?**
A: Encryption provides confidentiality and recoverability, while HMAC
provides deterministic keyed equality/uniqueness checks without decrypting
every row.

**Q: How would future PESEL verification work?**
A: HMAC the supplied value using the server-held HMAC key and compare it
with the stored lookup value using a safe equality mechanism.

**Q: What happens after logout and login?**
A: The same Keycloak subject resolves to the same persisted Customer row,
so the existing profile is restored rather than onboarding again.

**Q: Why does a 404 from /me not indicate an error?**
A: For a freshly authenticated CUSTOMER who has never onboarded, "no
Customer profile yet" is an expected, first-class application state — not
a server failure — so it is modeled as a distinct `missing` state in
`CurrentCustomerService`, not merged into the generic `error` state.

### 20. Scope Confirmation (M4.5)

NOT implemented, as scoped:

- Profile UPDATE/PATCH (`PATCH /api/customers/me`) — documented as FUTURE
  only, section 16.
- Customer deletion.
- PESEL reveal/display anywhere in the frontend.
- Leases, Documents (still placeholders under `requireCustomerProfileGuard`).
- NgRx, Redis, another UI framework, Storybook.
- Any change to Keycloak realm/client configuration.
- Any change to AES/HMAC algorithms or crypto keys.
- Any new Flyway migration — the existing schema (`keycloak_user_id`
  `UNIQUE`, `pesel_encrypted`, `pesel_lookup`) already supported this flow.
- Attaching the real Keycloak CUSTOMER identity to any `mock-user-*` row.


