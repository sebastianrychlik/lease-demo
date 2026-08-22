# M0.1 — Spring Boot Bootstrap — Implementation Prompt

## ROLE

You are a Senior Java Architect and Spring Boot Engineer.

Your task is to implement milestone M0.1 of the LeaseDemo project.

This repository is intended to become a public GitHub portfolio project
demonstrating professional enterprise development practices.

Implement production-quality code.

## OBJECTIVE

Create the enterprise Spring Boot foundation.

This milestone establishes the backend architecture only.

Do NOT implement any business logic.

## TECH STACK

- Java 21
- Spring Boot 3.x
- Maven
- Spring Web
- Spring Boot Actuator
- Spring Validation
- Lombok
- MapStruct
- Jackson

## SCOPE

Create a professional Spring Boot project.

Configure:

- Java 21
- Maven
- Spring Boot
- application.yml
- application-local.yml
- application-prod.yml

Create package structure:

- config/
- controller/
- service/
- repository/
- dto/
- mapper/
- entity/
- exception/
- model/
- util/

Create only the classes required for bootstrap.

## HEALTH ENDPOINT

Implement

```
GET /api/health
```

Example response:

```json
{
    "status": "UP",
    "application": "lease-demo",
    "version": "0.1.0"
}
```

No database. No JPA. No Security. No Flyway.

## CODE QUALITY

- Constructor injection only
- No field injection
- No wildcard imports
- No dead code
- No TODO comments
- No commented-out code
- Clean package structure
- Enterprise coding style

## README

Update README.md.

Add section:

- Backend prerequisites
- Build
- Run
- Health endpoint
- Project structure

## COMMAND DOCUMENTATION

Update `docs/commands/03-spring.md`.

Document ONLY commands actually executed. Do not invent commands.

## MILESTONE DOCUMENTATION

Create `docs/development/milestones/M01-spring-boot-bootstrap.md`.

Include:

- Objective
- Scope
- Technologies
- Dependencies Added
- Files Created
- Files Modified
- Commands Executed
- Design Decisions
- Verification
- Definition of Done
- Next Milestone

Documentation must describe the actual implementation. Do not invent information.

## PROMPT DOCUMENTATION

Create `docs/prompts/M01-spring-boot-bootstrap.md`.

Store this implementation prompt there.

## OUTPUT

At the end provide ONLY:

1. Summary
2. Files created
3. Files modified
4. Build result
5. Manual verification required

## RULES

- Edit source files directly.
- Never generate Python patch scripts.
- Never generate bash patch scripts.
- Never use python <<EOF heredocs.
- Never create unit test files (*.spec.ts) unless explicitly requested.
- Do not perform any HTTP/WebSocket tests. Skip them.
- Keep each modification under ~200 lines.
- Never modify files unrelated to the current milestone.
- Do not refactor existing code unless explicitly requested.
- Update all required documentation before finishing the milestone.
- Documentation is part of the Definition of Done.
- Do not make any Git operations (commit, push, checkout, merge, rebase, etc.).
- If more changes are needed, stop and wait for the next prompt.
