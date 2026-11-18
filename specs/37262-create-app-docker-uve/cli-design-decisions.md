# CLI design decisions — open before implementation

**Feature**: `37262-create-app-docker-uve` · **Blocks**: US2, US3, US4 (not US1)
**Status**: **ALL RESOLVED** 2026-08-28 · **Raised**: 2026-08-28

The contracts in [contracts/cli-exit-contract.md](./contracts/cli-exit-contract.md) state what the
CLI must *guarantee*. They did not state *how*. D1–D3 were raised before implementation; D4–D8 came
out of answering them. **All are now decided** — see the summary table at the end.

The largest change is **D4**: the CLI now ships its **own** compose file instead of editing the
shared `single-node-demo-site` example. That removes the biggest risk in the whole plan.

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

**Recommendation: Option A everywhere. Option B is not needed at all.**

An earlier draft said "B for the UVE path", which was imprecise. The UVE site
(`src/index.ts:369-371`) does not need throwing or catching — X2 requires the run to **continue**,
so the `process.exit(1)` there is simply deleted and replaced with ordinary control flow:

```ts
if (!setUpUVE.ok) {
    run.uveConfigured = false;
    warnUveFailed(setUpUVE.val, run);   // terminal-403 vs other, per contract X3
} else {
    spinner.succeed('Configured the Universal Visual Editor');
}
// falls through to scaffolding either way
```

So the whole change is: one `process.on('exit')` handler, plus deleting one `process.exit(1)`.
No 13-site refactor.

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

**DECISION (Freddy, 2026-08-28): silent auto-reuse on CI only. Otherwise ask, and let the user
stop right there.**

```ts
const isCI = Boolean(process.env.CI) || !process.stdout.isTTY;

if (isCI) {
    console.log(chalk.yellow('⚠  dotCMS already running on 8082 — reusing it (non-interactive).'));
    reuse = true;                       // decide, never block a scripted run
} else {
    reuse = await askReuseOrAbort();     // { Reuse this instance | Abort }
}
```

Two points this settles:

- **The prompt must offer abort, not just reuse.** "Ask" means a real choice — a user who did not
  expect a dotCMS on 8082 needs to stop and look, not be pushed forward.
- **Even the CI path prints a notice.** Silent means "no prompt", not "no output": a scripted run
  that quietly attaches to an unknown instance is exactly the failure this is meant to avoid.

**Edge case folded in**: no TTY but no `CI` env var either (a piped local run). Treated as CI —
there is nobody to answer the prompt, so blocking is the worst option. The printed notice is what
makes it recoverable.

**Still required before reusing**: the instance must pass readiness **and** token issuance.
Something answering on 8082 is not necessarily a usable dotCMS, and adopting a stranger's instance
would wire the user's project to the wrong CMS.

---

## Summary

| # | Decision | Recommendation | Blocks |
|---|---|---|---|
| D1 | Emit mechanism for X1 | `process.on('exit')` backstop + throw for the UVE path | T027, T028, T029 |
| D2 | UVE poll budget | 60s, named constants | T040 |
| D3 | Reuse when non-interactive | `isTTY`; auto-reuse; verify readiness + token first | T047, T048 |

Once these are settled I will correct **X1** in the contract — it is currently a guarantee that
cannot be implemented as written — and record D2/D3 alongside X3 and X6.


---

## D4 — The CLI owns its compose file *(decided: bundle it in the package)*

**Problem**: the original plan edited `docker/docker-compose-examples/single-node-demo-site/docker-compose.yml`,
which is fetched from `main` at runtime and also used directly by README readers. Every hardening
step we wanted (gate on OpenSearch, publish 8090, healthchecks that `--wait` depends on) was a
behavior change shipped unversioned to consumers who never asked for it — and gating on OpenSearch
in particular introduced a way for dotCMS to **never start** if that probe later broke (e.g. an
`opensearch:1` → `:2` bump invalidating `admin:admin`).

**Decision**: give the CLI its own compose file, **bundled in the npm package**, and leave the
shared demo example untouched.

- File ships at `core-web/libs/sdk/create-app/assets/docker-compose.yml`.
- No runtime download — this removes `downloadFile`'s missing timeout, redirect handling and retry,
  and the unpinned `main` URL, in one move.
- **Installed CLIs (≤1.2.5) keep fetching the old shared file and are not repaired.** Accepted
  knowingly: this tool starts fresh local instances, is not a CI dependency, no known users have it
  in CI — and `npx @dotcms/create-app` resolves to the latest published version anyway, so only a
  warm npx cache stays behind.

**Consequence**: the "ships unversioned to every consumer" risk — previously the single largest in
this work — no longer applies. Nothing else reads the CLI's file.

### D4a — Keep it easy to swap back to remote

Reading the file is an interface, so bundled and remote are interchangeable:

```ts
export interface ComposeSource {
    readonly describe: string;              // shown in diagnostics
    read(): Promise<string>;
}

export const bundledCompose: ComposeSource = { /* fs.readFile of the shipped asset */ };
export const remoteCompose = (url: string): ComposeSource => ({ /* hardened fetch */ });

export function resolveComposeSource(): ComposeSource {
    const override = process.env.DOTCMS_COMPOSE_URL;
    return override ? remoteCompose(override) : bundledCompose;
}
```

