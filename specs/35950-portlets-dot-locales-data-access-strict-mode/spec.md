# Spec: Enable TypeScript strict mode in `portlets-dot-locales-data-access`

**Issue:** [#35950](https://github.com/dotCMS/core/issues/35950) — [17/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Verified — **no code change required**
**Conclusion:** Already compliant. Not enforced, because the project has no `build` target.

---

## Objective

Verify `portlets-dot-locales-data-access` (`core-web/libs/portlets/dot-locales/data-access`) against the rollout bar. Layer 3, **0 internal dependents**.

Stale ACs (`typescript-strict-plugin` / `npx tsc-strict` / `@ts-strict-ignore`) corrected first, per `core-web/CLAUDE.md`.

## Evidence

**All six flags already present** in `libs/portlets/dot-locales/data-access/tsconfig.json`.

| Config | Errors |
|---|---:|
| `tsconfig.lib.json` | **0** |
| `tsconfig.spec.json` | **0** |

Both numbers are trustworthy: no `files` entry points at a missing file and no `types` entry is uninstalled, so neither `TS6053` nor `TS2688` aborted the program before semantic checking (the two masking traps documented in `core-web/CLAUDE.md`, which had hidden #35944 and #35947).

`nx run …:lint` and `:test` both pass.

## Not enforced

`nx show project` lists **no `build` target** — only `lint` and `test`. That means:

- The project's own `tsconfig.json` is never read by any build, so its flags apply only when `tsc -p` is run by hand.
- `:test` does not type-check either: `tsconfig.spec.json` sets `isolatedModules: true`, which puts ts-jest in transpile-only mode (established in #35948).

So the flags are correct and currently satisfied, but nothing would catch a regression. This is the same category as `dotcms-js` (#35939), `utils` (#35940), `sdk-analytics` (#35946) and `data-access` (#35948) — raised on the epic as one decision rather than per project.

Unlike `data-access`, though, being unenforced cost nothing here: the count is genuinely 0, not 0-because-nobody-looked.

## Success criteria

1. `tsc -p` clean on both configs — **met**.
2. `lint` and `test` pass — **met**.
3. `git diff` for this project stays **empty** — the deliverable is a verdict, not a diff.

## Commands

```bash
cd core-web
export NVM_DIR="$HOME/.nvm" && . "$NVM_DIR/nvm.sh" && nvm use   # 22.22.3
export NX_NO_CLOUD=true

corepack pnpm exec tsc -p libs/portlets/dot-locales/data-access/tsconfig.lib.json  --noEmit   # 0
corepack pnpm exec tsc -p libs/portlets/dot-locales/data-access/tsconfig.spec.json --noEmit   # 0
corepack pnpm exec nx run-many -t lint,test -p portlets-dot-locales-data-access
```

> `pnpm` is not on `PATH` in this worktree — use `corepack pnpm`.
