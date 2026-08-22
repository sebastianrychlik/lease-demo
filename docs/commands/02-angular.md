# 02 — Angular

Angular CLI commands used in this project.

---

## Prerequisites

| Tool | Version |
|---|---|
| Node.js | 20.x |
| npm | 10.x |
| Angular CLI | 18.x (project-local via npx) |

---

## Install dependencies

```bash
cd frontend
npm install
```

---

## Development server

```bash
cd frontend
npm start
```

or equivalently:

```bash
cd frontend
npx ng serve
```

Application starts at: `http://localhost:4200`

Hot module replacement is enabled by default.

The Angular development proxy is enabled automatically.
All requests to `/api/*` are forwarded to `http://localhost:8080`.

> **Startup order:** start the backend (`mvn spring-boot:run`) before the frontend.

---

## Build

### Development build

```bash
cd frontend
npx ng build --configuration development
```

Output: `frontend/dist/lease-demo/`

### Production build

```bash
cd frontend
npx ng build --configuration production
```

Production build includes:
- AOT compilation
- Tree shaking
- Output hashing
- Bundle optimization

---

## Lint

```bash
cd frontend
npx ng lint
```

---

## Format (Prettier)

```bash
cd frontend
npm run format
```

Formats all `.ts` and `.html` and `.scss` files under `src/`.

---

## Generate components (Angular CLI schematics)

### Component

```bash
cd frontend
npx ng generate component <path/name> --standalone --change-detection OnPush
```

Example — add a feature component:

```bash
npx ng generate component features/exchange-rate/exchange-rate-list \
  --standalone \
  --change-detection OnPush
```

### Service

```bash
cd frontend
npx ng generate service core/services/<name>
```

### Guard

```bash
cd frontend
npx ng generate guard core/guards/<name> --functional
```

### Pipe

```bash
cd frontend
npx ng generate pipe shared/pipes/<name> --standalone
```

### Directive

```bash
cd frontend
npx ng generate directive shared/directives/<name> --standalone
```

---

## Project structure overview

```
frontend/src/app/
├── core/           # Singleton infrastructure (services, interceptors, guards)
├── shared/         # Reusable UI (components, pipes, directives)
├── features/       # Business features — lazy-loaded route modules
└── layouts/        # Application layout wrappers
```

See `docs/development/milestones/M1.0-angular-foundation.md` for full architecture details.

---

## Proxy configuration

The Angular development server proxies `/api/*` to the Spring Boot backend.

**File:** `frontend/proxy.conf.json`

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "info"
  }
}
```

The proxy is active for all `ng serve` / `npm start` runs (wired via `angular.json` `serve.options.proxyConfig`).

---

## Environment configuration

| File | Purpose |
|---|---|
| `src/environments/environment.ts` | Development config — `apiBaseUrl: '/api'` (proxy forwards to backend) |
| `src/environments/environment.production.ts` | Production config — `apiBaseUrl: '/api'` (same-origin deployment) |

Both environments use the relative `/api` base URL.
The Angular build system swaps environment files automatically based on `--configuration`.

---

## Routing

Routes are defined in `src/app/app.routes.ts`.

All feature routes use `loadComponent` or `loadChildren` for lazy loading:

```typescript
{
  path: 'feature-name',
  loadChildren: () =>
    import('./features/feature-name/feature-name.routes')
      .then(m => m.FEATURE_NAME_ROUTES),
}
```

To add a new feature route, append an entry to `APP_ROUTES` in `app.routes.ts`.

---

## Exchange Rates feature (M2.0)

The exchange rates feature is available at `/exchange-rates`.

It is lazily loaded via `loadChildren` from:

```
src/app/features/exchange-rates/exchange-rates.routes.ts
```

Feature structure:

```
src/app/features/exchange-rates/
├── exchange-rates.routes.ts        # Lazy route definitions
├── models/
│   └── exchange-rate.model.ts      # ExchangeRate, ExchangeRateResponse interfaces
├── pages/
│   └── exchange-rates-page.component.ts  # Table + signals state management
└── services/
    └── exchange-rate.service.ts    # Delegates to ApiService
```

The component uses Angular Signals to represent four explicit states:
`loading` | `success` | `empty` | `error`

Navigate to `http://localhost:4200/exchange-rates` to view the feature.
