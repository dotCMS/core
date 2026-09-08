# Spec: Enable TypeScript strict mode in `dotcms-js`

**Issue:** [#35939](https://github.com/dotCMS/core/issues/35939) — [06/44] · **Epic:** [#35932](https://github.com/dotCMS/core/issues/35932)
**Status:** Awaiting review (Phase 1 — Specify)

---

## Objective

Enable TypeScript `strict` mode for the Nx project `dotcms-js` (`core-web/libs/dotcms-js`) and resolve the **38 type errors** it surfaces, without introducing new `any` and without breaking any of its **20 dependent projects**.

**Who benefits:** the ~20 downstream projects — including the `dotcms-ui` admin app — that compile `dotcms-js` source directly through the `@dotcms/dotcms-js` path alias. Today its null-safety holes are invisible to them; after this change the types tell the truth.

**Why it matters here specifically:** `dotcms-js` is a layer-1 core library (auth, site, routing, config, HTTP response wrapping). Six of its consumers are *already* strict, so its loose types are actively leaking `any`-shaped uncertainty into projects that have opted into rigour.

### Assumptions (validated with the requester)

1. Follow precedent **#36879** (`dotcms-models`): the six flags go in the project's own `tsconfig.json`. `tsconfig.base.json` stays at `"strict": false` — never flipped globally.
2. No `typescript-strict-plugin`, no `tsc-strict` script, no `// @ts-strict-ignore`. That approach was dropped; the bootstrap #35933 closed without the plugin landing. Sub-issue ACs referencing them are stale.
3. **Scope is `tsconfig.lib.json` only.** `tsconfig.spec.json` fails today with `TS2688: Cannot find type definition file for 'jasmine'` — a pre-existing, non-strict-related breakage. Out of scope.
4. **The `skip:lint` / `skip:test` tags are not touched.** `nx run dotcms-js:lint` currently fails with **42 problems** (41 errors, mostly `no-explicit-any`). Re-enabling lint is a separate effort.
5. Legacy packaging debt (`ng-package.json`, `tslint.json`, peerDeps pinned to Angular `^6.0.0 || ^7.2.0`) is left as-is.

### Accepted trade-off: strict will be unenforced

**Decision made by the requester: enable strict only — no `typecheck` target, no CI gate.**

Recorded plainly so it is not rediscovered later: `dotcms-js` has **no `build` target**. Its only targets are `lint`, `test`, and `nx-release-publish`, and the first two are tag-excluded from CI. Consumers compile it via path mapping under *their own* tsconfig, so the flags added here are read by nothing in CI.

Consequence: after this work, `tsc -p libs/dotcms-js/tsconfig.lib.json --noEmit` will be clean, but **nothing prevents the next commit from regressing it**. The flags document intent; they do not enforce it.

Partial mitigation that comes for free: the six already-strict consumers (below) will surface *some* regressions in their own builds, because they compile this source strictly. That coverage is incidental and incomplete — it only catches errors on code paths those six actually import.

---

## Tech Stack

| | |
|---|---|
| Language | TypeScript 6.0.3 |
| Framework | Angular 21+ (this library is Angular services + models, `@Injectable`) |
| Monorepo | Nx 23, pnpm 10.17.1, Node 22.22.3 |
| Test runner | Karma + Jasmine (3 spec files; target is tag-excluded from CI) |

---

## Commands

```bash
cd core-web

# Measure the current error surface (the core loop for this work)
pnpm exec tsc -p libs/dotcms-js/tsconfig.lib.json --noEmit

# Before the flags land, simulate them on the CLI
pnpm exec tsc -p libs/dotcms-js/tsconfig.lib.json --noEmit \
  --strict --noImplicitOverride --noPropertyAccessFromIndexSignature \
  --noImplicitReturns --noFallthroughCasesInSwitch --forceConsistentCasingInFileNames

# Verify the six already-strict consumers still compile
pnpm exec nx run-many -t build,lint -p data-access,global-store,portlets-dot-analytics,portlets-dot-analytics-data-access,portlets-dot-locales-portlet,utils-testing

# Full blast-radius check across all 20 dependents
pnpm exec nx affected -t build,lint --base=origin/main --exclude=tag:skip:lint

# Formatting gate
pnpm exec nx format:check --base=origin/main
```

> Environment note: `pnpm` is not on `PATH` by default in this worktree. Use `corepack pnpm` with Node 22.22.3 from nvm (`.nvmrc`).

---

## Project Structure

```
core-web/libs/dotcms-js/
├── src/
│   ├── public_api.ts                    → Public barrel (what the 20 consumers import)
│   └── lib/core/
│       ├── login.service.ts             → 12 errors — largest cluster
│       ├── util/response-view.ts        →  6 errors — HTTP response wrapper
│       ├── string-utils.service.ts      →  5 errors
│       ├── routing.service.ts           →  4 errors
│       ├── dotcms-config.service.ts     →  3 errors
│       ├── site.service.ts              →  2 errors
│       ├── shared/user.model.ts         →  2 errors — PUBLIC MODEL, handle with care
│       ├── site.service.mock.ts         →  1 error
│       ├── logger.service.ts            →  1 error
│       ├── api-root.service.ts          →  1 error
│       └── util/http-request-utils.ts   →  1 error
├── tsconfig.json                        → WHERE THE SIX FLAGS GO
├── tsconfig.lib.json                    → extends tsconfig.json; the compilation unit in scope
└── tsconfig.spec.json                   → out of scope (pre-existing jasmine failure)
```

31 `.ts` files, 3 `.spec.ts`. Errors touch 11 files.

---

## Code Style

The six flags, added to `libs/dotcms-js/tsconfig.json` — identical to precedent #36879:

```json
{
    "extends": "../../tsconfig.base.json",
    "compilerOptions": {
        "target": "es2020",
        "module": "preserve",
        "moduleResolution": "bundler",
        "lib": ["dom", "dom.iterable", "es2022"],
        "forceConsistentCasingInFileNames": true,
        "strict": true,
        "noImplicitOverride": true,
        "noPropertyAccessFromIndexSignature": true,
        "noImplicitReturns": true,
        "noFallthroughCasesInSwitch": true
    }
}
```

Fix style — model reality, do not silence the compiler:

```ts
// GOOD — the value genuinely can be absent, so say so
getCookie(name: string): string | null {
    return this.readCookie(name);
}

// BAD — hides the hole the flag just exposed
getCookie(name: string): string {
    return this.readCookie(name) as string;
}

// GOOD — index-signature access (TS4111), purely mechanical
this.urls['current']

// GOOD — uninitialized field that is genuinely set later
private _auth: Auth | null = null;

// BAD — definite-assignment assertion papering over real absence
private _auth!: Auth;
```

**Never** add `any`, `@ts-ignore`, or `!` non-null assertions to clear an error. If a value is truly always present, prove it with initialization or a constructor assignment.

---

## Testing Strategy

**There is effectively no test safety net here, and the plan must not pretend otherwise.**

- 3 `.spec.ts` files exist, but the `test` target is tag-excluded (`skip:test`) and `tsconfig.spec.json` does not even compile (pre-existing jasmine types failure).
- Therefore verification is **compilation-based**, not test-based.

| Level | Mechanism | What it proves |
|---|---|---|
| Unit | — | Nothing. No usable suite. |
| Type | `tsc -p tsconfig.lib.json --noEmit` | The 38 errors are gone |
| Integration | `nx run-many -t build,lint` on the 6 strict consumers | Public-surface changes did not break rigorous consumers |
| System | `nx affected -t build,lint` over all 20 dependents | No regression anywhere downstream |

Writing new tests is **out of scope** — the suite cannot run without first fixing the jasmine types breakage.

---

## Boundaries

**Always:**
- Run the full `nx affected -t build,lint` before opening the PR — 20 projects depend on this library
- Prefer fixes that widen types honestly (`string | null`) over fixes that assert away the problem
- Keep each fix minimal and local to the error site

**Ask first:**
- Any change to `src/public_api.ts` (the public barrel)
- Any change to `shared/user.model.ts` — it is a public model consumed downstream; making `username`/`password` optional alters the shape 20 projects see
- Any change that requires editing a *consumer* project to compile
- Removing the `skip:lint` / `skip:test` tags

**Never:**
- Add `any`, `@ts-ignore`, or `!` non-null assertions to silence a flag
- Touch `core-web/tsconfig.base.json`
- Modify `tsconfig.spec.json` or attempt to fix the jasmine breakage in this PR
- Commit with any of the 20 dependents failing to build

---

## The 38 errors, grouped by fix strategy

| # | Group | Count | Files | Risk to consumers |
|---|---|---|---|---|
| A | `TS4111` index-signature dot access | 8 | `login.service.ts` (all) | **None** — internal, mechanical `.x` → `['x']` |
| B | `TS2564` uninitialized class property | 8 | `login.service.ts`, `routing.service.ts` ×2, `user.model.ts` ×2, `site.service.mock.ts`, `site.service.ts`, `response-view.ts` | **Medium** — `user.model.ts` is public |
| C | `TS2322`/`TS2345` null & undefined mismatches | 21 | `response-view.ts` ×5, `string-utils.service.ts` ×2, `login.service.ts` ×3, `dotcms-config.service.ts` ×3, `routing.service.ts` ×2, others | **High** — widens public return types |
| D | `TS7006` implicit any parameter | 3 | `string-utils.service.ts` | None — internal callback params |
| E | `TS18046` `unknown` in catch | 1 | `logger.service.ts:80` | None |

Group A is the safe warm-up. Group C is where the judgement lives.

---

## Success Criteria

1. `libs/dotcms-js/tsconfig.json` contains all six flags, byte-identical in spirit to #36879.
2. `pnpm exec tsc -p libs/dotcms-js/tsconfig.lib.json --noEmit` exits **0**.
3. **Zero** new `any`, `@ts-ignore`, `@ts-expect-error`, or `!` non-null assertions introduced. Verify with a diff grep.
4. All six already-strict consumers build and lint green: `data-access`, `global-store`, `portlets-dot-analytics`, `portlets-dot-analytics-data-access`, `portlets-dot-locales-portlet`, `utils-testing`.
5. `pnpm exec nx affected -t build,lint --base=origin/main --exclude=tag:skip:lint` exits **0** across all 20 dependents.
6. `pnpm exec nx format:check --base=origin/main` exits **0**.
7. `tsconfig.spec.json` is untouched, and its pre-existing jasmine failure is unchanged (not newly introduced, not fixed).
8. No change to `src/public_api.ts` without explicit approval.

---

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Widening a public return type to `\| null` breaks one of the 6 strict consumers | **High** | They are compiled explicitly in the verification loop, before `affected`. Fix forward in the same PR if small; escalate if it cascades. |
| `user.model.ts` shape change ripples across 20 projects | Medium | Listed under "Ask first". Prefer initializing (`username = ''`) over making optional, to preserve the shape. |
| Strict regresses silently after merge | **Certain, accepted** | Out of scope by decision. The six strict consumers give partial, incidental coverage. |
| A fix hides a real bug instead of surfacing it | Medium | The "never assert away" rule in Boundaries; review each Group C fix against actual runtime behaviour. |

---

## Resolved Decisions

1. **`user.model.ts` → initialize, do not make optional.** `username = ''` and `password = ''`. This preserves the public shape, so the 20 consumers see no type change. Making them optional would be more honest about runtime reality but is not worth the blast radius here.
2. **`response-view.ts` → do not grow this PR.** If widening its getters cascades into the six strict consumers beyond a trivial fix, stop, revert that file's changes, and open a follow-up issue. The PR ships the other groups rather than absorbing a cascade.
3. **Stale ACs on #35939 will be corrected on the issue**, same as was done for epic #35932 — removing the `typescript-strict-plugin`, `npx tsc-strict`, and `// @ts-strict-ignore` criteria that no longer apply.

## Open Questions

None outstanding. Ready for Phase 2 (Plan).
