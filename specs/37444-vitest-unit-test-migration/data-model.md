# Phase 1 Data Model: Migration state per project

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md)

There is no application data model here — the "entities" of this feature are **projects** and their
**test wiring**. This file is the authoritative inventory the migration works through, measured at
`31c3672aa5`.

---

## Correction: the issue's runner inventory is wrong

The issue states four projects are "already migrated" to Vitest. **Only one is.** Verified:

| Project | `jest.config.ts` | In Jest `include` list | `test:` block in vite config | Actual runner |
|---|---|---|---|---|
| `libs/sdk/vue` | no | **no** | **yes** (`vite.config.mts:83`) | **Vitest** |
| `libs/sdk/analytics` | **yes** | **yes** | no | **Jest** |
| `libs/sdk/experiments` | **yes** | **yes** | no | **Jest** |
| `libs/edit-content-bridge` | **yes** | **yes** | no | **Jest** |

The latter three carry a `vite.config.mts`, but it is a **build** config with no `test` section, and
they appear in the `@nx/jest/plugin` `include` list. Their Vite config says nothing about how their
tests run.

**Consequences:**

1. The migration is **41 projects / 996 test files**, not "41 globs / ~1,050 specs" — the 26 files in
   those three projects were double-counted as already-done. (996, not 977: see the naming-convention
   correction under the inventory below.)
2. The "working reference" is **one Vue project with 14 specs**, not four. Combined with research
   R-1 (no `TestBed`/Spectator anywhere in it), the workspace offers **no** Angular-plus-Vitest
   precedent at all. This is why Phase 2 of the plan is a decision gate rather than a formality.

### Spec-file census (whole workspace: 1,093)

| Area | Specs | Status |
|---|---|---|
| Jest include list (41 projects) | **996** (977 `*.spec.*` + 19 `*.test.*`) | **In scope — migrate** |
| `libs/sdk/vue` | 14 | Already Vitest — untouched |
| `libs/dotcms-js` (Karma) | 3 | **DELETED** — the Karma executor cannot load (`Cannot find module 'karma'`), so these never ran; decision T006b |
| `apps/dotcms-block-editor` (Karma) | **0** | **DELETED** — dead test wiring removed; its `build` target and `polyfills.ts` are kept, they are live |
| `apps/dotcms-ui-e2e` | 35 | **Out of scope** — E2E, enforced by FR-002a |
| `libs/dotcms-webcomponents` | 7 | **Out of scope** — Stencil |
| Elsewhere (tooling, unlisted dirs) | 57 | Not in any migrated target |

---

## Project inventory (Jest include list — the migration worklist)

**`build`** = has a `build` target. **NO** means `buildTarget` must be set explicitly (research R-10).
**`env`** = declared `testEnvironment`; *(preset default)* means it inherits and must be resolved
per project (FR-007, research R-3).

| Project | Type | build | env | Specs |
|---|---|---|---|---|
| `apps/dotcms-ui` | application | yes | `(preset default)` | 228 |
| `libs/edit-content` | library | **NO** | `(preset default)` | 116 |
| `libs/ui` | library | **NO** | `@happy-dom/jest-environment` | 112 (111 spec + 1 `*.test.*`) |
| `libs/data-access` | library | **NO** | `(preset default)` | 87 |
| `libs/portlets/edit-ema/portlet` | library | **NO** | `(preset default)` | 64 |
| `libs/portlets/dot-experiments/portlet` | library | **NO** | `(preset default)` | 52 |
| `libs/portlets/dot-content-drive/portlet` | library | **NO** | `@happy-dom/jest-environment` | 34 |
| `libs/image-editor` | library | **NO** | `(preset default)` | 30 |
| `libs/block-editor` | library | **NO** | `(preset default)` | 29 |
| `libs/portlets/dot-analytics/portlet` | library | **NO** | `@happy-dom/jest-environment` | 26 |
| `libs/sdk/angular` | library | yes | `(preset default)` | 21 |
| `libs/portlets/edit-ema/ui` | library | **NO** | `(preset default)` | 20 |
| `libs/new-block-editor` | library | **NO** | `(preset default)` | 16 |
| `libs/template-builder` | library | **NO** | `(preset default)` | 16 |
| `libs/sdk/analytics` | library | **NO** | `(preset default)` | 15 |
| `libs/sdk/client` | library | yes | `(preset default)` | 15 |
| `libs/sdk/create-app` | library | yes | `node` | 12 |
| `apps/mcp-server` | application | yes | `node` | 10 |
| `libs/sdk/experiments` | library | yes | `(preset default)` | 9 |
| `libs/portlets/dot-analytics/data-access` | library | **NO** | `(preset default)` | 8 |
| `libs/global-store` | library | **NO** | `(preset default)` | 6 |
| `libs/portlets/dot-velocity-playground` | library | **NO** | `(preset default)` | 6 |
| `libs/sdk/ai` | library | yes | `node` | 6 |
| `libs/portlets/dot-locales/portlet` | library | **NO** | `@happy-dom/jest-environment` | 5 |
| `libs/sdk/uve` | library | yes | `jsdom` | 5 |
| `libs/portlets/dot-categories` | library | **NO** | `(preset default)` | 4 |
| `libs/portlets/dot-plugins` | library | **NO** | `(preset default)` | 4 |
| `libs/portlets/dot-tags` | library | **NO** | `(preset default)` | 4 |
| `libs/dot-rules` | library | **NO** | `@happy-dom/jest-environment` | 3 |
| `libs/portlets/dot-query-tool` | library | **NO** | `(preset default)` | 3 |
| `libs/utils` | library | **NO** | `(preset default)` | 3 |
| `libs/edit-content-bridge` | library | yes | `(preset default)` | 2 |
| `libs/portlets/dot-es-search` | library | **NO** | `(preset default)` | 2 |
| `libs/portlets/dot-experiments/data-access` | library | **NO** | `(preset default)` | 2 |
| `apps/dotcms-binary-field-builder` | application | yes | `(preset default)` | 1 |
| `libs/portlets/dot-content-drive/ui` | library | **NO** | `(preset default)` | 1 |
| `libs/portlets/dot-usage` | library | **NO** | `@happy-dom/jest-environment` | 1 |
| `apps/dotcdn` | application | yes | `(preset default)` | **0** |
| `libs/portlets/dot-locales/data-access` | library | **NO** | `(preset default)` | **0** |
| `libs/sdk/react` | library | yes | `(preset default)` | **18** (`*.test.tsx`) |
| `libs/sdk/types` | library | yes | `node` | **0** |

