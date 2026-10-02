# Spec: Enable TypeScript strict mode in `sdk-client`

**Issue:** [#35942](https://github.com/dotCMS/core/issues/35942) — [09/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Awaiting review (Phase 1 — Specify)
**Conclusion:** **No code change required. The issue is already satisfied, and enforced.**

---

## Objective

Verify whether `sdk-client` (`core-web/libs/sdk/client`, package `@dotcms/client`) meets the rollout's strict-mode bar, and close the issue with evidence rather than manufacturing a diff.

It does. This is the **third** project in the rollout that was already compliant before the epic began — and the clearest of the three, because its build uses `compiler: 'tsc'` outright.

### Assumptions

1. "Strict" means the six flags from precedent #36879, in the project's own `tsconfig.json`. No `typescript-strict-plugin` / `tsc-strict` / `@ts-strict-ignore` — that approach was dropped.
2. "Done" requires three things, not one: flags present **and** zero errors **and** something in CI that actually verifies it. The third clause is what separated `sdk-types` / `sdk-uve` (done) from `dotcms-js` / `utils` (declared but unenforced).

---

## Evidence

### 1. Flags already present

`libs/sdk/client/tsconfig.json` carries all six (`strict`, `forceConsistentCasingInFileNames`, `noImplicitOverride`, `noPropertyAccessFromIndexSignature`, `noImplicitReturns`, `noFallthroughCasesInSwitch`).

This is almost certainly the **origin** of the pattern across the SDK: `git log --follow` on `libs/sdk/uve/tsconfig.json` showed it was created as a `C100` (100%-identical) copy of *this* file.

### 2. Compiles clean

| Config | Errors |
|---|---:|
| `tsconfig.lib.json` | **0** |
| `tsconfig.spec.json` | **0** |

### 3. Clean without escape hatches

Production source only (specs excluded), across **9600 lines** in 48 `.ts` files (15 of them specs):

| Pattern | Count |
|---|---:|
| `: any` / `<any>` / `any[]` | **0** |
| `@ts-ignore` / `@ts-expect-error` | **0** |
| Non-null assertions (`!.`) | **0** |

### 4. Enforced — and here it is unambiguous

`libs/sdk/client/rollup.config.cjs`:

```js
compiler: 'tsc',                     // ← not 'babel'
tsConfig: './tsconfig.lib.json'      // ← the config carrying the strict flags
```

`skipTypeCheck` is not set anywhere. Unlike `sdk-uve` — where `compiler: 'babel'` made this look ambiguous until the `@nx/rollup` source confirmed the TypeScript plugin is inserted unconditionally — here the build compiles with `tsc` directly against the strict config. A strict violation fails the build.

Targets, all green on a fresh no-cache run:

| Target | Result |
|---|---|
| `nx run sdk-client:build` | pass |
| `nx run sdk-client:lint` | pass |
| `nx run sdk-client:test` | pass |

### 5. That build runs in CI and gates every release

- `project.json` has `"tags": []` — no `skip:build` / `skip:lint` / `skip:test`.
- CI runs `nx run-many -t build --exclude=tag:skip:build` via the `build-test` execution in `core-web/pom.xml`, which has **no `<skip>` element** — `-DskipTests` and `-Pvalidate` cannot disable it.
- `@dotcms/client` is published to npm and matches the `sdk-*` glob in the SDK release pipeline (`cicd_release-sdk.yml` → `deploy-javascript-sdk`), so the same type-checked build gates every release.
  > The local `package.json` says `1.2.0`, but that is **not** what ships. The release action rewrites the version to the dotCMS release tag (ADR-0019 date lockstep); npm `latest` is **26.8.7-1** across 262 published versions. Do not quote the local version as the published one.

### 6. Consumers

6 dependents; **5 already strict**:

| Consumer | Strict? |
|---|---|
| `sdk-angular`, `sdk-react`, `sdk-vue`, `sdk-experiments`, `sdk-create-app` | **Yes** |
| `portlets-edit-ema-portlet` | Inherits `false` — and it imports from `@dotcms/client/internal` (`dot-page-api.service.ts:8`) |

Nothing is being widened, so there is no blast radius.

### 7. Path aliases all resolve

Unlike the dangling `@dotcms/uve/types` found in #35941, every alias here points at a real file:

| Alias | Target | Exists |
|---|---|:--:|
| `@dotcms/client` | `libs/sdk/client/src/index.ts` | ✅ |
| `@dotcms/client/internal` | `libs/sdk/client/src/internal.ts` | ✅ |
| `@dotcms/query-builder` | `libs/sdk/client/src/lib/client/content/builders/query/query.ts` | ✅ |

---

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
| **#35942** | **`sdk-client`** | ✅ | ✅ | ✅ | **No-op** |

Emerging pattern worth noting for the remaining 35: **every `libs/sdk/*` project is already strict and already enforced**, because they share a tsconfig lineage and all build through Nx executors that type-check. The genuinely unfinished work is concentrated in the non-SDK libraries and apps.

---

## Adjacent observation — committed artifact that CI never regenerates

The `build:js` target emits a file that is **committed to git**, but the target is not invoked by `core-web/pom.xml` or any workflow:

| Project | Committed artifact |
|---|---|
| `sdk-client` | `dotCMS/src/main/webapp/html/js/editor-js/sdk-editor.js` |
| `sdk-uve` | `dotCMS/src/main/webapp/ext/uve/dot-uve.js` |

If the source changes and nobody runs `build:js` by hand, the committed file silently drifts out of sync with it, and nothing in CI notices. Out of scope for the strict-mode rollout, but worth a ticket.

## Commands

```bash
cd core-web
pnpm exec tsc -p libs/sdk/client/tsconfig.lib.json --noEmit     # 0
pnpm exec tsc -p libs/sdk/client/tsconfig.spec.json --noEmit    # 0
pnpm exec nx run sdk-client:build --skip-nx-cache
pnpm exec nx run sdk-client:lint
pnpm exec nx run sdk-client:test
```

> Node 22.22.3 via nvm; `pnpm` is not on `PATH` in this worktree — use `corepack pnpm`.

## Testing Strategy

No new tests. `sdk-client` has 15 spec files and its `test` target runs in CI with no skip tag; ts-jest type-checks them against `tsconfig.spec.json`, which inherits the strict flags. Verification for this issue is compilation- and build-based.

## Boundaries

**Always:** back the "already done" verdict with reproducible commands in the issue comment.

**Ask first:** any change to `libs/sdk/client` source — published package, 5 strict consumers, and nothing needs changing.

**Never:** re-add flags that are already there, or make a cosmetic edit purely to produce a diff.

---

## Success Criteria

1. `tsc` clean on both `tsconfig.lib.json` and `tsconfig.spec.json`.
2. `nx run sdk-client:build` / `:lint` / `:test` pass on a no-cache run.
3. `git diff origin/main -- core-web/libs/sdk/client` stays **empty** — the deliverable is a verdict, not a diff.
4. #35942 closed with the evidence, its stale ACs corrected first — same treatment as #35939 and #35941.

## Steps

1. Correct the stale ACs on #35942 (`typescript-strict-plugin`, `npx tsc-strict`, `// @ts-strict-ignore`).
2. Comment with the evidence table and reproducible commands.
3. Close as completed — criteria met, just not by this rollout.
4. Commit this `spec.md` alongside those for #35939 and #35941.
5. Do **not** add `Closes #35942` to PR #36957 — there is no diff, and linking it would imply that PR resolved it.
