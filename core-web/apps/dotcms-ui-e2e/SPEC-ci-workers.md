# Spec: Playwright CI Workers = 2

**Issue:** [#36567 – e2e: Increase Playwright CI workers to reduce E2E job duration](https://github.com/dotCMS/core/issues/36567)

## Objective

Reduce wall-clock time of the PR job **E2E Tests - Nx Playwright** by allowing two Playwright workers on CI. Baseline: ~49m Playwright phase / ~56m job with `workers: 1` ([run 29282328043](https://github.com/dotCMS/core/actions/runs/29282328043)). Target: Playwright phase **< 30 minutes** average across ≥5 consecutive green runs, without new parallel-related flakes.

Users: Falcon / CI maintainers and anyone waiting on PR E2E feedback.

## Tech Stack

- Playwright via `@nx/playwright` in `core-web`
- Maven module `:dotcms-ui-e2e` sets `CI=true` and `CURRENT_ENV=ci`
- CI matrix entry: `.github/test-matrix.yml` → `"E2E Tests - Nx Playwright"` with `-De2e.test.env=ci -pl :dotcms-ui-e2e`

## Commands

```bash
# Local (unchanged — workers remain undefined / Playwright default)
cd core-web && pnpm nx run dotcms-ui-e2e:e2e

# CI-parity locally (requires running dotCMS on :8080)
cd core-web && CI=true CURRENT_ENV=ci pnpm nx run dotcms-ui-e2e:e2e --configuration=ci

# Maven path used by CI (from repo root; needs stack + image as CI does)
./mvnw verify -pl :dotcms-ui-e2e -De2e.test.skip=false -De2e.test.env=ci
```

## Project Structure

```
core-web/apps/dotcms-ui-e2e/
  playwright.config.ts   # workers: CI ? 2 : undefined
  pom.xml                # already CI=true
  SPEC-ci-workers.md     # this spec
  src/tests/**/*.spec.ts # serial describes unchanged
```

## Code Style

```typescript
/* Parallelize CI (2 workers); local keeps Playwright default. */
workers: process.env.CI ? 2 : undefined,
```

Keep `fullyParallel: true`, `retries: process.env.CI ? 2 : 0`, `forbidOnly: !!process.env.CI`.

## Testing Strategy

| Level | What |
|-------|------|
| Config review | Diff shows CI workers `1` → `2`; comment updated |
| PR CI | Single PR run of **E2E Tests - Nx Playwright** must pass |
| Soak (post-merge) | ≥5 consecutive green PR runs; avg Playwright phase < 30m |
| Flake triage | Any failure: note spec name + Actions URL on the issue; fix only if clearly caused by parallelism |

No new unit tests for a one-line config change.

## Boundaries

- **Always:** Keep `fullyParallel: true`; leave serial `describe`s alone; document before/after timings on #36567 after soak.
- **Ask first:** Raising workers above 2; sharding across jobs; changing retries/timeouts; serializing additional specs to paper over contention.
- **Never:** Reduce coverage to hit the time goal; skip failing tests; change Docker image build / Initial Artifact Build as part of this issue.

## Success Criteria

1. CI `workers` is `2` in `playwright.config.ts`.
2. Spec file committed and linked from the PR.
3. ≥5 consecutive green PR E2E runs after merge.
4. Avg Playwright phase < 30m across those runs (baseline ~49m).
5. No new flakes attributable to parallel execution (or documented + mitigated).
6. Results comment posted on #36567.

## Open Questions

None blocking.

## Out of scope

Sharding, product test logic, Docker image build optimization, env-driven worker override (follow-up if 2 is flaky or still too slow).
