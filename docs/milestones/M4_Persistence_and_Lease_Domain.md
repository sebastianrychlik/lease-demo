# M4 — Persistence & Lease Domain

M4 introduces durable business persistence to LeaseDemo and, over its
sub-milestones, the LeaseDemo domain model itself (customers, lease
applications, etc.).

This is a **living document** for the entire M4 milestone. Each
sub-milestone extends this same file instead of creating a new
document. Sections are explicitly marked as **IMPLEMENTED** or
**PLANNED / FUTURE ARCHITECTURE** — never assume a "PLANNED" section
already exists in code.

Current implemented scope: **M4.0, M4.1, and M4.1.1** (persistence
foundation, the initial `Customer` domain, and OpenAPI/Swagger UI
documentation).

Future planned scope: M4.2 (LeaseApplication) and beyond.

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
| M4.2 | LeaseApplication Domain | PLANNED |

No implementation details beyond high-level intent are asserted for
M4.2 here; it will be documented in this same file as it is built.

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

For local convenience, `scripts/deploy-local.sh` generates ephemeral
session-only keys with `openssl rand -base64 32` if
`CRYPTO_AES_KEY`/`CRYPTO_HMAC_KEY` are not already set in the shell
environment, and never prints them.

**Explicit caveat:** if new random keys are generated while the
persistent local PostgreSQL volume (`lease-demo-postgres-data`) still
contains `Customer` rows created under previous keys:

- existing `pesel_encrypted` values cannot be decrypted with the new
  AES key,
- the same PESEL now produces a **different** `pesel_lookup` under the
  new HMAC key, so duplicate-detection/lookup behavior becomes
  inconsistent across sessions.

Ephemeral generated keys are therefore acceptable **only** for
disposable local data/testing. Once persistent seeded local `Customer`
data is introduced, local development keys should become **stable
across restarts** while still staying outside Git — e.g. via a
developer-local, git-ignored environment/secret file, or another local
secret mechanism. This is a local-development-only consideration, not
a change to the production model in §12.

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
| Testcontainers-based repository tests | **NOT IMPLEMENTED** |
| Managed PostgreSQL (production) | **FUTURE** |
| Production KMS/Secret Manager | **FUTURE** |
| Read replicas | **FUTURE** |
| PITR | **FUTURE** |
| BIK/KRD integration | **FUTURE** |
| Credit scoring | **FUTURE** |
| PDF generation | **FUTURE** |
| E-signature | **FUTURE** |
