# lease-demo

Purpose-built technical interview demo showcasing end-to-end enterprise application development with Angular, Spring Boot, PostgreSQL, Docker, Cloud Run, REST APIs, SSE, testing and clean architecture.

---

## Project Overview

Purpose-built technical interview demo showcasing end-to-end enterprise application development.

The application implements an **Exchange Rate** service built with Angular, Spring Boot, PostgreSQL, Docker, and Google Cloud Run.

## Architecture

Three-tier enterprise architecture:

- **Frontend** — Angular 18 SPA (standalone components, signals, lazy loading)
- **Backend** — Spring Boot 3.x REST API
- **Database** — PostgreSQL (future milestone)
- **Infrastructure** — Docker Compose (local), Google Cloud Run (production)

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 18, TypeScript 5.4, RxJS 7.8 |
| Backend | Java 21, Spring Boot 3.x, Maven |
| Database | PostgreSQL (future milestone) |
| Containerisation | Docker, Docker Compose |
| Cloud | Google Cloud Run |
| CI/CD | GitHub Actions (future milestone) |

## Folder Structure

```
lease-demo/
├── frontend/          # Angular 18 SPA
├── backend/           # Spring Boot 3.x REST API
├── docker/            # Docker Compose configuration
├── scripts/           # Build, deploy and maintenance scripts
└── docs/              # Architecture docs, ADRs, guides, milestones
```

## Development

### Backend

#### Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 (Eclipse Temurin recommended) |
| Maven | 3.9+ |

#### Build

```bash
cd backend
mvn clean package
```

#### Run

```bash
cd backend
mvn spring-boot:run
```

The application starts on `http://localhost:8080` with the `local` profile active.

#### Health endpoint

```
GET http://localhost:8080/api/health
```

#### Exchange rates endpoint

```
GET http://localhost:8080/api/exchange-rates
```

Returns current NBP Table A exchange rates. Fetches live data from the Polish National Bank (NBP) public API.

Example response:

```json
{
  "tableNo": "150/A/NBP/2025",
  "effectiveDate": "2025-08-01",
  "rates": [
    { "code": "USD", "name": "dolar amerykański", "midRate": 3.9245 }
  ]
}
```

#### Health endpoint response example

```json
{
  "status": "UP",
  "application": "lease-demo",
  "version": "0.1.0"
}
```

Spring Boot Actuator is also available at:

```
GET http://localhost:8080/actuator/health
```

#### Project structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/lease-demo
│   │   │   ├── LeaseDemoApplication.java   # Application entry point
│   │   │   ├── config/                           # Spring configuration classes
│   │   │   ├── controller/                       # REST controllers
│   │   │   ├── dto/                              # Data Transfer Objects
│   │   │   ├── entity/                           # JPA entities (future)
│   │   │   ├── exception/                        # Global exception handling
│   │   │   ├── mapper/                           # MapStruct mappers (future)
│   │   │   ├── model/                            # Domain model classes (future)
│   │   │   ├── repository/                       # Spring Data repositories (future)
│   │   │   ├── service/                          # Business logic services
│   │   │   └── util/                             # Utility classes (future)
│   │   └── resources/
│   │       ├── application.yml                   # Base configuration
│   │       ├── application-local.yml             # Local profile
│   │       └── application-prod.yml              # Production profile
│   └── test/
│       └── java/com/lease-demo
│           └── LeaseDemoApplicationTests.java
└── pom.xml
```
### Frontend

#### Prerequisites

| Tool | Version |
|---|---|
| Node.js | 20.x |
| npm | 10.x |

#### Install

```bash
cd frontend
npm install
```

#### Run (development server)

```bash
cd frontend
npm start
```

Application starts at: `http://localhost:4200`

The Angular development proxy routes `/api/*` → `http://localhost:8080` automatically.
Start the backend before the frontend.

#### Build

```bash
cd frontend
npx ng build --configuration production
```

#### Project structure

```
frontend/src/app/
├── core/           # Singleton infrastructure (ApiService, interceptors, guards)
├── shared/         # Reusable UI components, pipes, directives
├── features/       # Business feature modules (lazy-loaded)
└── layouts/        # Application layout wrappers
```

See `docs/commands/02-angular.md` for full command reference.

See `docs/development/milestones/M1.0-angular-foundation.md` for architecture details.

See `docs/development/milestones/M1.1-frontend-backend-integration.md` for integration details.

See `docs/development/milestones/M2.0-exchange-rates.md` for exchange rates feature details.

## Deployment

<!-- TODO: Describe local Docker Compose deployment and Cloud Run deployment. -->

## Documentation

<!-- TODO: Link to all documents under docs/ (architecture, commands, guides, ADRs, interview notes). -->


## Local Development Infrastructure

LeaseDemo local environment is designed as a small distributed-systems laboratory.
Angular and Spring Boot can run directly on the host during development, while
infrastructure services run as Docker containers.

### Application

| Component | Runtime | Host Port | Purpose |
|---|---|---:|---|
| Angular Frontend | Local process | `4200` | Admin and Customer web application |
| Spring Boot Backend | Local process | `8080` | REST API, business logic, security and integration layer |

### Infrastructure Containers

