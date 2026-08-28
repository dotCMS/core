# CLI design decisions — open before implementation

**Feature**: `37262-create-app-docker-uve` · **Blocks**: US2, US3, US4 (not US1)
**Status**: awaiting decision · **Raised**: 2026-08-28

The contracts in [contracts/cli-exit-contract.md](./contracts/cli-exit-contract.md) state what the
CLI must *guarantee*. They do not state *how*, and three of those guarantees turn out to need a
real decision before code is written. One of them (D1) is specified **incorrectly** today.

None of this affects **US1 (compose)**, which is fully designed, Red-confirmed, and independent.

---

## D1 — How is "no successful state is ever discarded" actually implemented?

**Contract X1 today says**: *"Implemented as an emit step that runs from a `finally`-equivalent
position, not from the happy path."*

**That is wrong.** `finally` does not run when `process.exit()` is called. Verified:

```js
process.on('exit', (c) => console.log(`[process.on(exit)] RAN (code=${c})`));
try { process.exit(3); } finally { console.log('[finally] RAN'); }
```
```
  in try
  [process.on(exit)] RAN (code=3)      ← only this
  exit code: 3
```

And there are **17 `process.exit(1)` call sites** — `src/asks.ts:221`, and sixteen in
`src/index.ts`, of which **13 sit inside the single `try` opened at `index.ts:93`**. So the current
wording cannot be implemented against the current control flow.

### Option A — `process.on('exit')` backstop *(recommended)*

Register one handler that prints `host`/`token`/`siteId` and writes `.env` from `RunState`.

- **Cost**: one new call site. No existing `process.exit()` has to move.
- **Strongest property**: it also catches exits nobody anticipated — including future ones. The
  guarantee stops depending on remembering to route every error path correctly.
- **Constraint**: the handler is synchronous. `.env` must use `writeFileSync`, and it cannot
  prompt or await.
- **Risk checked**: stdout truncation on exit. Tested at 200 lines to both a pipe and a file —
  **200/200 survived**. The recoverable-state block is ~5 lines, well inside that.

### Option B — convert inner exits to throws

Replace the 13 in-`try` `process.exit(1)` calls with `throw new CliError(…)`, caught by the
existing top-level `catch` at `index.ts:388`, which emits and then exits.

- **Cost**: 13 call sites, touching every error path in the file.
- **Better**: explicit, ordinary control flow; trivially unit-testable; permits async work
  (prompts, awaited writes) on the way out.
- **Worse**: a large diff in exactly the code the user is trusting after a bad experience, and a
  future `process.exit()` added anywhere silently re-opens the hole Option A closes structurally.

**Recommendation: A for the guarantee, B only for the UVE path.** X2 needs the UVE failure to
*continue* to scaffolding, not exit — so that one site becomes ordinary flow regardless. A gives a
structural guarantee for everything else at a fraction of the blast radius.

**Open question for you**: accept the synchronous-handler constraint (`writeFileSync`, no prompt on
the way out)?

---

## D2 — What is the UVE poll budget? — **RESOLVED BY MEASUREMENT: the premise was wrong**

**Original question**: X3 says "poll `GET` until 200"; for how long?

**Answer: it does not matter, because the 403 never clears.** Measured 2026-08-28 on an
M5 / 64GB MacBook Pro, dotCMS constrained to 2 CPUs / 4G.

### Measurement 1 — clean boot: there is no settling window

| Signal | First success |
|---|---|
| `POST /api/v1/authentication/api-token` | **46s** |
| `GET /api/v1/apps/dotema-config-v2/{site}` | **46s** |
| `/dotmgt/livez`, `/dotmgt/readyz` | 48s |
| `/api/v1/appconfiguration` (current CLI probe) | 49s |

The UVE endpoint was usable **2 seconds before `readyz` went green**. Server logs show why:
the starter import (T+20s) and the ES reindex (T+44s) both complete *inside* Tomcat startup
(`Server startup in [36517] milliseconds`), and the HTTP connector does not accept traffic until
after them. On a clean boot **nothing answers while the instance is still settling** — so the
race described in the spec's original root cause 2 cannot occur.

> Hardware caveat: 46s is best-case. A 4-core laptop with a cold image cache will be
> substantially slower, and the ~1.5GB pull is additional. The *ordering* above is structural
> and should hold regardless; the absolute numbers should not be quoted as typical.

### Measurement 2 — interrupted boot: the 403 reproduces and is PERMANENT

