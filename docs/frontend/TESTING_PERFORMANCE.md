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

### First, the trap that produced a wrong answer here

Do **not** measure two variants back-to-back in one batch. The first run pays the cold Vite cache
and the second inherits it warm, so the second variant looks better whatever it is. That artefact
produced a confident "`isolate: false` is −42%" in this repo, which then failed to reproduce.
Measure each arm best-of-two: `pnpm test:profile <p> --runs=2 -- <flags>`.

### The results

`isolate` — best-of-two per arm, warm cache both sides, idle machine:

| project | `isolate: true` | `--no-isolate` |
| --- | --- | --- |
| `dotcms-ui` | 34.16s | 33.95s |
| `edit-content` | 27.32s | 27.89s |
| `portlets-edit-ema-portlet` | 28.05s | 28.36s |
| `data-access` | 9.54s | 9.48s |
| `portlets-content-drive` | 28s | 35s |
| total of the first four | **99.07s** | **99.68s** (+0.6%) |

**`isolate: false` earns nothing.** The `setup` phase is the tell — it does not drop: `dotcms-ui`
reads 122.27s isolated and 125.65s non-isolated. If isolation were what makes setup run per *file*
instead of per worker, that number would fall. It doesn't, so the per-file cost lives somewhere
isolation does not reach. `isolate: true` stays, and is now written explicitly in every config
(research.md R-9 asked for that and it had been left to Vitest's default).

Everything else, on `libs/data-access` (85 files / 853 tests):

| Change | Effect | Verdict |
| --- | --- | --- |
| `pool: 'threads'` | `environment` 12.9s → **33.1s** | **Regression.** The DOM is more expensive in worker threads. Stay on `forks` |
| `environment: 'happy-dom'` (from `jsdom`) | `environment` 6.4s → 3.1s **but** `tests` 2.7s → **22.7s** and **4 files fail** | **Regression here.** Only per project, only where the baseline shows `environment` dominant |
| `NODE_COMPILE_CACHE=…` | inside the noise | **No measurable gain** on Node 24, and the docs note it is disabled by the `v8` coverage provider, which this suite uses |
| `deps.optimizer.web.enabled` | `data-access` 10.13s → 10.38s; `dotcms-ui` 34.16s → 37.59s with `transform` **31.54s → 31.58s** | **Dead end, structurally.** See below — do not retry |
| Drop the Angular plugin where nothing imports `@angular/*` | 5 projects, `deps.inline` 23 → 1, ~25% off each project's wall, identical test counts | **Works.** Now detected rather than assumed |

Three of those contradict the generic advice in Vitest's own
[improving-performance](https://vitest.dev/guide/improving-performance.html) guide, which leads with
"switch to `threads`", offers `happy-dom` as a cheaper `jsdom`, and recommends `isolate: false`.
All three are neutral-to-worse in this workspace. Measure before adopting.

### Why `deps.optimizer` cannot help this workspace

It looks like the obvious win — `transform` and `import` are the two largest phases in every
Angular project here (`dotcms-ui`: 31.5s and 110.7s) and the optimizer's whole job is to pre-bundle
dependencies with esbuild once instead of letting Vite transform them per import.

It does nothing, and the reason is structural rather than a tuning problem: **the optimizer only
pre-bundles dependencies that are EXTERNALISED, and `server.deps.inline` deliberately inlines
almost everything that matters.** That list is a correctness fix — every package shipping
Angular-compiled code must resolve to one `@angular/core` instance or components die with NG0203 /
`ngModule of null` — so it is not negotiable, and it leaves the optimizer with nothing to bundle.

The measurement says exactly that: enabling it on `dotcms-ui` moved `transform` from 31.54s to
31.58s. Not "a small gain" — no effect at all, plus ~10% of wall in overhead. `data-access` behaved
the same way. Both kept all their tests passing, so this is useless rather than dangerous.

Reopening this is only worth it if `deps.inline` ever shrinks, which would mean the
single-Angular-instance problem was solved some other way.

### Beware of your own machine

A `nx run <project>:test` measured 336s against a 27.72s baseline — same 30 files, same 1370 tests.
The cause was another profiling run holding all 16 cores (`load average` 21). Check `uptime` and
`pgrep -f vitest` before believing any number, and never benchmark two things at once.

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

Exceptions go in `NO_ISOLATE` in `tools/generate-vite-configs.mjs`, and **that map is empty because
the experiment was run and lost** — see the table above. The bar for adding an entry, should anyone
find a project where it does pay:

1. The project passes with `--no-isolate`, with the **same test count**.
2. Three consecutive passing runs plus one with `--sequence.shuffle`, to rule out the order
   dependence R-9 warns about — `isolate: false` turns a healthy suite into an order-dependent one.
3. A saving that survives best-of-two measurement on both arms, not a cold-cache artefact.
4. The numbers written into the map, so the next person re-checks instead of re-deriving.

Never flip it workspace-wide, and never for a project you have not run. A project whose suite is red
cannot qualify at all: with no green run there is nothing to compare against.

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
# The dirs that already have a generated config — NOT --all, which would also create
# apps/dotcdn/src/vite.config.mts, a project with zero specs and no tag:skip:test,
# i.e. a brand-new `test` target that fails with "No test files found".
grep -rl "GENERATED by tools/generate-vite-configs" --include=vite.config.mts apps libs \
  | sed 's|/vite.config.mts||' > /tmp/dirs.txt
cat /tmp/dirs.txt | xargs node tools/generate-vite-configs.mjs

# Prettier is part of the contract, not an afterthought: the generator emits raw text
# (double quotes, collapsed arrays) and the committed files are prettier-formatted, so
# regeneration alone shows ~1700 lines of pure formatting churn.
sed 's|$|/vite.config.mts|' /tmp/dirs.txt | xargs pnpm exec prettier --write

git diff --exit-code -- '**/vite.config.mts'   # clean = generator output unchanged
```

In zsh, `node tools/generate-vite-configs.mjs $DIRS` does **not** work: zsh does not
word-split unquoted variables, so all 45 paths arrive as one argument and the generator
reports `0 config(s)` — a no-op that reads like success. Pipe through `xargs`.

The generator's house style is that every deviation from what `nx g` would emit carries the measured
failure that justifies it. Follow it: a performance option with no measurement in its comment is the
thing this doc exists to prevent.