| # | Container | Host Port | Purpose |
|---:|---|---:|---|
| 1 | `lease-demo-keycloak` | `8081` | Authentication, OAuth2/OIDC, JWT, ADMIN/CUSTOMER roles |
| 2 | `lease-demo-postgres-pl` | `5433` | Polish PostgreSQL business database |
| 3 | `lease-demo-postgres-de` | `5434` | German PostgreSQL business database / replication node |
| 4 | `lease-demo-redis-shared` | `6379` | Distributed cache and shared ephemeral state |
| 5 | `lease-demo-kafka-events` | `9092` | Domain event streaming |
| 6 | `lease-demo-kafka-connect-cdc` | `8083` | Kafka Connect + Debezium PostgreSQL CDC |
| 7 | `lease-demo-elasticsearch-eu-search` | `9200` | Shared EU search/read model |
| 8 | `lease-demo-mailpit` | `8025` UI / `1025` SMTP | Local email testing |
| 9 | `lease-demo-kafka-ui` | `8090` | Kafka topics, partitions, messages and consumer monitoring |

### Data Flow

```text
                         Angular
                            |
                            v
                       Spring Boot
                            |
          +-----------------+------------------+
          |                 |                  |
          v                 v                  v
        Redis         PostgreSQL PL       External APIs
                           |
                           | Spock
                           v
                     PostgreSQL DE

PostgreSQL / outbox_events
          |
          | WAL / CDC
          v
Kafka Connect + Debezium
          |
          v
        Kafka
          |
     +----+----------------+
     |                     |
     v                     v
Search Projection      Async Consumers
     |                     |
     v                     +--> PDF generation
Elasticsearch               +--> Notifications
                            |
                            v
                          Mailpit

```
## Infrastructure Responsibilities
### PostgreSQL

PostgreSQL is the source of truth for durable business data:

- customers
- lease applications
- consents
- assessments
- documents
- transactional outbox events

postgres-pl and postgres-de are separate PostgreSQL instances used to
demonstrate logical replication and eventually active-active replication with
pgEdge Spock.

### Redis

Redis is used for shared short-lived state and distributed caching.

Example use cases:

- unfinished lease application drafts
- shared state between stateless Spring Boot instances
- cached NBP/FX exchange rates
- TTL-based temporary data

### Kafka

Kafka stores and distributes domain events such as:

- LeaseApplicationSubmitted
- AssessmentCompleted
- DocumentGenerated
- ContractSigned

Ordering is guaranteed within a Kafka partition.

### Debezium / Kafka Connect

Debezium performs Change Data Capture (CDC) from PostgreSQL WAL.

The primary LeaseDemo use case is:
```text
PostgreSQL outbox_events
        |
        v
     Debezium
        |
        v
      Kafka
```
This allows committed transactional outbox events to be published to Kafka
without performing a direct dual-write from Spring Boot.

### Elasticsearch

Elasticsearch contains a denormalized search/read model built from regional
business data.

PostgreSQL remains the source of truth.

Example global search:

PL -> Jan Kowalski -> Fiat Tipo
DE -> Hans Müller  -> Fiat Tipo

Search: "Fiat Tipo"
-> results from both regions

### Mailpit

Mailpit provides a local SMTP server and browser inbox for testing asynchronous
email workflows without sending real email.

### Kafka UI

Kafka UI is used during development and demos to inspect:

- topics
- partitions
- messages
- consumer groups
- consumer lag



```md
## Development Scripts

### Main environment scripts

| Script | Purpose |
|---|---|
| `scripts/deploy-local.sh` | Starts the LeaseDemo development application: local infrastructure, Spring Boot and Angular |
| `scripts/infrastructure-start.sh` | Starts Docker infrastructure services |
| `scripts/infrastructure-stop.sh` | Stops Docker infrastructure services without removing persistent volumes |
| `scripts/infrastructure-status.sh` | Displays infrastructure container status |
| `scripts/build.sh` | Builds the project |
| `scripts/test.sh` | Runs project tests |
| `scripts/lint.sh` | Runs linting |
| `scripts/format.sh` | Runs source formatting |
| `scripts/clean.sh` | Cleans generated/build artifacts |
| `scripts/deploy-cloud-run.sh` | Cloud Run deployment workflow |

### Database utilities

| Script | Purpose |
|---|---|
| `scripts/postgres-reset.sh` | Resets local PostgreSQL data. Destructive operation |
| `scripts/postgres-seed-local-data.sh` | Loads deterministic local demo/customer data |
| `scripts/generate-customer-mock-data.py` | Generates deterministic mock Customer data used by local seeding |

### Keycloak utilities

| Script | Purpose |
|---|---|
| `scripts/keycloak-logs.sh` | Follows Keycloak container logs |

### Security utilities

| Script | Purpose |
|---|---|
| `scripts/lib/local-crypto-keys.sh` | Loads persistent local AES/HMAC keys used for sensitive field protection |
| `scripts/lib/local-crypto-keys.test.sh` | Tests local crypto-key handling |

### Demo scenarios

| Script | Demonstrates |
|---|---|
| `scripts/demo/demo-redis.sh` | Shared lease draft surviving backend instance restart/change |
| `scripts/demo/demo-redis-fx.sh` | Redis FX cache: cache MISS -> external API -> cache HIT |
| `scripts/demo/demo-kafka-recovery.sh` | Kafka outage, Debezium backlog recovery and asynchronous processing |
| `scripts/demo/demo-spock.sh` | PostgreSQL PL <-> DE replication |
| `scripts/demo/demo-elasticsearch.sh` | Cross-region Elasticsearch search over PL and DE data |
Jeszcze jedna rzecz

Na samym końcu tej sekcji dałbym też bardzo krótkie:

### Useful Local URLs

- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Keycloak: http://localhost:8081
- Kafka UI: http://localhost:8090
- Mailpit: http://localhost:8025
- Elasticsearch: http://localhost:9200


