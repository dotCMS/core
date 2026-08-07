# Spec: Enable TypeScript strict mode in `sdk-uve`

**Issue:** [#35941](https://github.com/dotCMS/core/issues/35941) — [08/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Awaiting review (Phase 1 — Specify)
**Conclusion:** **No code change required. The issue is already satisfied.**

---

## Objective

Verify whether `sdk-uve` (`core-web/libs/sdk/uve`, package `@dotcms/uve`) meets the strict-mode bar defined by the rollout, and close the issue with evidence rather than producing a change for its own sake.

**Result of the investigation: it already does — and unlike some earlier projects in this rollout, it is genuinely enforced.**

### Assumptions (stated for correction)

1. The rollout's definition of "strict" is the six flags from precedent #36879, in the project's own `tsconfig.json`. No `typescript-strict-plugin`, no `tsc-strict`, no `// @ts-strict-ignore` — that approach was dropped.
2. "Done" means: flags present **and** zero errors **and** something in CI actually verifies it. The third clause is the one that separated `sdk-types` (done) from `dotcms-js` / `utils` (declared but unenforced).
3. The 2 commits by which this branch trails `origin/main` are irrelevant to the conclusion, but the branch should be updated before any work.

---

## Evidence

### 1. The flags are already there

`libs/sdk/uve/tsconfig.json` carries all six:

```json
"forceConsistentCasingInFileNames": true,
"strict": true,
"noImplicitOverride": true,
"noPropertyAccessFromIndexSignature": true,
"noImplicitReturns": true,
"noFallthroughCasesInSwitch": true
```

Present since `277cbbc8f7` — *"chore(SDK): Create `getUVEState` to manage UVE state headlessly (#31242)"* — i.e. from early in the library's life, not added by this rollout.

### 2. It compiles clean

| Config | Errors |
|---|---:|
| `tsconfig.lib.json` | **0** |
| `tsconfig.spec.json` | **0** |

### 3. The code is genuinely clean, not clean-by-escape-hatch

Across **4518 lines** in 21 `.ts` files (5 of them specs):

| Pattern | Count |
|---|---:|
| `: any` / `<any>` / `any[]` | **0** |
| `@ts-ignore` / `@ts-expect-error` | **0** |
| Non-null assertions (`!.`) | **0** |

So the zero-error result is not propped up by suppressions.

### 4. It is enforced — verified at the source, not assumed

This is the clause that failed for `dotcms-js` and `utils`, so it was checked directly rather than inferred.

`libs/sdk/uve/rollup.config.cjs` sets `compiler: 'babel'`, which earlier in this rollout was **wrongly** read as "no type checking". `compiler` governs only the transpile step. `@nx/rollup`'s `withNx` **always** inserts a TypeScript plugin — see `@nx/rollup/src/plugins/with-nx/with-nx.js:164-196`:

```js
options.useLegacyTypescriptPlugin !== false
    ? require('rollup-plugin-typescript2')({
          check: !options.skipTypeCheck,      // ← type checking
          tsconfig: tsConfigPath, ... })
    : require('@rollup/plugin-typescript')({
          ...,
          noEmitOnError: !options.skipTypeCheck })   // ← fails the build
```

`sdk-uve` sets neither `skipTypeCheck` nor `useLegacyTypescriptPlugin`, so it gets `rollup-plugin-typescript2` with `check: true` against `tsconfig.lib.json` — the config that carries the strict flags. A strict violation fails the build.

The same mechanism was **proven empirically** on the sibling project `sdk-types` in this PR: reverting a fix there made `nx run sdk-types:build` fail with `@rollup/plugin-typescript TS2564`.

### 5. That build runs in CI, and in the release pipeline

- `project.json` has `"tags": []` — **no `skip:build` / `skip:lint` / `skip:test`.**
- CI runs `nx run-many -t build --exclude=tag:skip:build` (`build-test` in `core-web/pom.xml`), so `sdk-uve` is built on every PR.
- `@dotcms/uve` is a **published npm package** (v1.1.1) and matches the `sdk-*` glob in the SDK release pipeline, so the same type-checked build gates every release.

There are in fact **three** type-checking paths, two of which run in CI:

| Path | Type-checks? | In CI? |
|---|---|---|
| `build` (rollup) | Yes — TS plugin with `noEmitOnError: !skipTypeCheck` | **Yes**, and the `build-test` execution in `core-web/pom.xml:191` has **no `<skip>` element** — it cannot be turned off |
| `test` (ts-jest) | Yes — `diagnostics` is not disabled in `jest.preset.js` nor the project config, so it defaults on, against `tsconfig.spec.json` | **Yes**, via `nx affected -t test` (no skip tag) |
| `build:js` (esbuild) | Yes — `compiler: "tsc"`, `skipTypeCheck` unset → runs `tsc --noEmit` over `tsconfig.lib.json` | No — the target name does not match `nx run-many -t build`, and its output is committed by hand |

`build-test` being unskippable is what makes this the strongest-enforced project of the rollout so far: `-DskipTests=true` disables `unit-test` and `-Pvalidate` gates `lint-test`, but neither can disable the build.

### 6. Consumers

7 dependents; **5 already strict**:

| Consumer | Strict? |
|---|---|
| `sdk-analytics`, `sdk-angular`, `sdk-experiments`, `sdk-react`, `sdk-vue` | **Yes** |
| `dotcms-ui`, `portlets-edit-ema-portlet` | Inherits `false` |

Nothing to widen, so no blast radius to manage.

---

## Comparison with the rest of the rollout

| Issue | Project | Flags | Clean | Enforced | Outcome |
|---|---|:--:|:--:|:--:|---|
| #35935 | `sdk-types` | ✅ | ✅ | ✅ | No-op; shipped docs |
| #35936 | `dotcms` | ❌ | ❌ | ❌ | Dead code → #36950 |
| #35937 | `dot-layout-grid` | ❌ | ❌ | ❌ | Dead code → #36950 |
| #35938 | `sdk-create-app` | ❌ | 2 errors | ✅ | Fixed |
| #35939 | `dotcms-js` | ❌ | 38 errors | ❌ | Fixed, unenforced |
| #35940 | `utils` | ❌ | 32+17 errors | ❌ | Fixed, unenforced |
| **#35941** | **`sdk-uve`** | ✅ | ✅ | ✅ | **No-op** |

`sdk-uve` is the second project in the rollout that was already finished before the epic started.

---

## Commands

```bash
cd core-web

pnpm exec tsc -p libs/sdk/uve/tsconfig.lib.json --noEmit     # expect 0
pnpm exec tsc -p libs/sdk/uve/tsconfig.spec.json --noEmit    # expect 0
pnpm exec nx run sdk-uve:build --skip-nx-cache               # expect pass
pnpm exec nx run sdk-uve:lint
pnpm exec nx run sdk-uve:test
```

> Node 22.22.3 via nvm; `pnpm` is not on `PATH` in this worktree, use `corepack pnpm`.

## Project Structure

```
core-web/libs/sdk/uve/
├── src/                      21 .ts files, 4518 lines (5 specs)
│   ├── index.ts              public barrel      → @dotcms/uve
│   ├── internal.ts           internal barrel    → @dotcms/uve/internal
│   ├── types.ts                                 → @dotcms/uve/types
│   ├── lib/core/, lib/editor/
│   └── script/sdk-editor.ts  entry for build:js → dotCMS webapp
├── tsconfig.json             ← the six flags already live here
├── tsconfig.lib.json         declaration: true  ← what makes rollup type-check
└── rollup.config.cjs         compiler: 'babel'  (transpile only; TS plugin is separate)
```

## Code Style

Not applicable — no code is being written. If a future change touches this project, the existing bar is: no `any`, no `@ts-ignore`, no `!` assertions, all currently at zero.

## Testing Strategy

No new tests. Existing `sdk-uve:test` and `sdk-uve:lint` targets run in CI (no skip tags) and must stay green. Verification for this issue is compilation- and build-based, per the commands above.

## Boundaries

**Always:** back the "already done" claim with reproducible commands in the issue comment.

**Ask first:** any change to `libs/sdk/uve` source — it is a published package with 5 strict consumers, and nothing here needs changing.

**Never:** add flags that are already present, or make a cosmetic change purely to have a diff for the issue.

---

## Success Criteria

1. `tsc -p libs/sdk/uve/tsconfig.lib.json --noEmit` → 0 errors.
2. `tsc -p libs/sdk/uve/tsconfig.spec.json --noEmit` → 0 errors.
3. `nx run sdk-uve:build` passes, and the rollup TS-plugin evidence above is recorded.
4. `git diff origin/main -- core-web/libs/sdk/uve` stays **empty** — the deliverable is a verdict, not a diff.
5. #35941 is closed with the evidence, and its stale ACs (`typescript-strict-plugin`, `npx tsc-strict`, `// @ts-strict-ignore`) corrected first — same treatment as #35939.

## Resolved

**How to record the closure:** close #35941 directly with the evidence, rather than adding `Closes #35941` to PR #36957. There is no diff to attach, and linking it would imply this PR resolved it — it did not; the project has been compliant since February 2025.

## Incidental finding — out of scope

`core-web/tsconfig.base.json:104` maps `@dotcms/uve/types` → `libs/sdk/uve/src/types.ts`, **a file that does not exist**. Nothing imports that specifier, and it is absent from the `exports` / `typesVersions` maps in `libs/sdk/uve/package.json`. A dead alias, unrelated to strict mode. Not touched here — worth a separate cleanup ticket.

## Steps

1. Correct the stale ACs on #35941 (`typescript-strict-plugin`, `npx tsc-strict`, `// @ts-strict-ignore`), same treatment as #35939.
2. Comment on #35941 with the evidence table and the reproducible commands.
3. Close it as completed — the acceptance criteria are met, just not by this rollout.
4. Commit `spec.md` alongside the one for #35939 already in PR #36957, so the "already compliant" verdict is recorded for the remaining 36 issues.
