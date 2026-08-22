# luxsoft-dxc-demo

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
luxsoft-dxc-demo/
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
  "application": "luxsoft-dxc-demo",
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
│   │   ├── java/com/luxsoft/dxc/
│   │   │   ├── LuxsoftDxcDemoApplication.java   # Application entry point
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
│       └── java/com/luxsoft/dxc/
│           └── LuxsoftDxcDemoApplicationTests.java
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
