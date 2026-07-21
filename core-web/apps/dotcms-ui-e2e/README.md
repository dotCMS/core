# dotCMS UI E2E Tests

Browser end-to-end tests for the dotCMS admin UI. This is the **canonical** E2E suite — Playwright, Nx, and **pnpm**. The legacy suite at `e2e/dotcms-e2e-node` is deprecated and will be removed; do not add tests there.

## Prerequisites

- Docker and a docker compose setup you already use locally
- dotCMS license at `~/.dotcms/license/license.dat`
- Node.js per [`.nvmrc`](../../../.nvmrc) (`nvm use`)
- Java per [`.sdkmanrc`](../../../.sdkmanrc) (`sdk env install`)
- [`just`](../../../justfile) from the repo root
- Dependencies installed in `core-web/` (`pnpm install`)

## Local debug flow (frontend developers)

Use this path when iterating on a feature and debugging tests interactively.

### 1. Build a local dotCMS image

From the **repo root**:

```bash
just build-no-cache
```

This performs a full rebuild (clears Nx/Angular/node_modules caches) and produces the local test image:

```text
dotcms/dotcms-test:1.0.0-SNAPSHOT
```

### 2. Point your docker compose at that image

In your habitual docker compose file, set the dotCMS service image:

```yaml
image: dotcms/dotcms-test:1.0.0-SNAPSHOT
```

Start your stack and wait until dotCMS is reachable at `http://localhost:8080/dotAdmin`.

### 3. Run Playwright in UI mode (Angular on :4200)

From **`core-web/`**:

```bash
pnpm nx e2e dotcms-ui-e2e --ui
```

This runs in **dev** mode (`CURRENT_ENV=dev`):

- Starts the Angular dev server on port **4200** (proxies `/api/*` to `:8080`)
- Opens Playwright's interactive UI for step-by-step debugging

Ensure dotCMS is already running on **8080** before starting. Playwright reuses an existing dev server when one is already up (`E2E_REUSE_EXISTING_SERVER` defaults to `true`).

### 4. Run and debug tests manually

Use the Playwright UI to pick specs, run individual tests, inspect traces, and iterate on locators.

**Codegen** (record selectors against the running app):

```bash
npx playwright codegen http://localhost:4200/dotAdmin
```

### Quick reference

All steps in order:

```bash
# 1. repo root — build local dotCMS image
just build-no-cache

# 2. your compose file — set image: dotcms/dotcms-test:1.0.0-SNAPSHOT, then start the stack
docker compose up

# 3. core-web/ — Angular dev server on :4200 (optional; Playwright can start this for you in step 4)
pnpm nx serve dotcms-ui

# 4. core-web/ — Playwright UI against :4200 (proxies API to :8080)
pnpm nx e2e dotcms-ui-e2e --ui
```

Ensure dotCMS is up on **8080** before step 4. If step 3 is already running, Playwright reuses it (`E2E_REUSE_EXISTING_SERVER=true` by default).

### Useful dev variants

All commands run from `core-web/`:

```bash
# Full lifecycle: builds Docker image, starts dotCMS via Maven, opens Playwright UI
pnpm e2e:dev

# Headless dev run against an already-running stack (faster feedback)
pnpm e2e:dev:headless

# Filter by test name
pnpm nx e2e dotcms-ui-e2e --grep "Login"

# Run a specific file
pnpm nx e2e dotcms-ui-e2e src/tests/login/login.spec.ts

# Different reporter
pnpm nx e2e dotcms-ui-e2e --reporter=list
```