Reproducing the reporter's actual path — dotCMS killed 25s in (mid `com.dotmarketing.beans.Tree`
import), then hand-started, as in reproduction step 4:

```
T+39s  appconfiguration 200 — CLI proceeds
T+41s  api-token   -> 200
T+41s  defaultSite -> 200
T+41s  UVE GET -> 403   UVE POST -> 403
   ... 193 consecutive attempts over ~7 minutes, zero successes ...
T+440s UVE GET -> 403   UVE POST -> 403
```

Server-side cause:

```
DotSecurityException: User 'Admin User [ID: dotcms.org.1][email:admin@dotcms.com]'
  does not have READ permissions on Site 'demo.dotcms.com'
```

The interrupted import left the site's permission rows unwritten. The restart re-ran
`Task00004LoadStarter` and Tomcat started cleanly, but the permissions never appeared. **The
instance does not recover.** Only `docker compose down -v` and a fresh start fixes it.

### Consequences

1. **The read-before-write gate (X3 / US3) is the wrong fix.** Polling `GET` until 200 would poll
   forever against a condition that never clears.
2. **A poll budget should be SHORT, not long.** Any budget merely adds silence before the same
   warning. ~15-30s is generous.
3. **The warning text in X2 must change.** "Configure UVE manually at this URL" is useless advice
   here — manual configuration fails identically. The correct message is that the instance is in a
   broken state from an interrupted first boot and must be recreated with `docker compose down -v`.
4. **Fixing root cause 1 removes root cause 2.** No crash -> no interrupted import -> no 403.
   US1 (compose) is the actual fix; the CLI work is damage limitation for when it happens anyway.
5. **The spec's hypothesised mechanism was wrong** — not license gating, not a transient race, and
   not `user.isAdmin()` swallowing an exception via `Try.of(...).getOrElse(false)`. It is missing
   permission data. The P2 backend non-goal should be re-pointed accordingly.

**Recommendation**: replace the poll-until-200 gate with a **single** `GET` probe. On 403, skip the
write and emit the "recreate your instance" guidance. Keep retry only for `5xx`, which is a genuine
transient class.

**Limits of this evidence**: one host, one starter, one image; kill point fixed at 25s. Which
kill-points corrupt and which do not is unmapped, and *why* a re-run import leaves permissions
missing is a backend question deserving its own issue.

## D3 — What does "offer to reuse" do when there is no terminal?

**Contract X6 says**: *"the CLI offers to reuse that instance instead of exiting."* "Offer" implies
an `inquirer` prompt — and prompts are how this CLI asks everything (`src/asks.ts` uses
`inquirer.prompt` in five places).

**The problem**: there is **no** `--yes`, `--ci`, or non-interactive flag in the option list
(`src/index.ts:71-88`). A prompt in a scripted or CI run has nothing to read from and hangs — which
is a worse failure than the "Required ports are already in use" error we are replacing.

| Option | Behavior |
|---|---|
| **`process.stdout.isTTY` check** *(recommended)* | Prompt when interactive; auto-reuse with a printed notice when not. No new API surface. |
| Add `--yes` / `--reuse` flag | Explicit and scriptable, but new public CLI surface that must then be documented and supported. |
| Always auto-reuse, never prompt | Simplest, but silently attaches to an instance the user may not have meant to use. |

A second, smaller question rides along: **how much do we verify before reusing?** Something is
answering on 8082 — but is it a *suitable* dotCMS? Minimum bar should be that the readiness probe
and token issuance both succeed; otherwise treat the port as busy-and-unusable and fail as today.

**Recommendation: `isTTY`, auto-reuse when non-interactive, and only reuse an instance that passes
readiness + token issuance.**

**Open question for you**: is auto-reuse-when-piped the right default, or should a non-interactive
run fail loudly rather than silently adopt a stranger's instance?

---

## Summary

| # | Decision | Recommendation | Blocks |
|---|---|---|---|
| D1 | Emit mechanism for X1 | `process.on('exit')` backstop + throw for the UVE path | T027, T028, T029 |
| D2 | UVE poll budget | 60s, named constants | T040 |
| D3 | Reuse when non-interactive | `isTTY`; auto-reuse; verify readiness + token first | T047, T048 |

Once these are settled I will correct **X1** in the contract — it is currently a guarantee that
cannot be implemented as written — and record D2/D3 alongside X3 and X6.
