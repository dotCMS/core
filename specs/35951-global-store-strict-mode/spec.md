# Spec: Enable TypeScript strict mode in `global-store`

**Issue:** [#35951](https://github.com/dotCMS/core/issues/35951) — [18/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Implemented — one-line fix
**Conclusion:** Compliant, but it was leaking a `TS1205` into its consumers. Not enforced.

---

## Objective

Verify `global-store` (`core-web/libs/global-store`) against the rollout bar. Layer 3, **12 internal dependents**.

Stale ACs corrected first, per `core-web/CLAUDE.md`.

## Evidence

**All six flags already present** in `libs/global-store/tsconfig.json`, and both of its own configs were clean:

| Config | Errors |
|---|---:|
| `tsconfig.lib.json` | **0** |
| `tsconfig.spec.json` | **0** |

Neither was masked — no missing `files` entry, no uninstalled `types` entry.

> Worth noting where those zeros came from. Before #35948, `global-store` reported **36 errors**; every one was `data-access` leaking through its imports. Fixing `data-access` took it to 0 without anyone touching this project.

## The one real defect: a type re-exported as a value

`libs/global-store/src/index.ts:3` did:

```ts
export { WebSocketStatus } from '@dotcms/data-access';
```

`WebSocketStatus` is a **type** (`dot-events-socket.service.ts:9` — a string-literal union), so under `isolatedModules` this is `TS1205: Re-exporting a type when 'isolatedModules' is enabled requires using 'export type'`.

**This project's own configs never reported it.** `tsconfig.spec.json` includes only spec files and `test-setup.ts`, and no spec imports `./index`, so `index.ts` was never in its own program. The error only appeared from *consumers*: it was surfacing as the single spec error in `portlets-dot-analytics-data-access` (#35954), attributed there to a file in this library.

Fixed with `export type { WebSocketStatus }`. One line, and it cleared #35954 at the same time.

That is the useful generalisation: **a barrel file can carry an `isolatedModules` error that only its consumers ever see.** Same shape as `dot-ai.service.ts` in #35948.

## Results

| Gate | Result |
|---|---|
| `tsc -p tsconfig.lib.json --noEmit` | **0** |
| `tsc -p tsconfig.spec.json --noEmit` | **0** |
| `nx run global-store:lint` | pass |
| `nx run global-store:test` | pass — **187 tests** |
| `portlets-dot-analytics-data-access` spec config | 1 → **0** |

## Not enforced

No `build` target — only `lint` and `test`, and `:test` does not type-check (`isolatedModules: true` ⇒ ts-jest transpile-only, per #35948). With 12 dependents this one matters more than most; raised on the epic.

## Commands

```bash
cd core-web
export NVM_DIR="$HOME/.nvm" && . "$NVM_DIR/nvm.sh" && nvm use   # 22.22.3
export NX_NO_CLOUD=true

corepack pnpm exec tsc -p libs/global-store/tsconfig.lib.json  --noEmit   # 0
corepack pnpm exec tsc -p libs/global-store/tsconfig.spec.json --noEmit   # 0
corepack pnpm exec nx run-many -t lint,test -p global-store
# the consumer that was reporting the leak
corepack pnpm exec tsc -p libs/portlets/dot-analytics/data-access/tsconfig.spec.json --noEmit   # 0
```

> `pnpm` is not on `PATH` in this worktree — use `corepack pnpm`.

## Follow-up for the epic

**Check barrel files for `isolatedModules` re-export errors that only consumers see.** Two instances so far (`dot-ai.service.ts` in #35948, `global-store/src/index.ts` here), both invisible to the owning project's own configs. A cheap repo-wide grep for `export {` on type-only symbols would find the rest before they are misattributed to whichever consumer happens to trip over them.
