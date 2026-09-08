# Vitest suite performance (`core-web`)

How to measure the unit-test suite, what has already been measured, and which knobs turned out
**not** to help. Load this when you are about to change a Vitest option "to make tests faster" —
several of the obvious changes are measured regressions here, and the numbers are below so you do
not have to rediscover them.

For **writing** tests see [TESTING_FRONTEND.md](./TESTING_FRONTEND.md); for **reviewing** them see
[TESTING_REVIEW_RULES.md](./TESTING_REVIEW_RULES.md). This doc is only about run time.

## The one thing to understand first

Vitest reports five phases, and they are cumulative across workers rather than wall-clock:

| Phase | What it covers |
| --- | --- |
| `transform` | Vite transforming source (Angular AOT/JIT, TS) |
| `setup` | running `setupFiles` — **once per test file** |
| `import` | importing the spec and its dependency graph, incl. test collection |
| `tests` | actually executing test bodies |
| `environment` | building the DOM (`jsdom` / `happy-dom`) — **once per test file** |

Two things about those numbers before you use them:

- They are **cumulative across workers**, so they do not sum to the reported `Duration` and must
  never be compared against wall time. Compare phases to *each other*, within one project.
- **The ratio varies enormously by project.** Read your project's own numbers; do not generalise from
  someone else's.

`libs/data-access` (85 files, 853 tests) — a services library:

```
Duration 9.52s  (transform 20.06s, setup 29.63s, import 88.76s, tests 2.80s, environment 12.90s)
```

Here test execution is ~2% of the accumulated work; the rest is per-file startup. But
`apps/dotcms-ui` (226 files) reads roughly `setup 145s, import 125s, tests 84s` — startup still
dominates, yet test bodies are a real share. So:

- **Every project** pays per-file startup, and `setup` + `environment` is the biggest single block in
  nearly all of them. That is what `isolate` changes.
- **Heavy component projects** additionally have slow test bodies, which no config option fixes.

Check which case you are in with `pnpm test:profile <project>` before choosing a knob.

## Measuring

```bash
cd core-web

pnpm test:profile --list                  # every project with a test-running config, by spec count
pnpm test:profile data-access ui          # named projects
pnpm test:profile --top=6                 # the six projects with the most spec files
pnpm test:profile --all --json=out.json   # everything, machine-readable
pnpm test:profile data-access --runs=3    # repeat, keep the fastest (cold caches are noise)

# Anything after `--` goes to Vitest verbatim — this is how to compare options
# WITHOUT editing 46 config files:
pnpm test:profile data-access -- --no-isolate
pnpm test:profile data-access -- --pool=threads --no-isolate
```

`tools/profile-tests.mjs` parses the `default` reporter's own summary, so its numbers cannot drift
from what Vitest reports. It exits non-zero if any project fails, so it doubles as a gate.

It complements `tools/capture-baseline.sh`, which captures test **counts** and no timings. Counts
answer "did we lose tests?"; this answers "where does the time go?". Both are needed — a
configuration change that speeds things up by silently not running files is the failure mode this
migration already hit twice.

A full-suite baseline is captured with
`pnpm test:profile --all --json=../specs/37444-vitest-unit-test-migration/profile-baseline.json`.
Capture it against a **quiescent working tree**: an edit landing mid-run changes what later projects
compile, and so does a `pnpm install`. A run taken through either is not a baseline.

## Measured results — read before changing an option

All on `libs/data-access` (85 files / 853 tests), 16-core macOS, warm Vite cache unless noted.

| Change | Effect | Verdict |
| --- | --- | --- |
| `isolate: false` | Duration 9.52s → 5.55s (−42%), wall −25%; `setup` 29.6→18.6s, `environment` 12.9→6.4s. 853/853 pass | **Works.** Per-project measured exception — see below |
| `pool: 'threads'` | Duration 9.52s → 11.25s; `environment` 12.9s → **33.1s** | **Regression.** The DOM is more expensive in worker threads. Stay on `forks` |
| `pool: 'threads'` + `isolate: false` | wall 10s vs 12s for forks, but `environment` 35.1s | Not worth it; and `--cpu-prof` does not work with `threads` |
| `environment: 'happy-dom'` (from `jsdom`) | `environment` 6.4s → 3.1s **but** `tests` 2.7s → **22.7s** and **4 files fail** | **Regression here.** Only per project, only where the baseline shows `environment` dominant |
| `NODE_COMPILE_CACHE=…` | 11s vs 12s — inside the noise | **No measurable gain** on Node 24, and the docs note it is disabled by the `v8` coverage provider, which is what this suite uses |

