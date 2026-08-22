# M0.0A — Enterprise Repository Foundation

## Objective

Establish a clean, deterministic repository foundation before any framework code is introduced.

The repository foundation is deliberately separated from framework bootstrap (Spring Boot, Angular) for
the following reasons:

- Framework generators (Spring Initializr, Angular CLI `ng new`) download dependencies from external
  services and are non-deterministic by nature. Any failure during generation leaves the repository in
  a partially initialised state.
- A clean foundation allows the directory layout, conventions, tooling configuration, and documentation
  structure to be reviewed and approved before framework code is added.
- Separating concerns keeps each milestone small, reviewable, and independently verifiable against its
  own Definition of Done.
- Repository conventions (`.editorconfig`, `.gitignore`, VS Code settings) must be in place before any
  source files are committed, not retrofitted afterwards.

---

## Scope

### Included

| Area | Detail |
|------|--------|
| Repository structure | Top-level directory layout covering backend, frontend, docker, scripts, docs, and CI/CD |
| Documentation skeleton | Placeholder files for every planned documentation section |
| VS Code configuration | Workspace settings and a curated list of recommended enterprise extensions |
| Scripts skeleton | One script per operational concern; each contains only a shebang, a header, and a TODO comment |
| Repository conventions | `.editorconfig`, `.gitignore`, `.nvmrc`, `LICENSE`, `README.md` headings |

### Intentionally Not Implemented

- Spring Boot application bootstrap (no Spring Initializr, no `pom.xml`, no source files)
- Angular application bootstrap (no `ng new`, no `package.json`, no source files)
- npm package installation
- Maven build execution
- Docker image build
- Cloud Run deployment
- CI/CD workflow implementation (`.github/workflows/` directory created; no YAML files written)
- Git commits (repository state is not committed as part of this milestone)

---

## Repository Structure

```
lease-demo/
├── .github/
│   └── workflows/          # CI/CD pipeline definitions (empty — populated in a later milestone)
├── .vscode/
│   ├── extensions.json     # Recommended VS Code extensions
│   └── settings.json       # Workspace editor settings
├── backend/                # Spring Boot application (empty — populated in M0.1)
├── docker/                 # Dockerfiles and Compose files (empty — populated in a later milestone)
├── docs/
│   ├── adr/                # Architecture Decision Records
│   ├── architecture/       # High-level architecture documents
│   ├── commands/           # Step-by-step command references (01–06)
│   ├── diagrams/           # System and component diagrams
│   ├── development/
│   │   └── milestones/     # Per-milestone documentation (this file)
│   ├── guides/             # Operational guides
│   └── interview/          # Interview preparation notes
├── frontend/               # Angular application (empty — populated in a later milestone)
├── scripts/
│   ├── build.sh
│   ├── clean.sh
│   ├── deploy-cloud-run.sh
│   ├── deploy-local.sh
│   ├── format.sh
│   ├── lint.sh
│   └── test.sh
├── .editorconfig
├── .gitignore
├── .nvmrc
├── LICENSE
└── README.md
```

---

## Files Created

### Root

| File | Purpose |
|------|---------|
| `README.md` | Project overview with section headings; TODO placeholders only |
| `.gitignore` | Excludes OS artefacts, IDE files, Node modules, Maven target, Docker env files, logs, and test reports |
| `.editorconfig` | Enforces UTF-8, LF line endings, 2-space indent (4-space for Java and XML) across all editors |
| `.nvmrc` | Pins the Node.js major version to `20` |
| `LICENSE` | MIT licence |

### VS Code

| File | Purpose |
|------|---------|
| `.vscode/extensions.json` | 23 recommended extensions covering editor tooling, Angular, Java/Spring Boot, Docker, REST Client, YAML, Markdown, and database |
| `.vscode/settings.json` | Format-on-save, per-language formatter assignments, TypeScript import preferences, Java null-analysis, file/search exclusions, Git autofetch, terminal defaults |

### Scripts

| File | Purpose |
|------|---------|
| `scripts/build.sh` | Entry point for building backend and frontend artefacts |
| `scripts/clean.sh` | Removes all build output and generated files |
| `scripts/deploy-local.sh` | Starts the full stack locally via Docker Compose |
| `scripts/deploy-cloud-run.sh` | Builds and deploys the application to Google Cloud Run |
| `scripts/format.sh` | Runs code formatters for Java and TypeScript sources |
| `scripts/lint.sh` | Runs static analysis for Java and TypeScript sources |
| `scripts/test.sh` | Executes all backend and frontend tests |

