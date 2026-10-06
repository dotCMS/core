# Spec: Enable TypeScript strict mode in `portlets-dot-analytics-data-access`

**Issue:** [#35954](https://github.com/dotCMS/core/issues/35954) — [21/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Verified — **no change needed in this project**
**Conclusion:** Already compliant. Its one reported error belonged to `global-store` and was fixed there (#35951).

---

## Objective

Verify `portlets-dot-analytics-data-access` (`core-web/libs/portlets/dot-analytics/data-access`) against the rollout bar. Layer 3, **1 internal dependent**.

Stale ACs corrected first, per `core-web/CLAUDE.md`.

## Evidence

**All six flags already present** in the project's own `tsconfig.json`. Not masked — no missing `files` entry, no uninstalled `types` entry.

| Config | Before | After |
|---|---:|---:|
| `tsconfig.lib.json` | **0** | **0** |
| `tsconfig.spec.json` | 1 | **0** |

## The one error was not this project's

`tsc -p tsconfig.spec.json` reported exactly one error, and it pointed at another library:

```
libs/global-store/src/index.ts(3,10): error TS1205: Re-exporting a type when
'isolatedModules' is enabled requires using 'export type'.
```

`global-store`'s barrel did `export { WebSocketStatus }` on what is actually a type. `global-store`'s **own** configs never reported it — no spec there imports `./index`, so the file was never in its own program. It only became visible from a consumer that pulls the barrel in under a config with `isolatedModules: true`, which is this one.

Fixed in `global-store` as part of **#35951**, which took this project to 0 with no edit here.

> The generalisable point: **an error attributed to your project may live in a dependency's barrel file.** Read the path before assuming ownership. Two instances of this shape so far — `dot-ai.service.ts` (#35948) and `global-store/src/index.ts` (here).

## Results

| Gate | Result |
|---|---|
| `tsc -p tsconfig.lib.json --noEmit` | **0** |
| `tsc -p tsconfig.spec.json --noEmit` | 1 → **0** |
| `nx run …:lint` | pass |
| `nx run …:test` | pass — **217 tests** |

## Not enforced

No `build` target — only `lint` and `test`, and `:test` does not type-check (`isolatedModules: true` ⇒ ts-jest transpile-only, per #35948). Same category as #35939 / #35940 / #35946 / #35948; raised on the epic.

## Commands

```bash
cd core-web
export NVM_DIR="$HOME/.nvm" && . "$NVM_DIR/nvm.sh" && nvm use   # 22.22.3
export NX_NO_CLOUD=true

R=libs/portlets/dot-analytics/data-access
corepack pnpm exec tsc -p $R/tsconfig.lib.json  --noEmit   # 0
corepack pnpm exec tsc -p $R/tsconfig.spec.json --noEmit   # 0
corepack pnpm exec nx run-many -t lint,test -p portlets-dot-analytics-data-access
```

> `pnpm` is not on `PATH` in this worktree — use `corepack pnpm`.