**Totals**: 41 projects · **996 test files** (977 `*.spec.*` + 19 `*.test.*`) · 28 without a `build`
target · **3** with zero test files.

> **Corrected during implementation.** The first version of this inventory globbed only `*.spec.*`
> and therefore reported 977 files and four empty projects. Two projects use `*.test.*` naming —
> `libs/sdk/react` (18 files, **138 tests**, wrongly listed as empty) and `libs/ui` (1 file). Any
> tooling that enumerates test files must match **both** conventions; the FR-001 allowlist and the
> codemod already do, but the count did not.

### Distribution summary

| Dimension | Breakdown |
|---|---|
| Declared environment | 6 `@happy-dom/jest-environment` · 4 `node` · 1 `jsdom` · **30 inherited** |
| `buildTarget` needed | 28 explicit (non-buildable libs) · 13 default to own `build` |
| Size | 1 project ≥ 200 specs · 5 ≥ 50 · 20 ≤ 5 |

The 30 projects with an inherited environment are the FR-007 risk: reading only declared values
would silently move the **majority** of the workspace to whatever the new runner defaults to
(jsdom, per the builder schema — research R-3).

---

## Three projects have zero test files, and `passWithNoTests` is what hides them

`apps/dotcdn`, `libs/portlets/dot-locales/data-access` and `libs/sdk/types` each carry a `test`
target and a `jest.config.ts` but contain **no test files of either naming convention**.
(`libs/sdk/react` was in this list until the recount above found its 18 `*.test.tsx` files.) They pass today only
because `passWithNoTests: true` is set — in `nx.json` `targetDefaults` *and* in 42 `project.json`
files (research R-11).

FR-004 states no project may pass vacuously. `apps/dotcms-block-editor` sets the precedent the spec
already adopted: a project with zero specs has its dead test target deleted rather than migrated.

**These three need the same disposition decision**, and it is not yet made. Deleting their test
targets is consistent with FR-004 and with the block-editor precedent; keeping them migrated but
empty reintroduces exactly the vacuous pass FR-004 forbids. Raised for `/speckit-tasks` to gate
rather than decided here, because deleting a test target from an SDK library that is *expected* to
grow tests is a judgement call the spec does not cover.

---

## Migration state machine (per project)

```
  BASELINE_CAPTURED ──> MIGRATED ──> VERIFIED ──> COMMITTED
        │                   │            │
        │                   │            └── count parity fails ──> back to MIGRATED
        │                   └── suite red ──────────────────────┘
        └── FR-003b: must precede any config deletion
```

| State | Entry condition | Artifacts |
|---|---|---|
| `BASELINE_CAPTURED` | Test count recorded from the JUnit report while still on the old runner | baseline entry, per project |
| `MIGRATED` | `test` target rewired; `jest.config.ts` deleted; `passWithNoTests` removed; env + `buildTarget` + `isolate` set | `project.json`, optional `vitest-base.config.ts` |
| `VERIFIED` | Suite green · count matches baseline · declared skips justified · `typecheck` target unaffected | comparison output |
| `COMMITTED` | One commit for this project alone (FR-014) | commit |

**Invariant across every state**: no product file and no E2E file appears in the diff (FR-001,
FR-002a). This is checked mechanically, not per state.

---

## Workspace-level entities

| Entity | Before | After |
|---|---|---|
| `nx.json` → `@nx/jest/plugin` | present, 41-glob `include` | **removed** |
| `nx.json` → `@nx/vitest` | present, `testTargetName: "test"` | retained (serves `libs/sdk/vue`) |
| `nx.json` → `targetDefaults.test.inputs` | includes stale `{workspaceRoot}/karma.conf.js` | stale entry removed |
| `nx.json` → `targetDefaults["@nx/jest:jest"]` | `passWithNoTests: true`, `ci` configuration | **removed** |
| `nx.json` → generators | `unitTestRunner: "jest"` ×3 | `"vitest"` ×3 (Angular app, Angular lib, React lib) |
| `jest.preset.js` | reporters, coverage, `workerIdleMemoryLimit`, dead `snapshotFormat` | **deleted** |
| `package.json` | 13 Jest packages | 2 retained + annotated; 11 removed |
| `core-web/pom.xml` | `--detectOpenHandles --forceExit` | flags removed; `NODE_OPTIONS` rationale corrected |
| `core-web/tools/` | — | 3 new scripts (FR-001a) |
