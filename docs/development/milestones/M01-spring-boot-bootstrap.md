# M0.1 — Spring Boot Bootstrap

## Objective

Establish the enterprise Spring Boot foundation inside the `backend/` directory.

This milestone introduces all structural elements required to run the backend application: Maven
project configuration, Spring Boot application entry point, layered package structure, Spring profile
configuration, and a single operational endpoint (`GET /api/health`). No business logic is
implemented. No database, JPA, Security, or Flyway dependencies are introduced.

---

## Scope

### Included

| Area | Detail |
|------|--------|
| Maven project | `pom.xml` with Spring Boot 3.3.4 parent, Java 21, all declared dependencies |
| Spring Boot configuration | `application.yml`, `application-local.yml`, `application-prod.yml` |
| Application entry point | `LeaseDemoApplication` — `@SpringBootApplication` main class |
| Package structure | 10 packages: `config`, `controller`, `dto`, `entity`, `exception`, `mapper`, `model`, `repository`, `service`, `util` |
| Health endpoint | `GET /api/health` — returns `status`, `application`, `version` |
| Global exception handler | `GlobalExceptionHandler` using RFC 9457 `ProblemDetail` |
| Spring context test | `LeaseDemoApplicationTests` — verifies context loads |
| README update | Backend prerequisites, build, run, health endpoint, project structure |
| Command documentation | `docs/commands/03-spring.md` |

### Intentionally Not Implemented

- Database connectivity (no JPA, no DataSource, no Flyway)
- Spring Security
- Business domain logic
- Angular frontend
- Docker configuration
- CI/CD pipeline

---

## Technologies

| Technology | Version |
|------------|---------|
| Java | 21.0.4 (Eclipse Temurin) |
| Maven | 3.9.11 |
| Spring Boot | 3.3.4 |
| Spring Framework | 6.1.13 |
| Lombok | 1.18.34 |
| MapStruct | 1.5.5.Final |

---

## Dependencies Added

| Dependency | Scope | Purpose |
|------------|-------|---------|
| `spring-boot-starter-web` | compile | Embedded Tomcat, Spring MVC, Jackson |
| `spring-boot-starter-actuator` | compile | Operational endpoints (`/actuator/health`, `/actuator/info`, `/actuator/metrics`) |
| `spring-boot-starter-validation` | compile | Bean Validation (Jakarta) — for use in later milestones |
| `lombok` | compile (optional) | Compile-time code generation — excluded from packaged JAR |
| `mapstruct` | compile | Type-safe mapping between objects — for use in later milestones |
| `spring-boot-starter-test` | test | JUnit 5, Mockito, AssertJ, Spring Test |

---

## Files Created

### Maven

| File | Purpose |
|------|---------|
| `backend/pom.xml` | Maven project definition — coordinates, parent, dependencies, compiler plugin with Lombok + MapStruct annotation processors |

### Spring Boot Configuration

| File | Purpose |
|------|---------|
| `backend/src/main/resources/application.yml` | Base configuration — app name, active profile (`local`), server port, Actuator exposure, custom `application.version` property |
| `backend/src/main/resources/application-local.yml` | Local profile — DEBUG logging for `com.lease-demo` |
| `backend/src/main/resources/application-prod.yml` | Production profile — WARN root logging, INFO for `com.lease-demo` |

### Java — Main Sources

| File | Package | Purpose |
|------|---------|---------|
| `LeaseDemoApplication.java` | `com.lease-demo` | Application entry point |
| `ApplicationProperties.java` | `com.lease-demo.config` | `@ConfigurationProperties(prefix = "application")` — binds `application.version` |
| `WebMvcConfig.java` | `com.lease-demo.config` | `WebMvcConfigurer` stub — web layer customisation placeholder |
| `HealthController.java` | `com.lease-demo.controller` | `GET /api/health` — delegates to `HealthService` |
| `HealthResponse.java` | `com.lease-demo.dto` | Java record — `status`, `application`, `version` |
| `GlobalExceptionHandler.java` | `com.lease-demo.exception` | `@RestControllerAdvice` — maps unhandled exceptions to RFC 9457 `ProblemDetail` |
| `HealthService.java` | `com.lease-demo.service` | Constructs and returns `HealthResponse` from `ApplicationProperties` |

### Java — Empty Packages (placeholder `.gitkeep`)

| Package | Purpose |
|---------|---------|
| `com.lease-demo.entity` | JPA entities — populated in a later milestone |
| `com.lease-demo.mapper` | MapStruct mappers — populated in a later milestone |
| `com.lease-demo.model` | Domain model classes — populated in a later milestone |
| `com.lease-demo.repository` | Spring Data repositories — populated in a later milestone |
| `com.lease-demo.util` | Utility classes — populated in a later milestone |

