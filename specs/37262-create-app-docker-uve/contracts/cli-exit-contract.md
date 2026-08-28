# Contract: `@dotcms/create-app` exit behavior

**Consumer**: the person running `npx @dotcms/create-app`, and any script wrapping it.

The reported failure is a contract violation: the CLI held a working token and site ID and exited
without printing either. These are the guarantees the fix establishes.

---

## X1 — No successful state is ever discarded

> Once `token` and `siteId` are non-null, **every** terminal path — success, handled failure, or
> unexpected throw — emits `host`, `token`, `siteId`, and writes `.env`.

This is the single most important guarantee here: it converts every future unanticipated failure
from total loss into a recoverable one.

**Mechanism (D1, decided)**: a single `process.on('exit')` handler that prints `host`/`token`/
`siteId` and writes `.env` with `writeFileSync`. It must be synchronous — no `await`, no prompting.

An earlier note here said "from a `finally`-equivalent position"; that was wrong and is withdrawn.
`finally` does **not** run on `process.exit()`, and there are 17 such call sites (13 inside the
single `try` at `src/index.ts:93`). The exit hook covers all of them, including paths nobody has
written yet — which is the point. Verified: stdout survives the handler at 200 lines to both a pipe
and a file; the state block is ~5 lines.

## X2 — Optional steps are non-fatal

UVE configuration is **optional**. Its failure MUST:

1. warn — never `process.exit`;
2. print the [headless UVE guide](https://dev.dotcms.com/docs/author/pages-and-visual-editing/universal-visual-editor/uve-headless-config)
   with this run's `host`, `siteId`, and app key `dotema-config-v2`;
3. continue to scaffolding;
4. exit **0** with a complete project.

**Exit-code change**: a run that previously exited `1` now exits `0` with a warning. Deliberate, and
called out in the spec's Regression Risk — any wrapper asserting the old behavior will see it.

## X3 — Probe once before writing; never retry a 403

The UVE `POST` is attempted only after a `GET` of the same resource returns 200.

- The `GET` is a **single probe**, not a poll.
- `POST` retries on `5xx` only — the one genuinely transient class.
- `POST` does **not** retry on `403`, `401`, or any other `4xx`.

**Why no retry on 403** (this reverses the original contract). Measurement during planning showed a
403 here is not transient and never clears: after an interrupted starter import the site's
permission rows are missing, and the endpoint returned 403 on **193 consecutive attempts over ~7
minutes**, with zero successes. Polling would spin forever. Against a cleanly-booted instance the
same call returns 200 within ~46s of `docker compose up`, before `/dotmgt/readyz` is even green —
so there is no settling window to wait out.

**On 403 the CLI MUST NOT offer manual UVE setup steps.** Manual configuration fails identically,
for the same missing permissions. It must instead report that the instance is unrecoverable from an
interrupted first boot and must be recreated:

```
docker compose down -v && docker compose up -d --wait
```

Root cause and evidence: see spec.md "Cause 2", and #37268 for the backend defect.

## X4 — Progress is truthful

- "Containers started successfully" is printed only when containers are actually running and
  healthy — via `docker compose up -d --wait --wait-timeout 600` (D7).
- **Feedback MUST be continuous for the entire wait** — up to ten minutes. Ten minutes of frozen
  spinner is the failure this issue was reported for, so "pull progress is visible" is not enough.
  Two sources, both required:
  1. stream `docker compose up --wait`'s own per-container `Waiting → Healthy` transitions, which
     `execa` currently swallows; and
  2. a ticker showing elapsed time and per-service state, refreshed every ~2s.
- A `--wait` timeout is reported with diagnostics, not left as a silent block (research R4: a wrong
  probe hangs `--wait`; it does not cause a restart loop).
- Image-pull progress is streamed; no silent multi-minute spinner.
- Retry messages never interleave with an active `ora` spinner — `fetchWithRetry` takes a
  caller-supplied reporter instead of calling `console.log` directly.

## X5 — Readiness probe

Primary: `GET http://127.0.0.1:8090/dotmgt/readyz` (published by contract C4).
Fallback: `GET /api/v1/appconfiguration` on 8082, for stacks whose compose predates C4.

A response is "reachable" on any 2xx — `fetchWithRetry` already accepts 2xx, so the caller MUST NOT
re-narrow to `=== 200` (the current mismatch at `src/index.ts:507`).

## X6 — Re-runs are possible

A busy port is not by itself fatal. If 8082 is busy **and** answers as a healthy dotCMS, the CLI
reuses that instance instead of exiting. Only a busy port that is *not* dotCMS is an error.

**Interactivity (D3, decided)**:

| Context | Behavior |
|---|---|
| TTY | **Prompt**, offering *reuse* or *abort*. Someone who did not expect a dotCMS on 8082 needs to stop and look, not be pushed forward. |
| `CI` env var set, or no TTY | **Auto-reuse, but print a notice.** "Silent" means no prompt, not no output — a scripted run quietly attaching to an unknown instance is the failure this guards against. A piped local run has nobody to answer, so blocking is the worst option. |

Reuse requires the instance to pass **readiness *and* token issuance**. Something answering on 8082
is not necessarily a usable dotCMS, and adopting a stranger's instance would wire the project to the
wrong CMS.

Corollary: the CLI must not offer to empty a directory whose `docker-compose.yml` is the only way to
tear down the instance it is about to reuse.

## X7 — Failures are reported as failures

`installDependenciesForProject` returns `Result<boolean, string>`. `Err()` is `{ok: false, val}` —
**truthy** — so `if (!result)` at `src/index.ts:597` never fires and a failed `npm install` reports
success. Callers MUST branch on `result.ok`.

Fixing this makes a previously-unreachable branch reachable: runs with a broken npm that silently
"succeeded" will now correctly fail. Intended, and a visible behavior change.

## X8 — Filesystem side effects are unwound

`moveDockerComposeOneLevelUp` / `moveDockerComposeBack` MUST be paired in `try/finally`. Today the
scaffolding between them calls `process.exit(1)` internally, so the restore never runs and
`docker-compose.yml` is stranded in the parent directory (`src/index.ts:376-378`).

`.env` is written when absent; when already present it is left alone and the block is printed
instead (research R7).

**Filename (D6, decided): always `.env`**, for every framework. The examples disagree with each
other — `nextjs`/`astro`/`nextjs-experiments` ship `.env.local.example`, `angular-ssr`/`vuejs` ship
`.env.example`, `angular` ships neither — while the CLI today tells everyone `touch .env`. One
filename is simpler, and it is functionally safe: Next.js and Astro both read `.env` as well as
`.env.local`.

Ordering note: `cloneFrontEndSample` deletes everything in the target directory except `examples/`,
so a `.env` written *before* scaffolding would be destroyed. Writing from the exit hook (X1) happens
after scaffolding, so it survives.

## X9 — The compose file is bundled, not downloaded

The CLI ships its own compose file inside the npm package and **writes** it to the project
directory. It does not fetch it, and it does not modify the shared
`single-node-demo-site` example (D4).

Delivery goes through a swappable source so remote fetching stays one env var away:

```ts
resolveComposeSource()   // bundled asset, unless DOTCMS_COMPOSE_URL is set
```

Obtaining **contents** and writing them — rather than downloading to a path, as today — is what
makes the two sources interchangeable. This also removes `downloadFile`'s missing timeout, absent
redirect handling and lack of retry from the default path entirely.

---

## Verification

Unit-testable: X1, X2, X3, X5, X6, X7, X8 — see the Test Strategy table in [plan.md](../plan.md).
X4's compose behavior is manual — see [quickstart.md](../quickstart.md).
