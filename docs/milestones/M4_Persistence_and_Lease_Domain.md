# M4 — Persistence & Lease Domain

M4 introduces durable business persistence to LeaseDemo and, over its
sub-milestones, the LeaseDemo domain model itself (customers, lease
applications, etc.).

This is a **living document** for the entire M4 milestone. Each
sub-milestone extends this same file instead of creating a new
document. Sections are explicitly marked as **IMPLEMENTED** or
**PLANNED / FUTURE ARCHITECTURE** — never assume a "PLANNED" section
already exists in code.

Current implemented scope: **M4.0 only** (persistence foundation, no
business entities).

Future planned scope: M4.1 (Customer), M4.2 (LeaseApplication), and
beyond.

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

Conceptual future flow (once a `Customer` entity exists):

```
Customer Java object
        |
        v
JpaRepository
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

`Customer` **does not exist yet** in M4.0 — this diagram documents the
planned shape of persistence once M4.1 introduces it.

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

**Actual M4.0 state** (verified at runtime):

- `flyway_schema_history` table **exists**.
- **0 migrations executed** — `db/migration` is currently empty.
- No `V1` migration exists yet.
- No business tables exist.

The first real migration is intentionally reserved for M4.1:
`V1__create_customer_table.sql`.

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

`ddl-auto: validate` currently succeeds trivially: M4.0 has **zero
`@Entity` classes**, so Hibernate has no business mappings to validate
against the (empty) schema. This will become a meaningful check once
M4.1 introduces `Customer` and a matching `V1` migration — validation
will then fail if the entity and the migrated table diverge.

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

Rationale: M4.0 has no entities or repositories, so forcing every
`@SpringBootTest`/`@WebMvcTest` in the general suite to require a live
PostgreSQL server would add an environment dependency (and CI
fragility) for zero test value today.

**This strategy must be revisited once real persistence behavior is
introduced.** Likely future approaches (not implemented yet):

- `@DataJpaTest` for repository-level tests.
- Testcontainers-backed PostgreSQL for integration tests that need a
  real database engine.

### 10. Current Database State (verified)

| Item                        | Value |
|------------------------------|-------|
| Database                     | `lease_demo` |
| Schema                       | `public` |
| Current tables                | `flyway_schema_history` |
| Rows in `flyway_schema_history` | `0` |
| Business tables               | none |
| Hibernate-created tables       | none |

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
| M4.1 | Customer Domain | PLANNED |
| M4.2 | LeaseApplication Domain | PLANNED |

No implementation details beyond high-level intent are asserted for
M4.1/M4.2 here; they will be documented in this same file as they are
built.

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
