# Spec: Enable TypeScript strict mode in `portlets-dot-experiments-data-access`

**Issue:** [#35952](https://github.com/dotCMS/core/issues/35952) — [19/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Verified — **no code change required**
**Conclusion:** Already compliant. Not enforced, because the project has no `build` target.

---

## Objective

Verify `portlets-dot-experiments-data-access` (`core-web/libs/portlets/dot-experiments/data-access`) against the rollout bar. Layer 3, **2 internal dependents**.

Stale ACs corrected first, per `core-web/CLAUDE.md`.

## Evidence

**All six flags already present** in the project's own `tsconfig.json`.

| Config | Errors |
|---|---:|
| `tsconfig.lib.json` | **0** |
| `tsconfig.spec.json` | **0** |

Neither config was masked — no `files` entry pointing at a missing file, no uninstalled `types` entry, so neither `TS6053` nor `TS2688` aborted the program before semantic checking.

`lint` and `test` both pass.

> As with `global-store`, these zeros are recent. This project reported **36 errors** before #35948, all of them `data-access` leaking through imports. Fixing that library took it to 0 with no change here.

## Not enforced

No `build` target — only `lint` and `test`. The project's own tsconfig is therefore never read by a build, and `:test` does not type-check either (`isolatedModules: true` ⇒ ts-jest transpile-only, established in #35948). Same category as #35939 / #35940 / #35946 / #35948; raised on the epic as a single decision.

The flags are satisfied today, but nothing would catch a regression.

## Success criteria

1. `tsc -p` clean on both configs — **met**.
2. `lint` and `test` pass — **met**.
3. `git diff` for this project stays **empty** — the deliverable is a verdict, not a diff.

## Commands

```bash
cd core-web
export NVM_DIR="$HOME/.nvm" && . "$NVM_DIR/nvm.sh" && nvm use   # 22.22.3
export NX_NO_CLOUD=true

R=libs/portlets/dot-experiments/data-access
corepack pnpm exec tsc -p $R/tsconfig.lib.json  --noEmit   # 0
corepack pnpm exec tsc -p $R/tsconfig.spec.json --noEmit   # 0
corepack pnpm exec nx run-many -t lint,test -p portlets-dot-experiments-data-access
```

> `pnpm` is not on `PATH` in this worktree — use `corepack pnpm`.
