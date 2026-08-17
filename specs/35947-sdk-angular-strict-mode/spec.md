# Spec: Enable TypeScript strict mode in `sdk-angular`

**Issue:** [#35947](https://github.com/dotCMS/core/issues/35947) — [14/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Implemented
**Conclusion:** **Already compliant and genuinely enforced.** The only real defect was dead config that made one tsconfig unverifiable.

---

## Objective

Verify whether `sdk-angular` (`core-web/libs/sdk/angular`, package `@dotcms/angular`) meets the rollout's strict-mode bar, and close the issue with evidence rather than manufacturing a diff.

It does — the fourth already-compliant project after #35935, #35941 and #35942. But unlike those three, it was not a *clean* no-op: `tsc -p tsconfig.spec.json` could not run at all.

### Assumptions

1. "Strict" means the six flags from precedent #36879 in the project's own `tsconfig.json`, per `core-web/CLAUDE.md`. The issue's ACs citing `typescript-strict-plugin`, `npx tsc-strict` and `// @ts-strict-ignore` are **stale** — that approach was dropped and the plugin was never installed.
2. "Done" requires flags present **and** zero errors **and** something in CI that verifies it.

---

## Evidence

### 1. All six flags already present

`libs/sdk/angular/tsconfig.json` carries every one — `forceConsistentCasingInFileNames`, `strict`, `noImplicitOverride`, `noPropertyAccessFromIndexSignature`, `noImplicitReturns`, `noFallthroughCasesInSwitch` — plus Angular's own `strictTemplates`, `strictInjectionParameters` and `strictInputAccessModifiers` in `angularCompilerOptions`.

Nothing was added. Re-adding flags that are already there would have been a cosmetic diff.

### 2. The real defect: `tsconfig.spec.json` had never been type-checked

Both `tsconfig.lib.json` and `tsconfig.spec.json` referenced a `next/` directory that **existed and was removed** — the references were added on 2025-03-21 (`09e879b2ac`, `feat(sdk): Add dotCMSShowWhen directive`) and left behind when the directory went away.

One of them was fatal. `tsconfig.spec.json` listed `next/test-setup.ts` in **`files`**, so:

```
error TS6053: File '.../libs/sdk/angular/next/test-setup.ts' not found.
  The file is in the program because:
    Part of 'files' list in tsconfig.json
```

`tsc` aborts on that **before semantic checking**. So this config had never completed a single semantic pass, and any error count taken from it was meaningless. Same masking class as the `TS2688` trap already documented in `core-web/CLAUDE.md`, and the same class that hid `utils-testing`'s 33 real errors in #35944.

The asymmetry is worth knowing: a **non-matching `include` glob is harmless**, a **missing `files` entry is fatal**. That is why `tsconfig.lib.json` — which only had `next/` in `include`/`exclude` — kept working fine.

**Fix:** removed the dangling references from both files. `files`: `["src/test-setup.ts", "next/test-setup.ts"]` → `["src/test-setup.ts"]`; plus the three `next/` globs from the spec `include`, the `next/**/*.ts` entry from the lib `include`, and the three `next/` entries from the lib `exclude`. Nothing else changed.

### 3. Clean once measurable

| Config | Errors |
|---|---:|
| `tsconfig.lib.json` | **0 own** |
| `tsconfig.spec.json` (first real pass) | **0 own** |

Both report exactly one error, and it is neither this project's nor strict-mode's: `TS2307: Cannot find module 'virtual:sdk-version'` in `libs/sdk/client/src/lib/client/adapters/fetch-http-client.ts` — a Vite virtual module raw `tsc` cannot resolve but the build can. Pre-existing, already documented in the `sdk-react` section of PR #36957.

The spec config coming out clean was predicted rather than lucky: `jest.config.ts` transforms via `jest-preset-angular@17` → `ts-jest@29.4.6`, with `diagnostics` not disabled and transpile-only not set, reading `compilerOptions` from `tsconfig.spec.json`. So the specs *had* been type-checked all along — just per-file by ts-jest, never as a whole program. This is the exact opposite of `sdk-analytics` (#35946), where `babel-jest` stripped types and hid 14 errors.

### 4. Clean without escape hatches

Production source only (specs excluded), **33 `.ts` files**:

| Pattern | Count |
|---|---:|
| `: any` / `<any>` / `any[]` | **2** |
| `@ts-ignore` / `@ts-expect-error` | **0** |
| Non-null assertions | **0** |

Both `any`s are the same declaration — `export type DynamicComponentEntity = Promise<Type<any>>` (`lib/models/index.ts:12`) and its JSDoc. `Type<any>` is idiomatic Angular for dynamically-loaded components, and this is an exported public type, so narrowing it is a public-API change rather than strict-mode work. Left alone deliberately.

### 5. Enforced — proved by negative test

`build` uses `@nx/angular:package` (ng-packagr → ngtsc), which type-checks. Rather than infer, this was tested: a deliberate `const __strictProbe: number = "definitely not a number";` was added to `lib/store/dotcms.store.ts` and `nx run sdk-angular:build --skip-nx-cache` was run.

> **The build failed**, exit code 1:
> `libs/sdk/angular/src/lib/store/dotcms.store.ts:93:7 - error TS2322: Type 'string' is not assignable to type 'number'.`

Probe reverted immediately; file confirmed byte-identical to git.

There is no inferred `typecheck` target and none is needed — per `core-web/CLAUDE.md`, adding one to a project whose build already type-checks is redundant.

### 6. That build runs in CI

`project.json` tags are all `npm:*` — no `skip:build` / `skip:lint` / `skip:test`. CI runs `nx run-many -t build --exclude=tag:skip:build` via the `build-test` execution in `core-web/pom.xml`, which has no `<skip>` element, plus `affected -t lint` and `affected -t test`. `@dotcms/angular` is published to npm and matches the `sdk-*` glob in the SDK release pipeline, so the same type-checked build gates every release.

### 7. Consumers

**0 internal dependents.** `@dotcms/angular` appears outside the lib only as a string literal in `libs/sdk/create-app/src/constants/index.ts` (a scaffolding dependency list), not an import. No blast radius.

---

## Results

| Gate | Result |
|---|---|
| `tsc -p tsconfig.lib.json --noEmit` | 0 own errors |
| `tsc -p tsconfig.spec.json --noEmit` | 0 own errors — **first successful semantic pass** |
| `nx run sdk-angular:lint` | pass, 0 problems |
| `nx run sdk-angular:test` | pass — **21 suites, 234 tests** (unchanged before and after) |
| `nx run sdk-angular:build` | pass |
| `nx affected -t build,lint` (scoped to this change) | pass |

Test counts staying at exactly 21/234 is the guard that matters here: the edits changed which files the compiler sees, so an unchanged suite count proves nothing was silently dropped from the program.

## Commands

```bash
cd core-web
export NVM_DIR="$HOME/.nvm" && . "$NVM_DIR/nvm.sh" && nvm use   # 22.22.3
export NX_NO_CLOUD=true

corepack pnpm exec tsc -p libs/sdk/angular/tsconfig.lib.json  --noEmit
corepack pnpm exec tsc -p libs/sdk/angular/tsconfig.spec.json --noEmit
corepack pnpm exec nx run sdk-angular:lint
corepack pnpm exec nx run sdk-angular:test  --skip-nx-cache
corepack pnpm exec nx run sdk-angular:build --skip-nx-cache
```

> `pnpm` is not on `PATH` in this worktree — use `corepack pnpm`.

Scope `affected` with `--base=HEAD~1`. Against `origin/main` it sweeps the whole branch and six projects fail lint for pre-existing, unrelated reasons (`dotcms-js`, `utils`, `utils-testing`, `block-editor`, `dotcms-webcomponents`, `dotcms-block-editor` — legacy `no-explicit-any` / `no-console` / unused-var / `import/order` debt).

## Testing Strategy

No new tests. The 21 existing spec files already ran under ts-jest with diagnostics enabled; the gain here is that their tsconfig can now be checked as a whole program. Verification is compilation- and build-based, with `:test` guarding against files dropping out of the program.

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
| #35946 | `sdk-analytics` | partial | 18+29 err | ❌ | Fixed, unenforced |
| **#35947** | **`sdk-angular`** | ✅ | ✅ | ✅ | **Compliant; dead config removed** |

Taken with #35946, the SDK picture is now precise. `libs/sdk/*` projects built by **Rollup or ng-packagr** (`sdk-client`, `sdk-react`, `sdk-angular`) type-check during `build`, so their flags are enforced. The one built by **Vite** (`sdk-analytics`) does not — `vite-plugin-dts` emits declarations without failing on diagnostics. Build tooling, not directory, predicts enforcement.

## Follow-ups for the epic

1. **Sweep tsconfigs for dangling `files` entries before trusting any error count.** `sdk-angular`'s spec config had never completed a semantic pass, and nothing surfaced that — no build step reads it, and ts-jest only consumes its `compilerOptions`. Two of the fourteen projects triaged so far were masked this way (#35944 via `TS2688`, #35947 via `TS6053`); the remaining 30 should be checked before their counts are believed. `core-web/CLAUDE.md` now documents both variants.
2. **The `typecheck` gap raised in #35946 still stands** for Vite projects, and is unaffected by this issue.