All scripts contain only a shebang (`#!/usr/bin/env bash`), a descriptive header, and TODO comments
outlining the intended implementation steps.

### Documentation

| File | Purpose |
|------|---------|
| `docs/commands/01-project-bootstrap.md` | Commands for bootstrapping the project from scratch |
| `docs/commands/02-angular.md` | Angular CLI command reference |
| `docs/commands/03-spring.md` | Maven / Spring Boot command reference |
| `docs/commands/04-docker.md` | Docker and Docker Compose command reference |
| `docs/commands/05-cloud-run.md` | Google Cloud Run deployment command reference |
| `docs/commands/06-git.md` | Git workflow and branching command reference |

Each file contains only its title heading and a TODO placeholder.

---

## Commands Executed

Only two shell commands were executed during this milestone. No external services, package managers,
or build tools were invoked.

| Command | Purpose | Result |
|---------|---------|--------|
| `mkdir -p backend frontend docker scripts docs/architecture docs/commands docs/guides docs/adr docs/interview docs/diagrams .github/workflows .vscode` | Create the required directory tree in a single operation | Success — all 12 directories created |
| `git log --oneline -5` | Verify the current repository state and confirm no new commits were created during this milestone | Success — repository HEAD remains at the initial commit (`d5b6521`) |

---

## Design Decisions

### Documentation-first approach

Documentation directories and placeholder files are created before any source code exists. This
establishes a clear contract for what will be documented and ensures that documentation is never
treated as an afterthought.

### Deterministic repository bootstrap

This milestone contains no steps that depend on network access, external services, or tool versions
beyond the shell itself. Every file is written directly. The result is reproducible regardless of
environment.

### Framework initialisation postponed

Spring Boot and Angular are not bootstrapped here. Both frameworks rely on external generators
(Spring Initializr, Angular CLI `ng new`) that fetch dependencies at generation time. Deferring this
to dedicated milestones (M0.1, M0.2) isolates the risk and keeps each milestone small enough to be
reviewed in a single session.

### Small milestone strategy

Deliberately constraining the scope of each milestone to a single, well-defined concern reduces the
blast radius of any failure. A milestone that creates only files and directories cannot break a running
application.

### Enterprise folder layout

The top-level directory layout separates deployment concerns (`docker/`), operational scripts
(`scripts/`), living documentation (`docs/`), and application code (`backend/`, `frontend/`) into
distinct areas. This mirrors conventions used in production monorepos and avoids the ambiguity of
flat project structures.

### VS Code configuration committed to the repository

Workspace settings and extension recommendations are committed rather than left to individual
developer preference. This ensures consistent formatting, linting, and tooling behaviour across all
workstations from the first checkout.

---

## Lessons Learned

- Splitting the repository setup from framework bootstrap prevents the common situation where a
  failed `ng new` or Spring Initializr call leaves the repository in a partially committed, broken
  state.
- External project generators introduce version drift and network dependency at the most critical
  point of project setup. Isolating them into their own milestones makes failures recoverable without
  discarding earlier work.
- Establishing `.editorconfig` and `.gitignore` before any source files are committed eliminates an
  entire category of formatting and noise-diff issues that are otherwise expensive to retrofit.
- A documentation skeleton created before implementation serves as a forcing function: it makes gaps
  in planned documentation visible immediately rather than at delivery time.

---

## Definition of Done

- [x] Repository directory structure created
- [x] Documentation skeleton created (`docs/commands/01` – `06`)
- [x] Scripts skeleton created (shebang + header + TODO; 7 files)
- [x] VS Code configuration created (`.vscode/extensions.json`, `.vscode/settings.json`)
- [x] Repository conventions established (`.editorconfig`, `.gitignore`, `.nvmrc`, `LICENSE`)
- [x] `README.md` expanded with required section headings
- [x] No frameworks bootstrapped
- [x] No external commands executed (no npm, Maven, Spring Initializr, `ng new`, Docker)
- [x] No package installation attempted
- [x] No Docker build attempted
- [x] No Git commits created

---

## Next Milestone

**M0.1 — Spring Boot Enterprise Bootstrap**

Bootstrap the Spring Boot application inside the `backend/` directory using enterprise-grade project
structure, dependency management, and configuration conventions.
