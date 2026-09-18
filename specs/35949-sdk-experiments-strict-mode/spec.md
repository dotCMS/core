# Spec: Enable TypeScript strict mode in `sdk-experiments`

**Issue:** [#35949](https://github.com/dotCMS/core/issues/35949) — [16/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Implemented
**Conclusion:** **Flags-only change.** Zero errors surfaced, and the build is a real gate.

---

## Objective

Bring `sdk-experiments` (`core-web/libs/sdk/experiments`) to the rollout bar. Layer 2, **0 internal dependents**.

Stale ACs (`typescript-strict-plugin` / `npx tsc-strict` / `@ts-strict-ignore`) corrected first, per `core-web/CLAUDE.md`.

## Findings

`libs/sdk/experiments/tsconfig.json` carried **only `strict`** of the six flags.

The five missing flags were measured before being committed, by passing them on the CLI:

| Config | With `strict` alone | With all six |
|---|---:|---:|
| `tsconfig.lib.json` | 0 | **0** |
| `tsconfig.spec.json` | 0 | **0** |

So this is a pure flags change — nothing was hiding behind the missing five. No dangling `files` / `include` entries either, so neither config was masked (`TS6053` / `TS2688` both absent).

## Change

Added to `libs/sdk/experiments/tsconfig.json` alongside the existing `strict`:
`forceConsistentCasingInFileNames`, `noImplicitOverride`, `noPropertyAccessFromIndexSignature`, `noImplicitReturns`, `noFallthroughCasesInSwitch`.

Nothing else. `tsconfig.base.json` untouched.

## Enforced — proved by negative test

Despite the triage listing an inferred `typecheck` target, this project builds with **Rollup** (`rollup.config.cjs`), not Vite — so `@rollup/plugin-typescript` is in the chain, as with `sdk-react` and `sdk-client`.

Confirmed rather than assumed: a deliberate `const __strictProbe: number = "nope";` in `src/index.ts` **failed** the build, exit code 1:

```
[!] (plugin typescript) RollupError: [plugin typescript] src/index.ts (3:7):
    @rollup/plugin-typescript TS2322: Type 'string' is not assignable to type 'number'.
```

Probe reverted; file confirmed identical to git.

## Results

| Gate | Result |
|---|---|
| `tsc -p tsconfig.lib.json --noEmit` | **0** |
| `tsc -p tsconfig.spec.json --noEmit` | **0** |
| `nx run sdk-experiments:lint` | pass |
| `nx run sdk-experiments:test` | pass — **48 tests** |
| `nx run sdk-experiments:build` | pass |

## Commands

```bash
cd core-web
export NVM_DIR="$HOME/.nvm" && . "$NVM_DIR/nvm.sh" && nvm use   # 22.22.3
export NX_NO_CLOUD=true

corepack pnpm exec tsc -p libs/sdk/experiments/tsconfig.lib.json  --noEmit   # 0
corepack pnpm exec tsc -p libs/sdk/experiments/tsconfig.spec.json --noEmit   # 0
corepack pnpm exec nx run sdk-experiments:lint
corepack pnpm exec nx run sdk-experiments:test  --skip-nx-cache
corepack pnpm exec nx run sdk-experiments:build --skip-nx-cache
```

> `pnpm` is not on `PATH` in this worktree — use `corepack pnpm`.

## Testing Strategy

No new tests. The 48 existing ones pass. Note they do **not** type-check (`isolatedModules: true` puts ts-jest in transpile-only mode — see #35948), so `tsc -p` on both configs is the real verification, together with the Rollup build.

## Note for the rest of the rollout

Measuring the missing flags on the CLI **before** editing the tsconfig (`tsc -p <cfg> --noEmit --strict --noImplicitOverride ...`) tells you the cost up front and distinguishes a flags-only change like this one from a real migration. Worth doing first on every remaining project.
