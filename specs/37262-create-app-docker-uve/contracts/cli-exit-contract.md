# Contract: `@dotcms/create-app` exit behavior

**Consumer**: the person running `npx @dotcms/create-app`, and any script wrapping it.

The reported failure is a contract violation: the CLI held a working token and site ID and exited
without printing either. These are the guarantees the fix establishes.

---

## X1 — No successful state is ever discarded

> Once `token` and `siteId` are non-null, **every** terminal path — success, handled failure, or
> unexpected throw — emits `host`, `token`, `siteId`, and writes `.env`.

Implemented as an emit step that runs from a `finally`-equivalent position, not from the happy path.
This is the single most important guarantee here: it converts every future unanticipated failure
from total loss into a recoverable one.

## X2 — Optional steps are non-fatal

UVE configuration is **optional**. Its failure MUST:

1. warn — never `process.exit`;
2. print the [headless UVE guide](https://dev.dotcms.com/docs/author/pages-and-visual-editing/universal-visual-editor/uve-headless-config)
   with this run's `host`, `siteId`, and app key `dotema-config-v2`;
3. continue to scaffolding;
4. exit **0** with a complete project.

**Exit-code change**: a run that previously exited `1` now exits `0` with a warning. Deliberate, and
called out in the spec's Regression Risk — any wrapper asserting the old behavior will see it.

## X3 — Write only after a successful read

The UVE `POST` is permitted only after a `GET` of the same resource returns 200.

- `GET` is polled while the instance settles.
- `POST` retries on `401`, `403`, `5xx` (transient permission / still-settling signatures).
- `POST` does **not** retry other `4xx` — a real client error retrying cannot fix.

Rationale: `/dotmgt/readyz` covers CDI, memory, threads and the servlet container — not starter
import, role seeding, or permission-cache warm-up (research R5). Transport readiness is not
data-plane readiness.

## X4 — Progress is truthful

- "Containers started successfully" is printed only when containers are actually running and
  healthy — via `docker compose up -d --wait` with an explicit `--wait-timeout`.
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
offers to reuse that instance instead of exiting. Only a busy port that is *not* dotCMS is an error.

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

---

## Verification

Unit-testable: X1, X2, X3, X5, X6, X7, X8 — see the Test Strategy table in [plan.md](../plan.md).
X4's compose behavior is manual — see [quickstart.md](../quickstart.md).
