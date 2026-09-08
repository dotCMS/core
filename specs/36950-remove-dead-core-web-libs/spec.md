# Issue Resolution Specification: Remove dead core-web libraries (`libs/dotcms`, `libs/dot-layout-grid`)

**Feature Branch**: `nicobytes/36950-remove-dead-core-web-libraries-libsdotcms-libsdot-layout-grid`

**Created**: 2026-08-20

**Status**: Draft

**Type**: Issue / Bug Resolution (dead-code removal / maintenance)

**Related GitHub Issue**: [#36950](https://github.com/dotCMS/core/issues/36950)

**Input**: User description: "https://github.com/dotCMS/core/issues/36950"

<!--
  Defect framing note: the "defect" here is repository state, not runtime behavior. Two Nx
  projects exist in the workspace that no code consumes and that no longer compile. They
  cost maintenance effort (every mechanical sweep touches them) and they blocked two
  sub-issues of the strict-mode epic. Reproduction is therefore a build/graph observation
  rather than a UI sequence.
-->

## Problem Statement *(mandatory)*

The `core-web` Nx workspace contains two libraries that are dead code:

| Library | What it is | Superseded by |
|---|---|---|
| `core-web/libs/dotcms` (43 files) | Legacy `initDotCMS` JavaScript SDK, published to npm as `dotcms@0.0.21` | `@dotcms/client` |
| `core-web/libs/dot-layout-grid` (20 files) | Vendored `angular2-grid` (`NgGrid`) layout-grid directives | GridStack, in `libs/template-builder` |

Neither has a single consumer anywhere in the repository, neither is built, linted, or tested
by CI, and **neither compiles** against the workspace's current Angular 22 / TypeScript
configuration. They still appear in the Nx project graph, in `tsconfig.base.json`, in
`core-web/README.MD`, and in a now-broken `package.json` docs script.

They surfaced during the TypeScript strict-mode rollout (epic #35932): enabling `strict` on
either project would mean fixing type errors in code nobody uses, with no CI target compiling
it to verify the fix. Both rollout sub-issues were closed as not-applicable (#35936, #35937)
and the epic was rescoped from 44 to 42 projects, leaving this removal as the outstanding work.

**Severity / Impact**: Low severity, no runtime or customer impact. The cost is to maintainers:
every workspace-wide sweep (Angular v19/v20/v21/v22 upgrades, Nx 23, ESLint 9, the `inject()`
codemod) has had to touch these projects for no benefit, and their presence in the project
graph produces misleading automation output — the 44 strict-mode sub-issues were generated
from the graph without checking whether each project was alive.

## Reproduction *(mandatory)*

**Environment**: `core-web` Nx workspace on `main`; Node 22.22.3 (`.nvmrc`), pnpm,
`@angular/core` 22.1.0, `tsconfig.base.json` with `"strict": false`.

**Steps to Reproduce**:

1. `cd core-web && pnpm install`
2. Observe both projects still exist in the graph:
   `pnpm exec nx show projects --json | jq -r '.[]' | grep -E '^(dotcms|dot-layout-grid)$'`
3. Attempt to compile `libs/dotcms` with every strict flag explicitly disabled:
   `pnpm exec tsc -p libs/dotcms/tsconfig.lib.json --noEmit --strict false --noPropertyAccessFromIndexSignature false --noImplicitReturns false`
4. Attempt to compile `libs/dot-layout-grid` with no strict flags at all:
   `pnpm exec tsc -p libs/dot-layout-grid/tsconfig.lib.json --noEmit`
5. Search the whole repository for any consumer of either library (path alias, exported
   symbol, or — for the directive library — template selector).

**Expected Behavior**: A project present in the Nx workspace compiles, is covered by at least
one CI target, and has at least one consumer (or a published-package contract that justifies
its existence).

**Actual Behavior**:

- Step 2 prints both project names — they are still in the graph.
- Step 3 fails with `TS2307` on `'dotcms-models'`, an import path alias that no longer exists
  (the current alias is `@dotcms/dotcms-models`), from
  `libs/dotcms/src/lib/api/DotApiContentType.ts:1`, `libs/dotcms/src/lib/api/DotApiForm.ts:1`
  and `libs/dotcms/src/lib/api/DotApiForm.spec.ts:1`.
- Step 4 fails because `libs/dot-layout-grid/src/lib/directives/NgGrid.ts:8` imports
  `ComponentFactoryResolver` from `@angular/core`, which modern Angular no longer exports
  (deprecated v13, since removed; the workspace is on 22.1.0).
- Step 5 finds **zero** consumers. Evidence below.

**Reproducibility**: Always — this is static repository state, not a timing- or data-dependent
defect.

### Evidence: `libs/dotcms` has no consumers

| Check | Result |
|---|---|
| `from '@dotcms/dotcms'`, `from 'dotcms'`, `require('dotcms')`, `import('dotcms')` repo-wide | **0 importers** |
| Occurrences of the `@dotcms/dotcms` alias | 1 — the alias definition itself, `core-web/tsconfig.base.json:36` |
| All 44 exported symbols, word-boundary grep repo-wide | **0 hits outside the library** |
| `"dotcms":` as a dependency key in any `package.json` | **0** |
| CI build | `core-web/pom.xml:196` runs `nx run-many -t build --exclude=tag:skip:build`; `project.json` tags are `["skip:test","skip:lint","skip:build"]` → never built, linted, or tested |
| CI publish | No workflow invokes `nx publish` or `tools/scripts/publish.mjs`; that script ends in `npm publish --dry-run` regardless |
| Last functional commit | `66f1a15917`, 2022-10-05. Everything since is formatting/upgrade sweeps. |

Name-collision caution: `DotCMSNavigationItem`, `DotPageAssetLayoutBody/Row/Column/Sidebar`
and `DotCMSContainerStructure` are **independently declared** in `libs/sdk/types`
(`@dotcms/types`) and are live. They are not references to this library.

### Evidence: `libs/dot-layout-grid` has no consumers

Because this is a library of Angular **directives**, consumable from templates by selector, the
path-alias check alone is not sufficient proof — selectors were checked too.

| Check | Result |
|---|---|
| Occurrences of the `@dotcms/dot-layout-grid` alias | 1 — the alias definition itself, `core-web/tsconfig.base.json:34` |
| All 12 exported symbols (`NgGrid`, `NgGridItem`, `NgGridModule`, `NgGridConfig`, `NgGridItemConfig`, …) | **0 hits outside the library** |
| Declared selectors `[ngGrid]`, `[ngGridItem]`, `ng-grid-placeholder` in `.html` / `.vtl` / `.jsp` / `.ts` / `.java` | **0 hits repo-wide** |
| `angular2-grid`; the vendored `NgGrid.css` | **0** external references — the CSS is not wired into `angular.json` styles or any SCSS |
| Build target | **None at all** — `project.json` defines only `test`; tags are `["skip:test","skip:lint"]` |
| npm | Never published — the registry returns 404 for `dot-layout-grid` |
| Last consumer | Deleted in `5e75d4ddb0` (2024-08-22, *"remove unused code from old template builder"*). Confirmed by inspecting `5e75d4ddb0^`, which contained `dot-edit-layout-grid.component.{ts,html,spec.ts}` plus two models importing `NgGridItemConfig`. Orphaned ~2 years. |

One benign false positive to ignore:
`.agents/skills/angular-developer/references/angular-aria.md` documents Angular Aria's
unrelated `ngGrid`/`ngGridRow`/`ngGridCell` directives.

## Scope of Investigation *(mandatory)*

- **Affected area**: `core-web` frontend build/tooling only — the Nx project graph, TypeScript
  path aliases, the Jest plugin `include` glob, and workspace documentation. No product
  capability, portlet, or user-facing surface is involved.
- **Suspected surface**: **Neither** modern `com.dotcms.*` nor legacy `com.dotmarketing.*` — no
  Java is touched. Principle I (Legacy-Aware Development) is not engaged: nothing under
  `dotCMS/src/main/java` changes, and the deleted code is frontend TypeScript with no backend
  counterpart. The `libs/dotcms` library is itself legacy frontend code, but it is being
  removed outright rather than progressively enhanced, which is the correct treatment for code
  with zero consumers and a documented successor (`@dotcms/client`).
- **Related known decisions**: None known. `/speckit-plan` will formally consult
  `dotCMS/platform-adrs` via the `before_plan` hook; a dead-code deletion with a
  published successor is not expected to be governed by a binding ADR, and that result must
  be recorded rather than assumed.

## Root-Cause Hypothesis

Not a code defect — an accumulation of unremoved artifacts. Two separate lapses:

1. **`libs/dot-layout-grid`**: commit `5e75d4ddb0` correctly deleted the old template-builder
   consumers but left the now-orphaned library, its tsconfig alias, its README row, and its
   `ng-package*.json` files in place.
2. **`libs/dotcms`**: the SDK was superseded by `@dotcms/client` and the npm package was
   deprecated, but the workspace source was never deleted. The `skip:build`/`skip:lint`/
   `skip:test` tags then hid the resulting rot — including a broken `'dotcms-models'` import
   and a `build:docs:dotcms` script pointing at a `typedoc.json` that does not exist — from
   every CI gate.

The systemic root cause is that both projects opted out of all CI targets via `skip:` tags,
so nothing ever reported them as broken.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Delete `core-web/libs/dotcms/` (43 files).
- Delete `core-web/libs/dot-layout-grid/` (20 files), including the orphaned
  `ng-package.json` / `ng-package.prod.json` that point at `dist-lib/dot-layout-grid`, a path
  referenced nowhere.
- Remove both path aliases from `core-web/tsconfig.base.json` (lines 34 and 36).
- Remove `"libs/dotcms/**/*"` from the `@nx/jest/plugin` `include` array in
  `core-web/nx.json` (line 227).
- Remove the broken `"build:docs:dotcms"` script from `core-web/package.json` (line 16) — it
  invokes `typedoc --options libs/dotcms/typedoc.json`, and that file does not exist.
- Remove both rows from the project table in `core-web/README.MD` (lines 12 and 13).
- Remove the stale `"core-web/libs/dot-layout-grid"` triage area from
  `.claude/triage-config.json` (line 31).

**Explicitly out of scope / non-goals**:

- **`npm deprecate dotcms`** — already done. The registry reports
  `deprecated = 'Please use @dotcms/client'`. No npm action is required.
- **Closing #35936 / #35937 and rescoping epic #35932** — already done. Both sub-issues are
  CLOSED and the epic body already states 42 projects and cites #36950 for both removals.
- **Unpublishing `dotcms@0.0.21` from npm.** The package stays published (still ~439
  downloads/month). Deleting monorepo source does not and must not unpublish it.
- Removing or altering any `skip:` tag on the *remaining* projects, or changing the
  `--exclude=tag:skip:build` behavior in `core-web/pom.xml`.
- Any strict-mode work on other projects in epic #35932 — separate issues.
- Touching the live siblings that merely look related: `@dotcms/dotcms-js`,
  `@dotcms/dotcms-models`, `@dotcms/dotcms-webcomponents`, `build:docs:dotcms-models`, the
  workspace-wide `test:dotcms` / `lint:dotcms` scripts, and the generic `'@dotcms/**'`
  import-order rule at `core-web/eslint.config.mjs:174`.
- Auditing the rest of the workspace for further dead projects — the issue already records
  that sweep, and these two were its only findings.

## Regression Risk *(mandatory)*

- **Blast radius**: Effectively none. Nothing imports either library, so no code path is
  shared. The realistic failure modes are mechanical, not behavioral:
  1. A malformed `tsconfig.base.json` after removing two alias lines (trailing comma / JSON
     validity), which would break path resolution workspace-wide. Caught by
     `nx graph` and any build.
  2. Removing the wrong `package.json` script (`build:docs:dotcms-models` is live and must
     stay).
  3. Removing the wrong `tsconfig.base.json` lines — the alias list is alphabetical, so the
     `@dotcms/dotcms*` siblings sit immediately below the target.
- **Backward compatibility**: No API, DB schema, ES mapping, or serialized state is touched,
  so this is **not** a rollback-unsafe change under
  `docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`. The one external contract is the already-deprecated
  npm package `dotcms@0.0.21`, which remains published and installable; its source simply stops
  living in this monorepo. Both libraries are fully recoverable from git history.
- **Data considerations**: None. No migration, no stored data, no content model impact.

## Acceptance & Verification *(mandatory)*

- **AC-001**: `pnpm exec nx show projects --json` no longer lists `dotcms` or `dot-layout-grid`.
  (Note: `nx show projects` emits a single-line JSON array when stdout is not a TTY, so a
  line-oriented `grep` needs `--json | jq -r '.[]'`. Corrected after the spec was approved —
  command form only, no change to any acceptance criterion.)
- **AC-002**: `core-web/libs/dotcms/` and `core-web/libs/dot-layout-grid/` do not exist.
- **AC-003**: A repository-wide search for `@dotcms/dot-layout-grid`, `"@dotcms/dotcms"`,
  `libs/dotcms/` and `libs/dot-layout-grid` returns no results.
- **AC-004** (regression): `pnpm exec nx affected -t build,lint,test --base=origin/main`
  is green.
- **AC-005** (regression): `pnpm exec nx graph --file=/tmp/graph.json` succeeds, proving
  `tsconfig.base.json` is still valid JSON with correct path resolution.
- **AC-006**: `core-web/pnpm-lock.yaml` is unchanged (neither library has a lockfile entry),
  and `git status` shows only the intended deletions plus the 5 edited files.
- **AC-007**: The live siblings listed under non-goals are untouched — specifically
  `build:docs:dotcms-models` in `core-web/package.json` and the `@dotcms/dotcms-js`,
  `@dotcms/dotcms-models`, `@dotcms/dotcms-webcomponents{,/loader}` aliases in
  `core-web/tsconfig.base.json`.

**Verification method**:

Per Principle V, the test-type decision is stated explicitly rather than left silent:

> **No new unit, integration, Postman, Karate, or e2e test is added.** This change deletes
> code with zero consumers and zero call sites; there is no behavior to assert. A test here
> would assert the absence of a file, which the build and graph checks below already do more
> directly and more durably.

The Red → Green gate is an executable removal assertion, run before and after the deletion:

```bash
cd core-web

# RED — must hold BEFORE implementation
pnpm exec nx show projects --json | jq -r '.[]' | grep -qE '^(dotcms|dot-layout-grid)$' && echo "RED: still present"
grep -Ec '"@dotcms/dotcms"|"@dotcms/dot-layout-grid"' tsconfig.base.json   # expect 2

# GREEN — must hold AFTER implementation
pnpm exec nx show projects --json | jq -r '.[]' | grep -qE '^(dotcms|dot-layout-grid)$' || echo "GREEN: gone"
grep -Ec '"@dotcms/dotcms"|"@dotcms/dot-layout-grid"' tsconfig.base.json   # expect 0
```

The regression gate is the existing suite (AC-004/AC-005), re-run in CI by
`core-web/pom.xml`, which is the authoritative signal for the implementation PR.

## Assumptions

- The team accepts that `dotcms@0.0.21` stays published on npm, already deprecated with a
  pointer to `@dotcms/client`, and that removing the monorepo source does not affect existing
  installs. (The issue's AC asked for a `npm deprecate` run; it was already performed.)
- `node_modules` must be installed in a fresh worktree before any Nx verification command can
  run, and Node must match `.nvmrc` (22.22.3).
- Line numbers cited here reflect current `main`; they have already drifted once from the
  issue body (aliases are at `tsconfig.base.json` 34 and 36, not 33 and 35), so the
  implementation should match on content, not on line number.
- No binding ADR governs this removal. `/speckit-plan` confirms this via
  `/speckit-adr-context`; if one is found, this spec needs revision and re-approval.
