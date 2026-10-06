# Spec: Enable TypeScript strict mode in `data-access`

**Issue:** [#35948](https://github.com/dotCMS/core/issues/35948) — [15/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Implemented
**Conclusion:** **83 real errors fixed.** The flags had been present and completely inert. Still unenforced by CI, by explicit decision.

---

## Objective

Bring `data-access` (`core-web/libs/data-access`) to the rollout's strict-mode bar. First non-isolated project in the rollout: **27 direct dependents** (the issue said 23), of which **6 were already strict**.

The issue's ACs were stale (`typescript-strict-plugin` / `npx tsc-strict` / `@ts-strict-ignore`); corrected before starting, per `core-web/CLAUDE.md`.

---

## Starting state — flags present, and doing nothing

All six flags plus Angular's `strictTemplates` / `strictInjectionParameters` / `strictInputAccessModifiers` had been in `libs/data-access/tsconfig.json` for some time. They were inert:

- The project has **no `build` target** (only `lint` and `test`), so its own tsconfig is never consumed by any build.
- Its 27 dependents compile these sources under **their own** non-strict configs.

So 36 errors sat in a layer-3 shared services hub with CI fully green. This matches PR #36957's incidental measurement of `data-access` dropping 106 → 68 → 36 as `dotcms-js` and `utils` were fixed.

| Config | Before | After |
|---|---:|---:|
| `tsconfig.lib.json` | **36** | **0** |
| `tsconfig.spec.json` | 84 (47 own spec + 37 lib pulled in) | **0** |

Neither config had a masking error (`TS6053` / `TS2688`), so both were completing real semantic passes — the errors were simply never looked at.

### `test` does not type-check — anywhere in this monorepo

`jest.config.ts` uses `jest-preset-angular` → `ts-jest@29.4.6` against `tsconfig.spec.json`, which looks like it would type-check. It does not, because that tsconfig sets `isolatedModules: true`:

- `ts-jest/dist/legacy/config/config-set.js:229` — `this.isolatedModules = this.parsedTsConfig.options.isolatedModules ?? false`, reading TypeScript's flag into ts-jest's own.
- `ts-jest/dist/legacy/compiler/ts-compiler.js:74` — the language-service host is constructed only `if (!this.configSet.isolatedModules)`.
- `_doTypeChecking()` computes diagnostics through `this._languageService.getSemanticDiagnostics(...)` (lines 181, 427), which needs that host.

`data-access` is the proof: **84 `tsc` errors alongside 754 passing tests.**

> This is repo-wide. `core-web/CLAUDE.md` *mandates* `isolatedModules: true` in every `tsconfig.spec.json`, so no project's `test` target type-checks its specs. It also means "tests pass" was never evidence of spec type-cleanliness — including in the reasoning published on #35947, which has been corrected there. That verdict stands because it was measured with `tsc -p` directly; the justification was wrong.

---

## Changes — production source (36)

### `paginator/paginator.service.ts` (14)

- **8 × `TS2564`** — fields populated from response headers, not the constructor. Given zero values (`0` / `''`) rather than `!`, which states "empty until the first request" honestly.
  - **`_sortOrder` deliberately left `?: OrderDirection`.** Defaulting it to `OrderDirection.ASC` would have been a **behaviour change**: `getParams()` gates the `direction` param on truthiness (`if (this.sortOrder)`), and `ASC === 1` is truthy where `undefined` was not, so every paginated request would have started sending a param it previously omitted. The public getter widened to `OrderDirection | undefined` instead.
- **5 × `TS2345`** — `response.headers.get()` returns `string | null`. Four `parseInt(...)` calls take `?? ''` (identical `NaN` outcome); the fifth revealed `private setLinks(linksString: string)` whose body already handled null (`linksString?.split(',') || []`) — the signature was simply wrong and is now `string | null`.
- **1 × `TS7053`** — `this.links[rel] = url` with `rel: string`. The parser stores whatever `rel` the server sends, so the file-local `interface Links` gained `[key: string]: string | undefined`.

### `dot-page-state/dot-page-state.service.ts` (12)

Types that were simply wrong — the service genuinely emits `null`. Widened rather than asserted, per the `dotcms-js` precedent (#35939):

| Site | Was | Now |
|---|---|---|
| `getInternalNavigationState()` | `DotPageRenderState` | `\| null` — it has an explicit `return null` |
| `setLock(options, lock)` | `lock: boolean = null` | `boolean \| null` |
| `getLockMode()` | `Observable<string>` | `Observable<string \| null \| undefined>` — returns `of(null)`, and `map(x => x?.message)` |
| `handleSetPageStateFailed()` | `Observable<DotHttpErrorHandled>` | `Observable<undefined>` — it ends with `map(() => undefined)` |
| `getFavoritePage()` | `Observable<DotCMSContentlet>` | `\| undefined` |
| `getRunningExperiment()` | `Observable<DotExperiment>` | `\| null` |

`handleSetPageStateFailed` was the key one: because it really emits `undefined`, the caller's `= [null, null]` destructuring default is **reachable and load-bearing**, not dead code. Declaring the honest type made the whole `switchMap` typable, and it was rewritten to destructure explicitly (`const [page, user] = result ?? [null, null]`) instead of fighting an annotation.

> **One deliberate guard change:** `if (page)` became `if (page && user)`. `forkJoin` emits both or the error path yields `undefined`, so `page` truthy already implied `user` present; this makes that explicit instead of asserting non-null. Behaviourally unreachable, but worth naming.

Plus one `TS4111` (`queryParams['url']`) and one `TS18048` (`state.viewAs.language?.id || 1`, mirroring what line 290 in the same file already did).

### Three smaller services (10)

| File | Fix |
|---|---|
| `dot-router.service.ts` (5) | `previousUrl` → `string \| undefined` (`PortletNav.previousUrl` is optional); `storedRedirectUrl` getter/setter → `string \| null` (`redirectMain` assigns null); `nav?.finalUrl` guard; `navExtras.queryParams` rebuilt via spread instead of indexing a possibly-null object |
| `dot-localstorage.service.ts` (3) | typed the `getValue` param; `setItem` builds a `string` with `String(value)` (matching what `localStorage` did implicitly); `getValue` early-returns `null` for a missing key — provably the same result as the old `parseInt(null)` → `JSON.parse(null)` path |
| `dot-content-types-info.service.ts` (2) | `prop` narrowed to `keyof (typeof this.contentTypeInfoCollection)[number]`; `result` initialised to `''`, which is falsy exactly like the previous `undefined` and keeps the public `string` contract of `getIcon` / `getClazz` / `getLabel` |

---

## Changes — specs (47)

**25 of the 47 came from three lines.** `dot-router.service.spec.ts`'s fake `Router` declared `navigate = jest.fn(() => ...)` etc., which infers **zero parameters**, so every `toHaveBeenCalledWith(...)` was a `TS2554`. Typing the three mocks fixed all 25.

> Note for later projects: this file's `jest` comes from `@types/jest`, which uses `jest.fn<TReturn, TArgs>` — **two** type parameters. `@jest/globals` (used in `sdk-analytics`) uses `jest.fn<Fn>`. Passing the wrong arity yields `TS2743`.

The remaining 22 were fixture drift, and two were real bugs hiding behind disabled suites:

| Site | Finding |
|---|---|
| `dot-global-message.service.spec.ts` | Imported `DotMessageService` from `dot-alert-confirm.service`, **which does not export it**. The suite is `xdescribe`d, so nobody noticed. Pointed at `dot-messages.service`. |
| `dot-ai.service.ts` | `export { DotAiProviderConfig }` on a type — invalid under `isolatedModules`. Now `export type`. A **production** file, surfaced only by the spec config. |
| `dot-page-layout.service.spec.ts` | Called `save(id, mockDotLayout())`, but `save` takes a `DotTemplateDesigner` (`{ layout, themeId }`) and posts it verbatim — which is what the real caller (`edit-ema-layout.component.ts:111`) sends. The spec was testing a shape production never uses; now uses a `DotTemplateDesigner`, call and body assertion together. |
| `dot-content-drive.service.spec.ts` | Used `offset`, removed from `DotContentDriveSearchRequest` in favour of cursors. **The model's own JSDoc example still showed `offset`** — that stale doc is what the spec was copied from, and it was fixed too. |
| `dot-edit-page-resolver.service.spec.ts` | `const route: any = jest.spyOn(ActivatedRouteSnapshot, 'toString')` — abusing `spyOn` to obtain a mutable object, on a method that is not a static. Replaced with `{}`. |
| `dot-folder`, `dot-personas`, `dot-system-config`, `dot-themes`, `add-to-bundle`, `push-publish` | Incomplete or over-specified fixtures. `dot-personas` now reuses the existing `mockDotPersona` from `@dotcms/utils-testing` rather than hand-rolling 21 fields; `dot-themes` dropped a `host` key that `DotTheme` does not have. |
| `dot-license`, `dot-localstorage`, `dot-seo-meta-tags-util`, `dot-generate-secure-password`, `dot-upload` | `let result: boolean \| undefined`; `listen<string>('hola')`; local array annotations widened to match `areAllFalsyOrEmpty`'s already-nullable parameter; bracket access; `this` annotation on a `FormDataMock`. |

No new `any`. No `@ts-ignore` / `@ts-expect-error` anywhere in the diff.

---

## Results

| Gate | Result |
|---|---|
| `tsc -p tsconfig.lib.json --noEmit` | **36 → 0** |
| `tsc -p tsconfig.spec.json --noEmit` | **84 → 0** |
| `nx run data-access:lint` | pass |
| `nx run data-access:test` | pass — 79 suites, **754 tests**, unchanged |
| `nx affected -t build` (scoped) | all 6 projects pass |

`tsc -p` on both configs **is** the acceptance test here: there is no build to lean on, and `test` does not type-check.

### Dependent blast radius — measured before and after

Zero new errors anywhere, and three dependents went fully clean:

| Strict dependent | Before | After |
|---|---:|---:|
| `global-store` | 36 | **0** |
| `portlets-dot-analytics-data-access` | 36 | **0** |
| `portlets-dot-experiments-data-access` | 36 | **0** |
| `image-editor` | 148 | 111 |
| `portlets-dot-analytics` | 151 | 115 |
| `portlets-dot-locales-portlet` | 147 | 111 |
| `utils-testing` | 0 | 0 |

**218 errors removed across dependents.** The three that hit zero were carrying nothing but `data-access`'s leakage. This is the "high leverage" the issue predicted, quantified.

Runtime guard on the widening and the paginator initialisers — the two heaviest non-strict consumers:

| Project | Result |
|---|---|
| `dotcms-ui` | 81 suites, **820 tests** pass |
| `ui` | 221 suites, **2184 tests** pass |

### Pre-existing lint failures, unrelated

`nx affected -t lint` fails for `dotcms-js`, `utils`, `utils-testing`, `block-editor`, `dotcms-webcomponents`, `dotcms-block-editor` — legacy `no-explicit-any` / `no-console` / unused-var / `import/order` debt, established as pre-existing while working #35946 (`dotcms-webcomponents` reports 289 errors with and without any change). None of the reported files are in this diff. All **builds** pass.

---

## Enforcement — still none, by decision

No `typecheck` target and no CI wiring, consistent with how `dotcms-js` (#35939) and `utils` (#35940) were resolved. `data-access` therefore joins them and `sdk-analytics` (#35946) as **strict, clean, and unverified**: nothing will catch a regression of these 83 fixes.

That is now four projects, and with the ts-jest finding above the gap is wider than the epic assumed. Raised on #35932.

## Commands

```bash
cd core-web
export NVM_DIR="$HOME/.nvm" && . "$NVM_DIR/nvm.sh" && nvm use   # 22.22.3
export NX_NO_CLOUD=true

corepack pnpm exec tsc -p libs/data-access/tsconfig.lib.json  --noEmit   # 0
corepack pnpm exec tsc -p libs/data-access/tsconfig.spec.json --noEmit   # 0
corepack pnpm exec nx run data-access:lint
corepack pnpm exec nx run data-access:test --skip-nx-cache
corepack pnpm exec nx run-many -t test -p dotcms-ui ui --skip-nx-cache
```

> `pnpm` is not on `PATH` in this worktree — use `corepack pnpm`.

## Rollout status after this issue

| Issue | Project | Flags | Clean | Enforced | Outcome |
|---|---|:--:|:--:|:--:|---|
| #35935 | `sdk-types` | ✅ | ✅ | ✅ | No-op |
| #35936 / #35937 | `dotcms`, `dot-layout-grid` | ❌ | ❌ | ❌ | Dead → #36950 |
| #35938 | `sdk-create-app` | ❌ | 2 err | ✅ | Fixed |
| #35939 | `dotcms-js` | ❌ | 38 err | ❌ | Fixed, unenforced |
| #35940 | `utils` | ❌ | 32+17 err | ❌ | Fixed, unenforced |
| #35941 / #35942 | `sdk-uve`, `sdk-client` | ✅ | ✅ | ✅ | No-op |
| #35946 | `sdk-analytics` | partial | 18+29 err | ❌ | Fixed, unenforced |
| #35947 | `sdk-angular` | ✅ | ✅ | ✅ | Compliant; dead config removed |
| **#35948** | **`data-access`** | ✅ inert | **36+47 err** | ❌ | **Fixed, unenforced** |

A pattern worth stating for the remaining 29: **flags present ≠ flags applied.** `data-access` had all six for some time with 36 errors behind them, because a library with no `build` target never has its own tsconfig read. Expect the same wherever `nx show project` lists no `build`.

## Follow-ups for the epic

1. **Four projects are now strict-but-unenforced** (`dotcms-js`, `utils`, `sdk-analytics`, `data-access`). Needs one decision, not four.
2. **`isolatedModules: true` makes every `test` target transpile-only.** No spec type-checking anywhere; `CLAUDE.md` mandates the flag. Removing it would turn checking on monorepo-wide — an epic-level call, deliberately not made here.
3. **Audit for inert flags.** Any project without a `build` target may already claim strictness while carrying errors. `data-access` was the first one measured; it had 36.
