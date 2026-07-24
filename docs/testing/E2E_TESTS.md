# End-to-End (E2E) Tests

Browser end-to-end tests for the dotCMS admin UI live in a single Nx + Playwright project.

## Canonical suite

| Item | Location |
|------|----------|
| Project | [`core-web/apps/dotcms-ui-e2e`](../../core-web/apps/dotcms-ui-e2e/) |
| Runbook | [`core-web/apps/dotcms-ui-e2e/README.md`](../../core-web/apps/dotcms-ui-e2e/README.md) |
| Conventions | [`core-web/apps/dotcms-ui-e2e/AGENTS.md`](../../core-web/apps/dotcms-ui-e2e/AGENTS.md) |

Stack: Playwright, TypeScript, **pnpm**, Nx. CI runs with `workers: 2` when `CI=true` (see project `playwright.config.ts`).

## Layout

```
core-web/apps/dotcms-ui-e2e/
  src/
    fixtures/     # auth, API setup
    pages/        # shared page objects
    requests/     # REST helpers for test data
    tests/        # *.spec.ts
  playwright.config.ts
  project.json
```

## Running tests

**Local (frontend dev loop)** — see the README for Docker, `just build-no-cache`, and Playwright UI mode:

```bash
cd core-web
pnpm nx e2e dotcms-ui-e2e --ui
```

**CI parity (Maven + Docker lifecycle)** — from repo root:

```bash
just test-e2e
```

Equivalent Maven module:

```bash
./mvnw verify -De2e.test.skip=false -De2e.test.env=ci -pl :dotcms-ui-e2e
```

Use `-De2e.test.skip=false` or tests are skipped by default.

## CI

GitHub Actions uses `.github/test-matrix.yml` → `e2e` job → `-pl :dotcms-ui-e2e` with `-De2e.test.env=ci`.

## Reports and artifacts

After a Maven run, JUnit output is copied under `core-web/apps/dotcms-ui-e2e/target/playwright-reports/`. Playwright HTML reports and traces follow paths configured in `playwright.config.ts` under the project directory (`test-results/`, etc.).

## Adding tests

Do not add browser E2E tests outside `core-web/apps/dotcms-ui-e2e`. Follow AGENTS.md for locators, API setup via `src/requests/`, and isolation patterns.