> **Note:** `pnpm e2e:dev` runs the `e2e:dev:full` target — a heavy, end-to-end path that builds the dotCMS Docker image, runs `docker:stop` / `docker:start` via Maven, waits for `:8080`, then launches Playwright **UI mode** (`--ui`). Use it when you want Maven to manage the full stack. When dotCMS is already running (see [Local debug flow](#local-debug-flow-frontend-developers)), prefer `pnpm e2e:ui` or `pnpm nx e2e dotcms-ui-e2e --configuration=dev` instead.

## Full suite / CI parity from local

To run the **entire** E2E suite the same way CI does (headless, full Maven lifecycle):

From the **repo root**:

```bash
just test-e2e
```

Equivalent:

```bash
./mvnw -pl :dotcms-ui-e2e verify \
  -De2e.test.skip=false \
  -De2e.test.env=ci \
  -Dmaven.build.cache.skipCache=true
```

This:

- Provisions dependencies and Playwright via Maven
- Runs tests with `CURRENT_ENV=ci` (base URL `http://localhost:8080`, no Angular dev server)
- Executes headless against dotCMS directly

Secondary path from `core-web/` (also builds Docker and manages containers via Nx):

```bash
pnpm e2e:ci
```

Prefer `just test-e2e` when validating before merge — it matches the CI pipeline (full suite locally; CI runs **3 parallel shards** — see [CI sharding](#ci-sharding)).

### CI sharding

PR CI runs the Playwright suite as **three parallel jobs** (`--shard=1/3`, `2/3`, `3/3`). Each job boots its own dotCMS stack from the prebuilt Docker artifact. Rationale and how to add a 4th shard: [SPEC-ci-sharding.md](SPEC-ci-sharding.md).

Run one shard locally (Maven lifecycle, same as CI):

```bash
./mvnw -pl :dotcms-ui-e2e verify \
  -De2e.test.skip=false \
  -De2e.test.env=ci \
  -De2e.playwright.args=' -- --shard=1/3'
```

With the stack already up:

```bash
cd core-web && CI=true CURRENT_ENV=ci pnpm exec nx run dotcms-ui-e2e:e2e --configuration=ci -- --shard=1/3
```

## Environment reference

| Mode | `CURRENT_ENV` | Base URL | Backend |
|------|---------------|----------|---------|
| Dev (default) | `dev` | `http://localhost:4200` | Angular proxy → `:8080` |
| CI | `ci` | `http://localhost:8080` | Direct dotCMS |

### Environment variables

| Variable | Values | Default | Purpose |
|----------|--------|---------|---------|
| `CURRENT_ENV` | `dev` \| `ci` | `dev` | Selects base URL and webServer behavior |
| `HEADLESS` | `true` \| `false` | `true` in CI | Override browser visibility |
| `E2E_BASE_URL` | URL | per env | Override base URL |
| `E2E_REUSE_EXISTING_SERVER` | `true` \| `false` | `true` | Reuse running Angular dev server |

### Reports and artifacts

| Artifact | Location |
|----------|----------|
| HTML report | `apps/dotcms-ui-e2e/playwright-report/` |
| JUnit | `apps/dotcms-ui-e2e/test-results/junit.xml` |
| Nx output | `dist/.playwright/apps/dotcms-ui-e2e/` |
| Screenshots / video / traces | `apps/dotcms-ui-e2e/test-results/` (on failure / retry) |

Open a report manually:

```bash
npx playwright show-report apps/dotcms-ui-e2e/playwright-report
```

In dev mode, the HTML report opens automatically when tests finish.

## When to write Unit vs Integration vs E2E

**Governing rule:** put each assertion at the **lowest level that can prove it**. E2E is the level of last resort.

| Level | What is real | What is mocked | Use for |
|-------|--------------|----------------|---------|
| **Unit** | One component or class | All collaborators (services, store, HTTP, child components) | Pipes, guards, form validation, mapping logic, component output with stubbed deps (Jest + Spectator) |
| **Integration (FE)** | Angular app: real services, store, router, child components, PrimeNG | **Only HTTP** (`HttpTestingController`) | Request/response wiring, navigation, your bindings into PrimeNG — assert *your* contract, not PrimeNG internals |
| **E2E (browser)** | Full deployed stack (UI → backend → DB/ES) | Only third-party SaaS at the network edge | Cross-subsystem propagation a mock cannot fake: publish → index → delivery, real workflow transitions, server-side auth enforcement, E2E-class production incidents |

### Do not write E2E for

- Field validation, form error messages, copy, or visual layout → **Unit or Integration**
- A single endpoint's request/response shape → **Integration or contract test**
- Deterministic logic in one service → **Unit**
- Coverage or "being thorough" with no user journey → **Delete / refuse in review**

### Data strategy (E2E)

- Tests run against an **empty starter** — a clean dotCMS instance with no pre-existing content.
- Baseline seed data (sites, users) may exist but is **read-only** — do not mutate shared seed records.
- Any data a test asserts on must be **created by that test**, namespaced (e.g. `e2e_<journey>_<uuid>_`), and **cleaned up** on success and failure.

```typescript
test('Edit blog post', async ({ page, request }) => {
    const blog = await createContent(request, {
        contentType: 'Blog',
        title: `e2e_blog_${Date.now()}`,
        body: 'Test content'
    });

    await page.goto(`/content/edit/${blog.identifier}`);
    // ... assertions ...

    await deleteContent(request, blog.identifier);
});
```

Prefer `beforeEach` / `afterEach` or fixtures for setup and teardown. See existing patterns in `src/fixtures/` and `src/requests/`.

### API setup (`src/requests/`)

**All test data must be created via the REST API** — never through the UI during setup. The `src/requests/` folder holds shared helpers that call dotCMS endpoints directly (Playwright `APIRequestContext`).

Current modules include: `contentType.ts`, `contentlets.ts`, `sites.ts`, `folders.ts`, `pages.ts`, `templates.ts`, `schemas.ts`, `workflow.ts`, `workflowActions.ts`, `field-variables.ts`, `updateFeatureFlag.ts`.

**Before adding a new helper:**

1. Search `src/requests/` for an existing function that already covers the endpoint or payload you need.
2. **Reuse** it if it fits — do not duplicate API calls in specs or fixtures.
3. If nothing exists, add a function to the **most relevant file** in `src/requests/` (one concern per file, shared across specs).
4. Wire complex multi-step setup in `src/fixtures/`, but keep raw HTTP calls in `src/requests/`.

Never inline `request.post(...)` in a spec when the same call belongs in `src/requests/`.

## Creating new E2E tests

1. **Check the level first** — if Unit or Integration can prove it, do not add E2E.
2. **Copy an existing spec** under `src/tests/` that matches your feature area (e.g. `edit-content/fields/relationship-field/`).
3. **Follow the project layout** — see [AGENTS.md](AGENTS.md) for file naming, locators, and gotchas.
4. **Use Page Object Model** — shared UI in `src/pages/`, field-specific helpers in `tests/.../helpers/`, API setup in `src/requests/` and `src/fixtures/`.
5. **Locator priority:** `getByRole` → `getByTestId` → `getByLabel` → CSS (Dojo iframe only).
6. **Create your own data via REST API** — use helpers in `src/requests/`; check for duplicates before adding a new endpoint wrapper (see [API setup](#api-setup-srcrequests) above).

## AI-assisted workflow

Recommended pipeline when adding E2E coverage for a feature or issue:

1. **Write Unit and Integration tests first** — cover component logic, form validation, and HTTP wiring in Jest/Spectator before adding any E2E. Most behavior should already be proven at this level.
2. **`/test-plan`** — generate a QA-oriented test plan and user journeys from the GitHub issue.
3. **`/e2e-flow`** — turn the plan into Playwright BDD specs and implementation in this project.
4. **Re-evaluate with AI** — before merging, ask which proposed E2Es are redundant because Unit or Integration tests already cover the same behavior. Drop or push those assertions down per the cheat sheet above.

The skills own the detailed process; this README is the entry point and the redundancy gate.

## Project layout

```
src/
  fixtures/     # auth, API setup (e.g. relationship.fixture.ts)
  pages/        # shared page objects (*.page.ts)
  utils/        # iframe, portlets, credentials
  requests/     # REST API helpers for test data (content types, contentlets, sites, …)
  tests/        # specs grouped by feature
playwright.config.ts
```

Code conventions, Angular/Dojo gotchas, and locator rules: **[AGENTS.md](AGENTS.md)**.
