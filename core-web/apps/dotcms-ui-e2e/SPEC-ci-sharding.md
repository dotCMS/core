# Spec: CI Playwright Sharding

GitHub issue: [#36568](https://github.com/dotCMS/core/issues/36568)

## Objective

Split the single **E2E Tests - Nx Playwright** CI job into **3 parallel matrix jobs**, each running a disjoint Playwright shard (`--shard=N/3`), so PR E2E wall-clock is **under 25 minutes** without changing product tests or rebuilding Docker per shard.

**Users:** Falcon / CI maintainers.

**Success:** all shards green; max shard job wall-clock under 25m on representative PRs; flake documented vs the workers-only experiment ([#36567](https://github.com/dotCMS/core/issues/36567)).

## Tech Stack

- GitHub Actions matrix: `.github/test-matrix.yml` → `cicd_comp_test-phase.yml`
- Maven module `:dotcms-ui-e2e` → `pnpm exec nx run dotcms-ui-e2e:e2e --configuration=ci`
- Playwright CLI sharding (`--shard=N/M`); no shard config in `playwright.config.ts`

## Commands

```bash
# Full suite (local CI parity — unchanged)
./mvnw -pl :dotcms-ui-e2e verify -De2e.test.skip=false -De2e.test.env=ci

# Single shard (local / CI parity)
./mvnw -pl :dotcms-ui-e2e verify -De2e.test.skip=false -De2e.test.env=ci \
  -De2e.playwright.args=--shard=1/3

# Nx-only (stack already up)
cd core-web && CI=true CURRENT_ENV=ci pnpm exec nx run dotcms-ui-e2e:e2e --configuration=ci -- --shard=1/3
```

## Project Structure

| Path | Role |
|------|------|
| `.github/test-matrix.yml` | Three `e2e` suites with distinct shard `maven_args` |
| `core-web/apps/dotcms-ui-e2e/pom.xml` | `e2e.playwright.args` (no spaces; e.g. `--shard=1/3`) → Nx passthrough `--` in `e2e.test.cmd` |
| `SPEC-ci-sharding.md` | Living spec (this file) |
| `README.md` | Local shard commands and how to add a 4th shard |

## Code Style

Matrix job names use **1 of 3** (no `/`) because `stage_name` is embedded in GitHub Actions artifact names (`build-reports-test-*`), which reject forward slashes. Playwright still receives `--shard=N/3` via Maven.

Maven property default is empty so local full-suite runs are unchanged.

## Testing Strategy

- No new product specs (out of scope).
- Verify wiring: run Playwright with `--list` and a shard flag to confirm subset assignment.
- CI: three matrix jobs; PR green requires all three; measure slowest shard wall-clock.
- Flake: compare retries/failures to #36567 workers notes in an issue comment.

## Boundaries

- **Always:** Reuse `docker-image` artifact; keep `needs_docker` / `needs_license` / `needs_node`; update this spec when shard count changes.
- **Ask first:** Changing shard count (2 vs 4), changing `workers` in the same PR, editing test-phase workflow JS beyond matrix YAML.
- **Never:** Skip failing specs to hit time targets; commit secrets.

## Success Criteria

| Criterion | Check |
|-----------|--------|
| 2–3 sharded suites | Three entries under `e2e.suites` in `test-matrix.yml` |
| Disjoint subsets | Playwright `--shard=N/3`; all jobs must pass |
| Wall-clock under 25m | Max of parallel shard job durations on representative PR runs |
| Flake documented | Issue comment with timings vs workers experiment |
| Growth path | This spec + README explain how to add a 4th shard |

## Why 3 shards?

Baseline (workers=1): ~49m Playwright suite, ~56m job ([run 29282328043](https://github.com/dotCMS/core/actions/runs/29282328043)).

With workers=2 (~26m suite, ~36m job — [#36567](https://github.com/dotCMS/core/issues/36567)), a single job still exceeds the **under 25m** E2E phase target for sharding. Three parallel jobs each run ~⅓ of the suite on an isolated dotCMS stack:

- Playwright time per shard ≈ suite_time / 3 (plus per-job setup ~4–5m).
- Wall-clock for the E2E phase ≈ max(shard durations), not sum.
- Trade-off: 3× GitHub runners per PR vs one long serial job.

Sharding is orthogonal to `workers`: each shard job can still use CI workers from `playwright.config.ts`.

## How to add a 4th shard

1. Choose new total `M` (e.g. 4). Update **every** existing suite’s `--shard=N/M` so `N` runs 1…M with the same `M`.
2. In `.github/test-matrix.yml`, duplicate an e2e suite entry; set `name` / `stage_name` to `4/4` and `maven_args` to include `-De2e.playwright.args=--shard=4/4` (plus `-De2e.test.env=ci -pl :dotcms-ui-e2e`). Values must not contain spaces (CI `maven-job` tokenizes args on spaces).
3. Update this spec and `README.md` shard examples to use `/4`.
4. Validate on a PR: four green jobs, disjoint coverage, acceptable runner cost.

No workflow JavaScript changes are required if args continue to flow through Maven properties.

## Open Questions

None for implementation. Dependency: prefer measuring flake against merged #36567 workers config before treating sharding as the long-term default.
