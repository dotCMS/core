# Spec: Enable TypeScript strict mode in `sdk-analytics`

**Issue:** [#35946](https://github.com/dotCMS/core/issues/35946) — [13/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Implemented
**Conclusion:** **Real work required.** First `libs/sdk/*` project in the rollout that was not already compliant — and it is **not enforced** by any CI gate.

---

## Objective

Bring `sdk-analytics` (`core-web/libs/sdk/analytics`, package `@dotcms/analytics`) to the rollout's strict-mode bar: the six flags from precedent #36879 in the project's own `tsconfig.json`, zero type errors in both `tsconfig.lib.json` and `tsconfig.spec.json`, and an honest verdict on whether anything actually verifies it.

### Assumptions

1. "Strict" means the six flags in the project's own `tsconfig.json`, per `core-web/CLAUDE.md`. The issue's ACs citing `typescript-strict-plugin`, `npx tsc-strict`, and `// @ts-strict-ignore` are **stale** — that approach was dropped and the plugin is not installed anywhere in the repo.
2. "Done" requires flags present **and** zero errors **and** something in CI that verifies it. Clause three is what separated `sdk-types` / `sdk-uve` / `sdk-client` (done) from `dotcms-js` / `utils` (declared but unenforced).

---

## Starting state

`libs/sdk/analytics/tsconfig.json` already carried `"strict": true` but **none of the other five flags**. This breaks the pattern observed in #35942, where every `libs/sdk/*` project was found already strict and already enforced — the SDK tsconfig lineage propagated `strict` alone.

| Check | Before |
|---|---|
| `strict: true` | ✅ present |
| Other 5 flags | ❌ absent |
| `tsc -p tsconfig.lib.json` | 0 errors |
| `tsc -p tsconfig.lib.json` + all 6 flags | **18 errors** |
| `tsc -p tsconfig.spec.json` | **14 errors** |
| Same, with `--strict false` | **still 14** |

That last row is the important one: the 14 spec errors were **pre-existing drift**, not strict-mode fallout. Two independent gaps let them accumulate unnoticed:

- The Nx-inferred `typecheck` target runs `tsc --noEmit -p tsconfig.lib.json` — it **never checks the spec config**.
- `jest.config.ts` transforms with **`babel-jest`**, which strips types without checking them.

---

## Changes

### 1. Flags

Added to `libs/sdk/analytics/tsconfig.json` alongside the existing `strict`:
`forceConsistentCasingInFileNames`, `noImplicitOverride`, `noPropertyAccessFromIndexSignature`, `noImplicitReturns`, `noFallthroughCasesInSwitch`.

`tsconfig.base.json` untouched (stays `"strict": false`). No changes to `tsconfig.lib.json` / `tsconfig.spec.json`.

### 2. Production source — 18 errors, all `TS4111`

Every one came from `noPropertyAccessFromIndexSignature`, and every one was dot access on an index-signature type (`HTMLElement.dataset` → `DOMStringMap`, and a `Record<string, unknown>` of payload properties). Uniform mechanical fix to bracket notation. **No `any`, no `@ts-expect-error`, no type widening.**

| File | Count |
|---|---:|
| `shared/utils/dot-analytics.utils.ts` | 10 |
| `plugin/click/dot-analytics.click-tracker.ts` | 4 |
| `plugin/impression/dot-analytics.impression-tracker.ts` | 3 |
| `plugin/click/dot-analytics.click.utils.ts` | 1 |

Both reads and writes were affected (e.g. `element.dataset['dotAnalyticsDomIndex'] = ...`).

### 3. Specs — 29 errors (14 pre-existing + 15 newly surfaced `TS4111`)

The 15 new ones were the same mechanical `dataset` fix. The 14 pre-existing ones were genuine drift and each needed reading:

| Root cause | Count | Fix |
|---|---:|---|
| `ANALYTICS_CONTENTLET_CLASS` no longer exported — renamed to `CONTENTLET_CLASS` | 2 | Updated imports + 12 usages in the two click specs |
| Pageview fixture put `device` inside `data` and omitted required `locale_id` | 4 | Moved `device` into `context`, added `locale_id`. This was the root cause of **both** the "missing `device`" and the two "`doc_encoding` does not exist on `DotCMSContentImpressionPageData`" errors — with `page` invalid, TS fell through to the impression member of the `DotCMSEvent` union |
| Untyped `jest.fn()` inferring `never` for `mockResolvedValue` / `mockRejectedValue` | 3 | `jest.fn<() => Promise<unknown>>()` |
| `Location` mock missing `host` | 1 | Added `host` |
| `jest.spyOn(...).mockImplementation()` with no argument | 2 | Supplied a no-op |
| `mockInitialize` inferred as zero-arg | 1 | Typed as `(config: DotCMSAnalyticsConfig) => DotCMSAnalytics` |
| `result.custom` — not on `EnrichedTrackPayload` | 1 | Asserted via `as unknown as Record<string, unknown>`, preserving the runtime check rather than deleting it |
| `TS2589` excessively deep instantiation | 1 | Routed one full-structure `toHaveBeenCalledWith` through an untyped mock reference; matching a whole request body against the `DotCMSEvent` union exhausts tsc's instantiation depth |

Fixtures were corrected rather than production types widened. No source bug was found behind any of them.

### 4. Enforcement — verified empirically, and it is **not** enforced

`core-web/CLAUDE.md` notes Vite builds skip type checking, but this project runs `dts({ tsconfigPath: 'tsconfig.lib.json' })` (`vite.config.mts:34`, `vite-plugin-dts@4.5.4`), which invokes the TS compiler to emit declarations — so the build *might* have been the gate.

Tested directly: a deliberate `const __strictProbe: number = "definitely not a number";` was added to `shared/dot-analytics.logger.ts` and `nx run sdk-analytics:build --skip-nx-cache` was run.

> **The build succeeded.** `[vite:dts] Declaration files built in 1002ms.` → `✓ built in 1.16s` → `Successfully ran target build`.

The probe was reverted immediately (file confirmed byte-identical to git).

**Verdict: `sdk-analytics` is strict but unenforced** — same category as #35939 and #35940. `vite-plugin-dts` emits declarations without failing on diagnostics, and CI never invokes `typecheck` (0 matches for it across `.github/workflows/` and `core-web/pom.xml`; CI runs only `affected -t lint`, `format:check`, `run-many -t build`, `sdk-analytics:build:standalone`, and `affected -t test`).

Deliberately **not** fixed here: wiring `nx affected -t typecheck` into `core-web/pom.xml` is monorepo-wide and belongs to the epic, not to project 13 of 44.

---

## Results

| Gate | Result |
|---|---|
| `tsc -p tsconfig.lib.json --noEmit` | **0 errors** |
| `tsc -p tsconfig.spec.json --noEmit` | **0 errors** |
| `nx run sdk-analytics:typecheck` | pass |
| `nx run sdk-analytics:lint` | pass (1 pre-existing warning in `vite.config.mts:42`) |
| `nx run sdk-analytics:test` | pass — **15 suites, 314 tests** |
| `nx run sdk-analytics:build` | pass |
| `nx run sdk-analytics:build:standalone` | pass |

Because jest never type-checked these specs, the `:test` run was the real regression check on the fixture edits — all 314 stayed green.

### Consumers

**0 internal dependents.** The only references to `@dotcms/analytics` outside the lib are doc comments in `libs/sdk/uve/src/internal/constants.ts`. Nothing is widened; no blast radius.

### Note on `nx affected -t build,lint --base=origin/main`

Six projects fail lint: `dotcms-js`, `utils`, `utils-testing`, `block-editor`, `dotcms-webcomponents`, `dotcms-block-editor`. **All pre-existing and unrelated** — legacy `no-explicit-any` / `no-console` / unused-var / `import/order` debt. `affected` picks them up because it diffs the whole branch against `origin/main`, not this change.

Confirmed for the one project this change touches at all: `dotcms-webcomponents:lint` reports **289 errors both with and without** the change (the change there is a single digit inside a comment). Restricting to what this work affects — `nx affected -t lint,build --base=HEAD~1` — everything passes except that same pre-existing `dotcms-webcomponents:lint`.

---

## Commands

```bash
cd core-web
export NVM_DIR="$HOME/.nvm" && . "$NVM_DIR/nvm.sh" && nvm use   # 22.22.3
export NX_NO_CLOUD=true

corepack pnpm exec tsc -p libs/sdk/analytics/tsconfig.lib.json  --noEmit   # 0
corepack pnpm exec tsc -p libs/sdk/analytics/tsconfig.spec.json --noEmit   # 0
corepack pnpm exec nx run sdk-analytics:typecheck --skip-nx-cache
corepack pnpm exec nx run sdk-analytics:lint
corepack pnpm exec nx run sdk-analytics:test --skip-nx-cache
corepack pnpm exec nx run sdk-analytics:build --skip-nx-cache
corepack pnpm exec nx run sdk-analytics:build:standalone --skip-nx-cache
```

> `pnpm` is not on `PATH` in this worktree — use `corepack pnpm`.

## Testing Strategy

No new tests. The 15 existing spec files now type-check for the first time, which is itself the coverage gain. Verification is compilation- and build-based, with `:test` guarding runtime behavior through the fixture corrections.

## Rollout status after this issue

| Issue | Project | Flags | Clean | Enforced | Outcome |
|---|---|:--:|:--:|:--:|---|
| #35935 | `sdk-types` | ✅ | ✅ | ✅ | No-op |
| #35936 | `dotcms` | ❌ | ❌ | ❌ | Dead → #36950 |
| #35937 | `dot-layout-grid` | ❌ | ❌ | ❌ | Dead → #36950 |
| #35938 | `sdk-create-app` | ❌ | 2 err | ✅ | Fixed |
| #35939 | `dotcms-js` | ❌ | 38 err | ❌ | Fixed, unenforced |
| #35940 | `utils` | ❌ | 32+17 err | ❌ | Fixed, unenforced |
| #35941 | `sdk-uve` | ✅ | ✅ | ✅ | No-op |
| #35942 | `sdk-client` | ✅ | ✅ | ✅ | No-op |
| **#35946** | **`sdk-analytics`** | partial | **18+29 err** | ❌ | **Fixed, unenforced** |

Corrects the pattern proposed in #35942 ("every `libs/sdk/*` project is already strict and already enforced"). It holds for Rollup-built SDK libs, which type-check through `@rollup/plugin-typescript`. It does **not** hold for Vite-built ones: `sdk-analytics` inherited `strict` from the shared lineage but neither the other five flags nor a type-checking build.

## Follow-ups for the epic

1. **Unenforced strictness.** `sdk-analytics` joins `dotcms-js` and `utils`. With three projects now in this state, the epic should decide once whether `typecheck` gets wired into CI.
2. **`typecheck` ignores `tsconfig.spec.json`.** The inferred target only covers the lib config, so spec drift is invisible everywhere — this is how 14 errors accumulated silently here, and it applies to every Vite project in the rollout.