Two of those contradict the generic advice in Vitest's own
[improving-performance](https://vitest.dev/guide/improving-performance.html) guide, which leads with
"switch to `threads`" and offers `happy-dom` as a cheaper `jsdom`. Both are measured regressions in
this workspace. Measure before adopting.

## Where per-file startup comes from

Every `src/test-setup.ts` (36 of them are wired into a config) loads:

```ts
import '@analogjs/vitest-angular/setup-zone';
import '@angular/compiler';                      // the full Angular JIT compiler
import '@analogjs/vitest-angular/setup-snapshots';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';
setupTestBed({ zoneless: false, providers: [provideZoneChangeDetection()] });
```

With `isolate: true` that runs once per test file — roughly 1,090 times. With `isolate: false` it
runs once per worker, which is exactly where the measured saving comes from.

The long tail matters too: **26 of 46 projects have 12 or fewer spec files**, so for them almost all
wall time is a cold Vitest start (Vite server + config resolve + Angular plugin init), not test
execution. `libs/global-store`: 6 files, 5.98s wall, **0.23s** of it `tests`.

## `isolate` — the rule

`isolate: true` is the workspace default **by decision, not by accident**:
`specs/37444-vitest-unit-test-migration/research.md` R-9 chose it so the runner's isolation model
matches the one the specs were written under (Jest gives every file a fresh module registry; the
Angular builder defaults to Karma's shared context instead). That decision explicitly allows
exceptions: *"treat any project where that is too slow as a measured exception rather than flipping
the default."*

So `isolate: false` is allowed **per project**, and only with:

1. A run of that project with `--no-isolate` that passes, with the same test count.
2. Three consecutive passing runs, plus one with `--sequence.shuffle`, to rule out the order
   dependence R-9 warns about.
3. The project listed in the generator's exception list, with its measurement in the comment.

Never flip it workspace-wide, and never flip it for a project you have not run.

## Diagnostic recipes

```bash
cd core-web

# Which imports cost the most (candidates: primeng, @tiptap, monaco, gridstack)
pnpm exec vitest run --config libs/ui/vite.config.mts --experimental.importDurations.print

# Coverage: find files taking >3s, or large files pulled in by accident
DEBUG=vitest:coverage pnpm exec vitest run --config libs/ui/vite.config.mts --coverage

# CPU / heap profile of the test runner. Needs `fileParallelism: false` in the config,
# and does NOT work with pool: 'threads' — another reason this suite stays on forks.
#   execArgv: ['--cpu-prof', '--cpu-prof-dir=test-runner-profile']

# Profile the main thread (Vite itself, globalSetup)
node --cpu-prof --cpu-prof-dir=main-profile ./node_modules/vitest/vitest.mjs --run
```

Open the `.cpuprofile` output in Speedscope or Chrome DevTools' Performance panel.

## Reporters

Every project currently declares `['default', 'github-actions', ['junit', …]]`. Two notes:

- `github-actions` **auto-enables** when `GITHUB_ACTIONS=true`, so declaring it is redundant in CI
  and only adds noise locally.
- `junit` writes `core-web/target/core-web-reports/<project>.xml`. CI consumes those
  (`generates_test_results: true` in `.github/test-matrix.yml`); locally nothing reads them.

## Nx cache — two things that were wrong

`nx.json` `targetDefaults.test` **replaces** the `@nx/vitest` plugin's inferred `inputs` and
`outputs` rather than merging with them (verified with `nx show project <p> --json`). That silently
dropped two signals:

- **No `outputs`** meant a cache hit restored no JUnit XML and no coverage, while CI publishes those
  artifacts. Now declared as `target/core-web-reports/{projectName}.xml` plus
  `coverage/{projectRoot}` — per project, deliberately not the shared report directory, so one
  project's cache cannot restore another's stale XML.
- **No `externalDependencies: ["vitest"]`** meant bumping Vitest did not invalidate any test cache.

Related, and *not* fixed: the `production` named input does not exclude `vite.config.mts`, so
regenerating the configs invalidates the `test` cache of every dependent project via `^production`.
Excluding it from `production` is **not** the fix — `production` is also the own-input of `build`,
where `vite.config.mts` genuinely is the build config.

## Changing a Vitest option

All 46 `vite.config.mts` files are generated. **Edit
`core-web/tools/generate-vite-configs.mjs` and regenerate** — do not hand-edit a config, it will be
overwritten and reviewers will not see the reasoning:

```bash
cd core-web
node tools/generate-vite-configs.mjs --all
git diff --exit-code            # must be clean after a no-op regeneration
```

The generator's house style is that every deviation from what `nx g` would emit carries the measured
failure that justifies it. Follow it: a performance option with no measurement in its comment is the
thing this doc exists to prevent.
