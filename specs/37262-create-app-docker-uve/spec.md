# Issue Resolution Specification: create-app local Docker run never starts dotCMS, then a transient UVE 403 aborts the CLI and discards the project

**Feature Branch**: `37262-create-app-docker-uve`

**Created**: 2026-08-28

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37262](https://github.com/dotCMS/core/issues/37262)

**Input**: User description: "https://github.com/dotCMS/core/issues/37262"

## Problem Statement *(mandatory)*

`npx @dotcms/create-app` — the entry point promoted by the [Headless dotCMS Quick Start blog
post](https://www.dotcms.com/blog/headless-dotcms-quick-start-introducing-dotcms-create-app-cli)
— fails end to end when the user picks the "spin up dotCMS locally with Docker" path. Two
independent defects compound into total data loss for the run:

1. **dotCMS never actually starts.** The compose file the CLI downloads lets the `dotcms`
   container boot before Postgres and OpenSearch are accepting connections, and gives it no
   restart policy — so it exits and stays exited. The CLI meanwhile reports "containers
   started successfully" and burns its retry budget probing a container that is not running.
   The user only gets past this by manually pressing ▶ in Docker Desktop.

2. **A single non-essential config call aborts the whole run.** Once dotCMS is up, the CLI
   obtains an API token and resolves the default site — both succeed — then the Universal
   Visual Editor (UVE) app-configuration `POST` returns 403 and the CLI calls `process.exit(1)`.
   Because UVE setup runs *before* scaffolding, the user is left with an empty directory: no
   project, no `.env`, and the working token and site ID are discarded without ever being
   printed.

Recovery is then blocked by the CLI's own side effects: the port pre-check hard-fails on the
now-running dotCMS's ports, and the directory-clearing prompt would delete the
`docker-compose.yml` needed to tear that instance down.

**Severity / Impact**: High — the documented first-run experience for headless dotCMS is
broken for every new user on the local-Docker path. Affects evaluators and new developers at
their very first contact with the product, and the failure destroys work that had already
succeeded rather than degrading gracefully.

## Reproduction *(mandatory)*

**Environment**: `@dotcms/create-app` 1.2.5 · `dotcms/dotcms:latest` · compose fetched at
runtime from `dotCMS/core@main` (`docker/docker-compose-examples/single-node-demo-site/docker-compose.yml`)
· macOS + Docker Desktop · no pre-existing dotCMS containers, cold image cache

**Steps to Reproduce**:

1. On a machine with Docker Desktop running and no dotCMS containers present, run
   `npx @dotcms/create-app my-app`.
2. Answer the prompts: target directory `.`, "No - Spin up dotCMS locally with Docker",
   framework "Next.js".
3. Observe the `dotcms` container start and immediately exit while `db` and `opensearch` are
   still initializing. The CLI nonetheless prints "dotCMS containers started successfully" and
   enters its health-check retry loop.
4. Manually start the `dotcms` container from Docker Desktop. Because Postgres and OpenSearch
   are healthy by now, it boots.
5. Once the CLI's health check passes, observe `failed to setup UVE config: status=403,
   code=ERR_BAD_REQUEST` and a non-zero exit with nothing scaffolded.
6. Re-run the same command. It now aborts at "Required ports are already in use", because the
   dotCMS started in step 4 holds 8082/8443/9200/9600.

**Expected Behavior**:

- The compose stack brings dotCMS up on its own, ordered behind healthy `db` and `opensearch`,
  and restarts it if it exits.
- The CLI reports container start truthfully, and its readiness signal reflects that the calls
  it is about to make will succeed.
- A failure in optional UVE configuration warns and continues; the project is still scaffolded.
- No successful run state (host, token, site ID, `.env`) is ever discarded on exit.
- A second run is possible without tearing down a healthy instance.

**Actual Behavior**:

```
✔ dotCMS containers started successfully.        ← the container had already exited
⏳ dotCMS not ready (attempt 1/60) - ECONNRESET - Retrying in 5s...
   … 11 more attempts …
✔ dotCMS is running locally at http://localhost:8082
✔ Generated API authentication token             ← discarded on exit
✔ Retrieved default site (8a7d5e23-da1e-…)       ← discarded on exit
failed to setup UVE config: status=403, code=ERR_BAD_REQUEST
✖ Failed to setup UVE configuration in Dotcms.
```

Exit code 1, empty target directory.

**Reproducibility**: The compose defect (steps 3–4) is deterministic on any cold start where
dotCMS wins the race against Postgres. The 403 (step 5) is timing-dependent: replaying the
CLI's exact three-call sequence against a fully-settled dotCMS returns 200 for all three, so it
reproduces when the CLI reaches the UVE call while the instance is still settling.

## Scope of Investigation *(mandatory)*

- **Affected area**: Three surfaces, in priority order.
  1. **Docker compose examples** — `docker/docker-compose-examples/single-node-demo-site/docker-compose.yml`.
     This is the least hardened example in that directory; six siblings already use
     `condition: service_healthy`, and `lgtm-observability/docker-compose.yml` is the model.
     Verified in-repo: `dotcms` has `depends_on: [db, opensearch]` with no condition, no
     `restart:`, no healthcheck, and does not publish 8090; `opensearch` has no healthcheck and
     no `restart:`; only `db` defines a healthcheck, which nothing consumes.
  2. **`@dotcms/create-app` CLI** — `core-web/libs/sdk/create-app` (v1.2.5). Error handling,
     readiness probing, ordering of side effects, and recovery.
  3. **Backend (follow-up only)** — the Apps API permission path that produces the 403.
- **Suspected surface**: The CLI and compose work is frontend/SDK + infrastructure — no Java.
  The P2 backend follow-up touches **legacy** `com.liferay.portal.model.User` alongside modern
  `com.dotcms.security.apps.AppsAPIImpl`, so it carries legacy-impact weight and is deliberately
  separated from the P0/P1 fix.
- **Related known decisions**:
  - **ADR-0019 — Date-Lockstep Versioning for the dotCMS SDKs (accepted, binding).** The SDK
    version *is* the dotCMS release version it ships with, in both directions. This governs the
    "pin the image tag" item: the CLI must not pull `dotcms/dotcms:latest` against a hardcoded
    `starter-20260630` URL, and a CLI fix ships via a dotCMS release rather than a standalone
    SDK publish.
  - ADR-0016 (Docker container naming, *proposed*) is an unfilled template and imposes nothing.
  - The plan phase formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

**Cause 1 — compose ordering and restart policy (confirmed by reading the file).** `dotcms`
depends on `db` and `opensearch` without `condition: service_healthy`, so it starts against a
Postgres that is not yet accepting connections and dies. With no `restart:` policy it stays
dead. The `db` healthcheck that would have prevented this already exists and is simply unused.

**Cause 2 — the 403 is a startup race, not a permissions problem.** Ruled out by evidence in
the report: license gating (`LicenseUtil.getLevel()` has returned `PLATFORM` unconditionally
since #31261, Feb 2025, making the `InvalidLicenseException` path dead on any current image);
Apps-portlet access (`GET /api/v1/apps` and `GET /api/v1/apps/dotema-config-v2/{siteId}` both
return 200 for a token minted the way the CLI mints one); and a wrong site ID. What remains is
`AppsAPIImpl.userDoesNotHaveAccess()` (`AppsAPIImpl.java:104`) calling `user.isAdmin()`, which
is wrapped in `Try.of(…).getOrElse(false)` (`com/liferay/portal/model/User.java:321`) — so
*any* exception during the role lookup silently reports "not an admin", becomes a
`DotSecurityException`, and maps to 403.

The timing supports this: the CLI's readiness probe is `/api/v1/appconfiguration`, which answers
as soon as the web layer is up — it went green ~60s after container start, far too early for a
demo-starter import to have completed. The CLI then wrote app secrets to an instance still
settling roles, permissions and caches.

**The readiness signal is therefore wrong.** dotCMS ships a real readiness probe at
`/dotmgt/readyz` (verified: responds `ready`, unauthenticated, no IP ACL) — but only on port
**8090**, which this compose does not publish (`/dotmgt/readyz` on 8080 is a 404). Even
`/readyz` is not sufficient: its registered checks cover CDI, memory, threads and the servlet
container, not "starter import finished". For a CLI the reliable rule is **readiness means the
call you are about to make succeeds** — gate the write on a successful read of the same
resource.

**Cause 3 — the CLI discards recoverable state.** Independent of causes 1 and 2, and the reason
a transient failure becomes total loss. Verified in-repo:

- UVE setup exits at `src/index.ts:370`; the clone and `npm install` at `:377` never run.
- Token and site ID are obtained successfully but only printed by `displayFinalSteps()`, which
  is downstream of the exit.
- The UVE call has no retry, while authentication retries 3×.
- `checkPortsAvailability()` (`src/utils/index.ts:479`) hard-fails on 8082/8443/9200/9600 —
  exactly the ports a successful previous run now holds.
- `prepareDirectory()` (`src/asks.ts:180`) offers to empty the target directory, which would
  delete the `docker-compose.yml` needed for `docker compose down`.

**Additional defects found while reading** (all verified in-repo):

- `npm install` failure is unreachable: `installDependenciesForProject()` returns a `Result`,
  but `src/index.ts:597` tests `if (!result)`. `Err()` returns `{ ok: false, val }` — a truthy
  object, so the failure branch never fires and a failed install reports success.
- Orphaned compose file: `moveDockerComposeOneLevelUp()` runs at `src/index.ts:376`; if
  scaffolding fails it calls `process.exit(1)` internally, so `moveDockerComposeBack()` at
  `:378` never runs and `docker-compose.yml` is stranded in the parent directory. Needs
  `try/finally`.
- Multi-minute silence: `execa('docker', ['compose','up','-d'])` swallows image-pull progress,
  leaving a frozen spinner for the length of a ~1.5GB pull on a cold machine.
- Unguarded download: `downloadFile()` uses raw `https.get` with no timeout, no retry and no
  redirect handling, against an unpinned `main` URL.
- Status mismatch: `fetchWithRetry` accepts any 2xx; `isDotcmsRunning` then demands exactly 200.
- Interleaved output: `fetchWithRetry` calls `console.log` while an `ora` spinner is active —
  the cause of the mangled retry block in the log above.
- No tests: the package contains no spec file (confirmed — E2E suite tracked in #35096).

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

*Compose (P0 — ships without a CLI release, because the CLI fetches this file from `main` at
runtime, so it reaches every already-installed CLI immediately):*

- Add an `opensearch` healthcheck and `restart: unless-stopped`.
- Change `dotcms` `depends_on` to `condition: service_healthy` for both `db` and `opensearch`.
- Add a `dotcms` healthcheck against `http://127.0.0.1:8090/dotmgt/livez` with
  `start_period: 180s`, plus `restart: unless-stopped`.
- Publish port `8090:8090`.

*CLI (P0):*

- The CLI never exits without printing recoverable state (host, token, site ID), and writes the
  `.env` it already has every value for.
- UVE setup failure is non-fatal: warn, print manual setup steps, and continue to scaffolding.
  The warning must link the user to the official headless UVE configuration guide —
  <https://dev.dotcms.com/docs/author/pages-and-visual-editing/universal-visual-editor/uve-headless-config>
  — alongside the concrete values they need (host, site ID, and the app key
  `dotema-config-v2`), so the one unset setting is self-serviceable rather than a dead end.
- Gate the UVE write on a read — poll `GET` on the UVE app endpoint until 200, then `POST` with
  retry on 401/403/5xx.

*CLI (P1):*

- Port check probes before failing: if dotCMS already answers on 8082, offer to reuse it rather
  than exiting.
- Use `docker compose up -d --wait` and stream pull progress so the wait is visible.
- Fix the truthy-`Result` check at `src/index.ts:597`; wrap the compose move in `try/finally`.
- Switch readiness to `/dotmgt/readyz` on 8090 once the compose publishes it, keeping
  `/api/v1/appconfiguration` as fallback.

**Explicitly out of scope / non-goals**:

- **Changing `user.isAdmin()` exception handling** (`com/liferay/portal/model/User.java:321`)
  or `AppsAPIImpl.userDoesNotHaveAccess()`. This is a real defect — a transient role-lookup
  failure should not read as a permission denial — but it is legacy Liferay code on a hot
  permission path with a wide blast radius, and the P0 fix does not depend on it. Tracked as a
  P2 follow-up, specified and planned separately.
- **Pinning the dotCMS image tag alongside `CUSTOM_STARTER_URL`.** Real drift risk
  (`latest` + hardcoded `starter-20260630`) and it intersects binding ADR-0019, so it deserves
  its own decision rather than being folded into a bug fix. P2 follow-up.
- **Landing the E2E suite from #35096.** That issue owns it. This fix adds unit-level tests for
  the logic it changes; the fault-injection E2E case (kill dotCMS mid-run) belongs to #35096.
- Rewriting the CLI's `Result` type, prompt flow, or framework-scaffolding logic beyond the
  specific defects listed above.
- Hardening the other compose examples in `docker/docker-compose-examples/`. Only
  `single-node-demo-site` is fetched by the CLI; the rest are out of this fix's blast radius.
- Any change to the Apps REST API contract or the UVE app definition itself.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - *Compose:* `single-node-demo-site/docker-compose.yml` is fetched from `main` at runtime by
    every installed `@dotcms/create-app`, so a change ships instantly and unversioned to all
    existing CLI users — including older CLI versions that will not know about port 8090. This
    cuts both ways: it is why the fix is P0 and reaches users without a release, and it is the
    single largest regression risk in this work. The file is also used directly by users
    following the demo-site README. The added `dotcms` healthcheck must not fail on images
    where `/dotmgt/livez` behaves differently, or the container will be marked unhealthy and
    (with `restart: unless-stopped`) flap.
  - *CLI:* making UVE failure non-fatal changes the exit contract — a run that previously
    exited 1 will now exit 0 with a warning. Any CI or script asserting on the old behavior
    would see a behavior change. `--wait` on `docker compose up` changes how long the command
    blocks and requires the healthchecks above to be correct, or the CLI hangs until timeout
    instead of proceeding.
  - Fixing the truthy-`Result` check at `src/index.ts:597` makes a previously-unreachable
    failure branch reachable: runs with a broken npm that silently "succeeded" before will now
    correctly fail. This is the intended fix, but it is a visible behavior change.
- **Backward compatibility**: No dotCMS API contract, serialized state, DB schema or ES mapping
  changes. Publishing 8090 exposes the management port on the host for local demo stacks —
  acceptable for a local developer stack, but it must be stated in the README rather than
  introduced silently. Per ADR-0019, a CLI change ships as part of a dotCMS release, not a
  standalone SDK publish.
- **Data considerations**: None. No migration, no repair of existing data. Users left with an
  empty directory by the old behavior simply re-run; the P1 port-reuse work is what makes that
  re-run possible without tearing down a healthy instance.

### Required reviewers for the compose change

`.github/CODEOWNERS` does not cover `docker/`, so the compose file has no automatic reviewer
despite being the highest-blast-radius part of this fix. Reviewers are therefore drawn from
`git blame` on the exact hunks being changed. Note that the two largest raw blame counts are
mechanical — a bulk restore (#27432) and a `pgvector` version bump (#29915) — so the list below
weights *who shaped the design* over line count:

| Reviewer | Why they should review |
| --- | --- |
| **Steve Bolton** (`spbolton`) | Dominant blame on every hunk in scope — the `dotcms` service block, its `depends_on`, and its `ports`. Also authored `lgtm-observability/docker-compose.yml` (#32980), which is the `condition: service_healthy` pattern this change copies. Best-placed to say whether we are applying that pattern faithfully. |
| **Will Ezell** | Authored the OpenSearch 1.x + SSL setup in this file (#27754) and the Postgres 18 upgrade (#34236) that touched both this file and the model stack. Owns the current `db`/`opensearch` shape we are adding healthchecks to. |
| **Erick González** (`erickgonzalez`) | Most recent semantic change to the `dotcms` service (#36490, 2026-07-13) and prior starter-version work (#36362). Closest to the `CUSTOM_STARTER_URL` / starter-import behavior that the readiness race depends on. |
| **Daniel Colina** | Holds 13 lines of the `opensearch` block and part of the `depends_on` region via #29915. Secondary — loop in if the OpenSearch healthcheck shape is contested. |

Two review questions to put to them explicitly, since neither is settled by this spec:

1. **Publishing `8090:8090`** exposes the management port on the host for every user of this
   demo stack, not just CLI users. Acceptable for a local demo, or should the CLI reach it
   another way?
2. **`restart: unless-stopped` plus a `/dotmgt/livez` healthcheck** will flap the container if
   that endpoint behaves differently on some image tag. Is `start_period: 180s` enough headroom
   for a cold starter import on a slow machine?

## Acceptance & Verification *(mandatory)*

- **AC-001**: On a cold machine with no dotCMS containers, `npx @dotcms/create-app` on the
  local-Docker path brings the stack up **without manual intervention** — `dotcms` starts only
  after `db` and `opensearch` report healthy, and is restarted if it exits. Reproduction steps
  3 and 4 no longer occur.
- **AC-002**: The CLI's "containers started successfully" message is only printed when the
  containers are actually running and healthy.
- **AC-003**: When the UVE configuration call fails for any reason, the CLI prints a warning,
  **continues to scaffolding**, and exits 0 with a complete project. Reproduction step 5 no
  longer aborts the run. The warning includes the headless UVE configuration guide
  (<https://dev.dotcms.com/docs/author/pages-and-visual-editing/universal-visual-editor/uve-headless-config>)
  and the run-specific values needed to follow it — host, site ID, and app key
  `dotema-config-v2`. A user who hits this path can finish the setup by hand without leaving
  the terminal to go hunting for docs.
- **AC-004**: On **any** exit path after a token has been issued, the CLI prints the host,
  token and site ID, and writes the `.env` file from the values it holds. No successful state
  is discarded.
- **AC-005**: The UVE write is gated on a successful `GET` of the same resource, and retries on
  401/403/5xx rather than failing on first response.
- **AC-006** *(P1)*: With a healthy dotCMS already answering on 8082, a second run offers to
  reuse it instead of aborting with "Required ports are already in use". Reproduction step 6 no
  longer blocks.
- **AC-007** *(P1)*: A failed `npm install` causes the CLI to report failure — the branch at
  `src/index.ts:597` is reachable and correct.
- **AC-008** *(P1)*: If scaffolding fails after `moveDockerComposeOneLevelUp()`, the compose
  file is restored to the project directory (no orphan in the parent).
- **AC-009**: Image-pull progress is visible during `docker compose up`; no silent multi-minute
  spinner. Retry messages do not interleave with an active `ora` spinner.
- **AC-010**: No regression to the other `docker/docker-compose-examples/*` stacks, and the
  demo-site README documents the newly published 8090 port.

- **Verification method**:
  - *Compose:* `docker compose -f docker/docker-compose-examples/single-node-demo-site/docker-compose.yml up -d --wait`
    from a cold state (no volumes, no cached images), asserting `dotcms` reaches healthy without
    manual start; plus a fault-injection run (`docker kill` the `dotcms` container) asserting it
    is restarted.
  - *CLI unit tests:* the package currently has **no spec file at all**, so this fix establishes
    the harness. Jest specs covering, at minimum: the `Result` truthiness fix, the non-fatal UVE
    path, the read-before-write gate with a mocked 403-then-200 sequence, the `try/finally`
    compose move, and the port-reuse probe. Per constitution Principle V these are written and
    confirmed failing (Red) before the implementation lands.
  - *Manual end-to-end:* the reproduction steps above, run cold on macOS + Docker Desktop,
    verifying AC-001 through AC-006.
  - The full fault-injection E2E suite is #35096's scope, not this fix's.

## Assumptions

- The reporter's finding that the three-call sequence returns 200 against a fully-started
  dotCMS is taken as given; the fix targets the race rather than re-deriving the permission
  analysis.
- `/dotmgt/livez` and `/dotmgt/readyz` are available on port 8090 on `dotcms/dotcms:latest` and
  respond unauthenticated, as verified in the report. The plan should confirm this against the
  specific image the compose file pins.
- Making UVE configuration optional is acceptable product behavior: a scaffolded project with
  one unset editor configuration, printed manual steps and a link to the headless UVE guide is
  strictly better than an empty directory. **Confirmed by the issue owner** — conditional on the
  warning telling the user how to finish the setup, which AC-003 now requires.
- The compose change shipping unversioned to all installed CLI versions is acceptable and
  desirable, given every affected version is currently broken on this path.
