# Contract: The `nx test` target after migration

**Requirements**: FR-005, FR-006, FR-007, FR-009, FR-010 · **Research**: R-1, R-3, R-4, R-9, R-10

## Invariant

Every project that has a unit-test target keeps it named **`test`**. This is the whole reason CI
needs no workflow edit: `nx affected -t test` resolves identically before and after. The executor
behind the name changes; the name does not.

## Two project classes

### Class 1 — Angular projects (41 from the Jest list + `libs/dotcms-js`)

Executor `@angular/build:unit-test` with `runner: "vitest"`, configured in `project.json`.

Options fixed by contract:

| Option | Value | Why |
|---|---|---|
| `runner` | `"vitest"` | Explicit rather than relying on the schema default |
| `tsConfig` | project's `tsconfig.spec.json` | Exists in all 41; also the builder's own default |
| `setupFiles` | project's `test-setup.ts` where present | 41 such files exist; replaces Jest's `setupFilesAfterEach` |
| `buildTarget` | explicit for the 28 non-buildable libraries; own `build` for the 13 that have one | The builder's default (`<project>:build:development`) **does not exist** for 28 projects (R-10) |
| `isolate` | **`true`** | The builder defaults to `false` "to align with the Karma/Jasmine experience". Jest gave every spec file a fresh module registry; leaving the default would share state across 977 specs written under Jest semantics (R-9) |
| `passWithNoTests` | **absent** | FR-004. Present today in 42 `project.json` files, so removal is per project (R-11) |
| `reporters` / `outputFile` | JUnit + `github-actions` + default | FR-008 — see [ci-artifacts.md](./ci-artifacts.md) |
| `coverage*` | reporters `html,lcov,text`; **no thresholds** | FR-008 parity; thresholds deliberately unset (provider change makes deltas expected) |

**`runnerConfig`** — a sibling `vitest-base.config.ts`, added **only** where builder options are
insufficient. Required for:

- **DOM environment.** The builder exposes no `environment` option; without `browsers` it runs in
  Node with **jsdom**. The 6 projects on happy-dom and the 4 on node must set `environment` through
  `runnerConfig`, and each of the **30 projects with an inherited environment** must have its
  effective value resolved and pinned rather than left to the new default (FR-007, R-3).
- **Pool tuning** on the heaviest projects, if measurement shows it is needed (R-4).
- **Path aliases**, using `vite-tsconfig-paths` with explicit
  `{ root: <core-web>, projects: ['tsconfig.base.json'] }` — never a bare invocation, which crawls
  every tsconfig and segfaults the native resolver on CI (R-5).

The Angular team explicitly does not support `runnerConfig` contents. Keep these files minimal.

### Class 2 — Non-Angular SDK project (`libs/sdk/vue`)

Unchanged. Stays on `@nx/vitest` with its existing `vite.config.mts` `test` block. It is the only
project genuinely on Vitest today (see [data-model.md](../data-model.md)).

## Deletions

| Project | Action |
|---|---|
| `apps/dotcms-block-editor` | **Zero specs.** Delete the `test` target, `karma.conf.js`, `src/test.ts` and the test polyfills entry. Not migrated. |
| `libs/dotcms-js` | `@angular/build:karma` → `@angular/build:unit-test`; delete `karma.conf.js` and the `karmaConfig` option. Already in the Angular builder family, so this is an executor swap, not a rewrite. |
| `apps/dotcdn`, `libs/portlets/dot-locales/data-access`, `libs/sdk/react`, `libs/sdk/types` | **Zero specs.** Disposition undecided — see data-model.md. |

## Generator defaults (FR-010)

`nx.json` → `generators`: set `unitTestRunner: "vitest"` for `@nx/angular:application`,
`@nx/angular:library` and `@nx/react:library`. **Do not touch `e2eTestRunner`**, which sits in the
same block (FR-002a).

Note the asymmetry: the generators emit `@nx/vitest`-style projects, while this migration puts
Angular projects on the Angular builder. A newly generated Angular project will therefore not match
its migrated siblings. Documenting that in `core-web/CLAUDE.md` is part of FR-016 — an undocumented
mismatch is how the workspace drifts back to two conventions by accident rather than by decision.

## Verification per project

1. `nx test <project>` is green.
2. Executed-test count equals the baseline (FR-003a), skips justified (FR-004).
3. `nx show projects --with-target typecheck` count is unchanged workspace-wide (FR-011).
4. The diff for the project's commit passes [diff-allowlist.md](./diff-allowlist.md).