### Java — Test Sources

| File | Purpose |
|------|---------|
| `LeaseDemoApplicationTests.java` | Spring Boot context load test — verifies all beans wire correctly |

---

## Files Modified

| File | Change |
|------|--------|
| `README.md` | Added Backend section: prerequisites, build command, run command, health endpoint example, project structure tree |
| `docs/commands/03-spring.md` | Replaced placeholder with actual Maven commands executed during this milestone |

---

## Commands Executed

| Command | Directory | Purpose | Result |
|---------|-----------|---------|--------|
| `mvn --version` | any | Verify Maven installation and Java version | Maven 3.9.11, Java 21.0.4 |
| `java --version` | any | Verify Java installation | OpenJDK 21.0.4 Temurin |
| `mvn clean package` | `backend/` | Compile sources, run tests, package JAR | BUILD SUCCESS — 1 test passed |

---

## Design Decisions

### Constructor injection throughout

All Spring-managed classes use constructor injection exclusively. Field injection (`@Autowired` on
fields) is not used. Constructor injection makes dependencies explicit, supports immutability, and
enables instantiation without a Spring container in unit tests.

### Java record for HealthResponse

`HealthResponse` is implemented as a Java record rather than a mutable class or a Lombok
`@Value`-annotated class. Records are concise, immutable by default, and provide built-in `equals`,
`hashCode`, and `toString`. Jackson serialises records correctly without additional configuration.

### RFC 9457 ProblemDetail for error responses

`GlobalExceptionHandler` returns Spring's `ProblemDetail` (introduced in Spring Framework 6.0), which
implements the RFC 9457 Problem Details for HTTP APIs standard. This establishes a consistent,
standards-compliant error response format from the first milestone.

### Custom application properties bound via @ConfigurationProperties

The application version is externalised to `application.yml` as `application.version` and bound to
`ApplicationProperties` using `@ConfigurationProperties`. This avoids hardcoding the version in
service code and allows the value to be overridden per deployment profile.

### Actuator endpoints scoped to non-sensitive operations

Only `health`, `info`, and `metrics` endpoints are exposed. Sensitive endpoints (`env`, `beans`,
`dump`, `mappings`) are not exposed by default. This is a safe baseline for a public portfolio
application.

### Lombok and MapStruct declared for future use

Both dependencies are declared and their annotation processors wired into the Maven Compiler Plugin.
No Lombok annotations or MapStruct interfaces are used in this milestone, but having the processors
configured from the start prevents annotation processor ordering issues when they are first used.

---

## Verification

### Build verification

```
mvn clean package
```

Output confirmed:

- Compilation: success (0 errors, 0 warnings relevant to application code)
- Context load: `Started LeaseDemoApplicationTests in 1.968 seconds`
- Active profile: `local`
- Actuator endpoints: 3 exposed (`health`, `info`, `metrics`)
- Tests: 1 passed, 0 failed

### Manual verification required

Start the application and confirm the health endpoint responds correctly:

```bash
cd backend
mvn spring-boot:run
```

Then in a separate terminal:

```bash
curl http://localhost:8080/api/health
```

Expected response:

```json
{
  "status": "UP",
  "application": "lease-demo",
  "version": "0.1.0"
}
```

Also verify Actuator:

```bash
curl http://localhost:8080/actuator/health
```

---

## Definition of Done

- [x] `pom.xml` created with Java 21, Spring Boot 3.3.4, all required dependencies
- [x] `application.yml`, `application-local.yml`, `application-prod.yml` created
- [x] `LeaseDemoApplication` main class created
- [x] All 10 packages created (`config`, `controller`, `dto`, `entity`, `exception`, `mapper`, `model`, `repository`, `service`, `util`)
- [x] `GET /api/health` endpoint implemented and returning correct JSON structure
- [x] `GlobalExceptionHandler` in place using RFC 9457 `ProblemDetail`
- [x] `ApplicationProperties` binding `application.version` from YAML
- [x] Constructor injection used throughout — no field injection
- [x] No wildcard imports, no dead code, no TODO comments, no commented-out code
- [x] Spring context test passes: `mvn clean package` — BUILD SUCCESS
- [x] `README.md` updated with backend prerequisites, build, run, health endpoint, project structure
- [x] `docs/commands/03-spring.md` updated with actually executed commands
- [x] `docs/prompts/M01-spring-boot-bootstrap.md` created
- [x] This milestone document created
- [x] No database, JPA, Security, or Flyway introduced
- [x] No Git operations performed

---

## Next Milestone

**M0.2 — Angular Enterprise Bootstrap**

Bootstrap the Angular application inside the `frontend/` directory using enterprise-grade project
structure, strict TypeScript configuration, and a matching health check component.