The call site obtains **contents** and writes them, rather than downloading straight to disk as
today — that shape change is what makes the sources swappable. `DOTCMS_COMPOSE_URL` allows a
field hotfix with no code change or release. `updateDockerComposeStarterUrl` still rewrites the
file on disk afterwards, so `--starter` is unaffected.

**Packaging gotchas** (the asset will silently not ship otherwise): `package.json` `files` is
`["*.js", "README.md"]`, and `project.json`'s esbuild `assets` lists only README and package.json.
Both need the compose file added.

---

## D5 — How strict is the CLI's file? *(decided: strict, `start_period: 180s`)*

Now that nothing else consumes it, strictness costs nothing:

- `dotcms` `depends_on` gates on **both** `db` and `opensearch` at `condition: service_healthy`.
- OpenSearch probe: `curl -sk https://localhost:9200 -u admin:admin | grep -q cluster_name`
  — **verified on this stack, succeeds at 15s**; `curl` is present in the OpenSearch image.
- `dotcms` healthcheck on `http://127.0.0.1:8090/dotmgt/livez`, **`start_period: 180s`**
  (~4× the measured ~46s boot; above both precedents — lgtm 120s, metrics-monitoring 20s), plus
  `restart: unless-stopped`. Erring high is free: the first successful probe ends the window, so a
  46s boot leaves it at 46s regardless. Erring low is not: the container is marked `unhealthy` and
  `docker compose up --wait` **aborts** on an instance that would have been fine.
- **Rejected alternative — a credential-free OpenSearch probe** (accept `200` or `401` from the
  HTTP layer, dropping the `admin:admin` coupling). The coupling is this probe's only real
  exposure, but it is contained: the image tag is pinned to major `1`, so the `:1 → :2` bump that
  would invalidate the default credentials requires a deliberate edit to this very file by whoever
  then owns the probe. A proven probe beats an unproven one on the critical path.
- Management port published **loopback-only**: `127.0.0.1:8090:8090` (D-rationale in research R3).

---

## D6 — `.env` filename *(decided: always `.env`)*

The examples disagree with the CLI today: `nextjs`, `astro` and `nextjs-experiments` ship
`.env.local.example`; `angular-ssr` and `vuejs` ship `.env.example`; `angular` ships neither — while
the CLI tells everyone `touch .env`.

**Decision: always write `.env`**, regardless of framework. Functionally safe — Next.js and Astro
both read `.env` in addition to `.env.local` — and one filename is simpler to implement and explain.
Written **if absent**; if a file is already there it is left alone and the values are printed
instead.

---

## D7 — `--wait-timeout` and feedback *(decided: 600s, with continuous feedback)*

`--wait-timeout 600`. Pull time is spent before the container starts, so it does not consume this
budget; a timeout here means something is genuinely wrong.

**Conditional on continuous UI feedback for the whole wait** — ten minutes of frozen spinner is the
failure this issue was reported for. Requires both:

1. streaming `docker compose up --wait`'s own per-container `Waiting → Healthy` transitions, which
   `execa` currently swallows; and
2. a ticker showing elapsed time and per-service state, polled every ~2s.

This tightens AC-009 and contract X4: feedback must be continuous, not merely "pull progress
visible".

---

## D8 — Image tag *(decided: keep `latest` for now)*

The bundled file **could** pin `dotcms/dotcms:<CLI's matching release>` under ADR-0019, since the SDK
version is the dotCMS release version — which would make image/starter drift impossible.

**Deferred.** `latest` stays for now, so the drift the issue flagged (`latest` paired with a
hardcoded `starter-20260630`) **remains an open risk**, and ADR-0019 alignment is postponed rather
than resolved. Bundling makes this easy to revisit later.

---

## Summary of decisions

| # | Decision | Outcome |
|---|---|---|
| D1 | Emit mechanism for X1 | `process.on('exit')` handler prints state **and** writes `.env` with `writeFileSync`; the UVE `process.exit(1)` is simply deleted. No 13-site refactor. |
| D2 | UVE poll budget | Moot — a 403 is terminal. Single probe, retry `5xx` only. |
| D3 | Reuse when non-interactive | Silent auto-reuse on CI (or no TTY) **with a printed notice**; otherwise prompt offering **reuse or abort**. Reuse only an instance passing readiness + token issuance. |
| D4 | Compose file ownership | CLI ships its own, bundled; shared demo example untouched; `DOTCMS_COMPOSE_URL` swaps to remote. |
| D5 | Strictness | Gate on both services healthy; `start_period: 180s`; loopback 8090. Credential-free probe rejected — coupling contained by the major-tag pin. |
| D6 | `.env` filename | Always `.env`, write-if-absent. |
| D7 | `--wait-timeout` | 600s, conditional on continuous feedback. |
| D8 | Image tag | `latest` for now; drift risk and ADR-0019 alignment stay open. |

**Out of scope, confirmed (T066)**: #37268 (interrupted boot bricks the instance), image-tag
pinning / ADR-0019 alignment, and #35096 (E2E suite incl. fault injection).
