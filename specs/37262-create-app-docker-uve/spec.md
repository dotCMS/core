# Issue Resolution Specification: create-app local Docker run never starts dotCMS, and the resulting broken instance 403s the UVE call and discards the project

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
   printed. The 403 is not incidental: hand-starting the container in step 1 leaves the starter
   import incomplete, the site's permissions unwritten, and the instance **permanently** unable
   to serve the Apps API. The two defects are one causal chain, not two independent bugs.

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

**Reproducibility**: The compose defect (steps 3–4) occurs on a cold start where dotCMS wins the
race against Postgres; it is timing-dependent and did **not** reproduce on a fast machine with a
warm image cache. The 403 (step 5) is **deterministic once step 3 has happened**: killing dotCMS
mid starter-import and hand-starting it reproduces 403 on every subsequent attempt (193/193 over
~7 minutes). Against a cleanly-booted instance all three calls return 200, which is why the
original report read it as transient — it is not. See Root-Cause Hypothesis, Cause 2.

## Scope of Investigation *(mandatory)*

- **Affected area**: Three surfaces, in priority order.
  1. **Docker compose examples** — `docker/docker-compose-examples/single-node-demo-site/docker-compose.yml`.
     This is the least hardened example in that directory. **Three** siblings already use
     `condition: service_healthy` (`lgtm-observability`, `single-node-metrics-monitoring`,
     `experiments`, plus `single-node-os-migration` via provision jobs) —
     `lgtm-observability/docker-compose.yml` is the model. Note none of them gates `dotcms`
     on OpenSearch being *healthy*; every one uses `db: service_healthy` +
     `opensearch: service_started`. See Fix Scope for why this fix goes further.
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

**Cause 2 — the 403 is permanent damage caused by Cause 1, not a startup race.**
*This supersedes the original hypothesis, which planning disproved by experiment.*

The report proposed a transient race: the CLI writes while the instance is still settling roles
and permissions, and `AppsAPIImpl.userDoesNotHaveAccess()` calling `user.isAdmin()` — wrapped in
`Try.of(…).getOrElse(false)` — silently reports "not an admin". **Measurement does not support
that**, and two experiments replaced it (M5/64GB host, dotCMS constrained to 2 CPUs / 4G):

*Experiment 1 — clean boot has no settling window at all.*

| Signal | First success |
|---|---|
| `POST /api/v1/authentication/api-token` | **46s** |
| `GET /api/v1/apps/dotema-config-v2/{siteId}` | **46s** |
| `/dotmgt/livez`, `/dotmgt/readyz` | 48s |
| `/api/v1/appconfiguration` (the CLI's current probe) | 49s |

The UVE endpoint is usable **two seconds before `readyz` goes green**. Server logs show why: the
starter import (T+20s) and the ES reindex (T+44s) both complete *inside* Tomcat startup
(`Server startup in [36517] milliseconds`), and the connector accepts no traffic until after
them. There is no window in which the API answers but the data plane is unready — so the
hypothesised race cannot occur on a clean boot.

*Experiment 2 — the reporter's actual path reproduces it, permanently.*

Reproducing reproduction step 4 (dotCMS killed mid starter-import, then hand-started):

```
T+39s  appconfiguration 200 — CLI proceeds
T+41s  api-token   -> 200
T+41s  defaultSite -> 200
T+41s  UVE GET -> 403   UVE POST -> 403
   … 193 consecutive attempts over ~7 minutes, zero successes …
T+440s UVE GET -> 403   UVE POST -> 403
```

That is the reported log line for line. The server states the cause plainly:

```
DotSecurityException: User 'Admin User [ID: dotcms.org.1][email:admin@dotcms.com]'
  does not have READ permissions on Site 'demo.dotcms.com'
```

The interrupted import never wrote the site's permission rows. The restart re-ran
`Task00004LoadStarter` and Tomcat came up clean, but the permissions never appeared. **The
instance does not recover** — only `docker compose down -v` and a fresh start does.

**So the causal chain is**: Cause 1 (dotCMS races Postgres and exits) → user hand-starts the
crashed container → the starter import is left incomplete → site permissions are missing →
**every** Apps API call 403s, forever. Cause 2 is a *consequence* of Cause 1, not an independent
defect. **Fixing the compose file removes it.**

What this rules out, on evidence rather than inference: it is not license gating
(`LicenseUtil.getLevel()` has returned `PLATFORM` unconditionally since #31261), not
Apps-portlet access, not a wrong site ID, and **not** `user.isAdmin()` swallowing an exception —
the permission data is genuinely absent, so `isAdmin()` has nothing to throw about.

**Consequences for the fix** (these change the design, not just the narrative):

- **Retrying or polling the UVE call cannot work.** A read-before-write gate that polls `GET`
  until 200 would poll forever against a condition that never clears. A single probe is correct;
  retry only `5xx`.
- **The failure guidance must change.** "Configure UVE manually at this URL" is useless advice
  here — manual configuration fails identically. The CLI must say the instance is unrecoverable
  from an interrupted first boot and must be recreated with `docker compose down -v`.
- **A separate backend defect is implied**: any interrupted first boot silently bricks the
  instance, and a restart neither repairs nor reports it. That is broader than this issue and is
  filed separately.

**On the readiness signal.** Switching to `/dotmgt/readyz` on 8090 is still worth doing — it is
the purpose-built probe and does not depend on the web app — but it is a correctness tidy-up,
not the fix for the 403. Experiment 1 shows `/api/v1/appconfiguration` is not meaningfully late.

*Evidence limits*: one host, one starter, one image; the kill point was fixed at 25s. Which
kill-points corrupt and which do not is unmapped, and why a re-run import leaves permissions
missing is a backend question, not a CLI one.

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
  Gating on `db` is what fixes the reported crash. Gating on `opensearch` as well is a
  **deliberate deviation** from all four existing examples, which use `service_started` there —
  justified because this stack is driven by an unattended CLI, so an OpenSearch that is
  up-but-not-ready is a failure with nobody present to diagnose it. The OpenSearch probe should
  be the one already proven in `single-node-os-migration` (`-k` for the self-signed cert,
  `-u admin:admin`, since this stack sets `DOT_ES_AUTH_BASIC_PASSWORD: 'admin'`), not a new one.
- Add a `dotcms` healthcheck against `http://127.0.0.1:8090/dotmgt/livez` with
  `start_period: 180s`, plus `restart: unless-stopped`.
- Publish the management port **bound to loopback**: `127.0.0.1:8090:8090` — not `8090:8090`.
  The management port is authorized purely by the port a request arrives on
  (`InfrastructureManagementFilter`): no credential check, no IP allowlist. A wildcard binding
  would put `/dotmgt/health` and `/dotmgt/metrics` on the local network for every user of this
  demo stack. Loopback gives the CLI and the container's own healthcheck everything they need
  (both already use `127.0.0.1`) and gives the network nothing. This is stricter than the two
  existing examples that publish 8090.

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

- **The backend defect behind the 403.** Planning disproved the original `user.isAdmin()`
  exception-swallowing hypothesis: the permission rows are genuinely absent, so there is no
  exception to swallow. The real backend defect is larger and worse — **an interrupted first boot
  silently bricks the instance**: the starter import leaves site permissions unwritten, a restart
  re-runs `Task00004LoadStarter` and reports success, and every Apps API call 403s forever with no
  warning to the user. That is out of scope here (it is a starter-import/permissions problem in
  legacy `com.dotmarketing.*`, not a CLI one) and is filed separately. This fix removes the
  *trigger* by stopping the crash.
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
    where `/dotmgt/livez` behaves differently. Note the failure mode is **not** a restart loop:
    Compose restart policies react to container *exit*, not to health status (health-driven
    restart is a Swarm feature), so an unhealthy container is simply never marked ready. The
    consequence is that `depends_on` and `--wait` block on it — see the CLI bullet below.
  - *Compose ↔ installed CLIs:* the file MUST keep a line matching
    `/^(\s*["']?CUSTOM_STARTER_URL["']?\s*:\s*).+$/m`. `updateDockerComposeStarterUrl`
    (`src/index.ts:487`) rewrites the compose file with that regex when `--starter` is passed
    and **throws if there is no match**. Reformatting that key into a YAML block scalar, an
    anchor, or a `- KEY=value` list entry would break `--starter` for every already-installed
    CLI — with no release able to reach them.
  - *CLI:* making UVE failure non-fatal changes the exit contract — a run that previously
    exited 1 will now exit 0 with a warning. Any CI or script asserting on the old behavior
    would see a behavior change. `--wait` on `docker compose up` changes how long the command
    blocks and requires the healthchecks above to be correct: a wrong probe turns a working run
    into a block until timeout. This — not restart flapping — is the real cost of getting the
    healthcheck wrong, so `--wait` must be paired with an explicit `--wait-timeout` and a
    timeout must degrade to reported diagnostics rather than a silent hang.
  - Fixing the truthy-`Result` check at `src/index.ts:597` makes a previously-unreachable
    failure branch reachable: runs with a broken npm that silently "succeeded" before will now
    correctly fail. This is the intended fix, but it is a visible behavior change.
- **Backward compatibility**: No dotCMS API contract, serialized state, DB schema or ES mapping
  changes. Publishing 8090 exposes an **unauthenticated** management surface
  (`/dotmgt/health`, `/dotmgt/metrics`) to whoever can reach the binding — which is why the fix
  binds it to `127.0.0.1` rather than the wildcard the issue originally proposed. It must still
  be stated in the README rather than introduced silently. Per ADR-0019, a CLI change ships as
  part of a dotCMS release, not a standalone SDK publish.
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
| **Will Ezell** (`wezell`) | Authored the OpenSearch 1.x + SSL setup in this file (#27754) and the Postgres 18 upgrade (#34236) that touched both this file and the model stack. Owns the current `db`/`opensearch` shape we are adding healthchecks to, and is the most senior still-active owner of `docker/`. |
| **Erick González** (`erickgonzalez`) | Most recent semantic change to the `dotcms` service (#36490, 2026-07-13) and prior starter-version work (#36362). Closest to the `CUSTOM_STARTER_URL` / starter-import behavior that the readiness race depends on. |
| **Jose Castro** (`jcastro-dotcms`) | Not a blame match on this file. Added to cover the gap below: second-most-active contributor to `docker/` over the last 12 months. |

**Unavailable — the two strongest blame signals.** `spbolton` (Steve Bolton) holds dominant blame
on every hunk in scope *and* authored `lgtm-observability/docker-compose.yml` (#32980), the exact
`condition: service_healthy` pattern this change copies. `dcolina` (Daniel Colina) holds 13 lines
of the `opensearch` block via #29915. Neither is a collaborator on `dotCMS/core` any more (last
commits 2026-03-30 and 2026-04-07 respectively), so GitHub rejects a review request for them.

This leaves a real coverage gap: **nobody currently assignable designed the pattern being copied.**
The plan phase should treat the lgtm-observability compose as the specification for correct
usage — reading it directly rather than relying on a reviewer to catch a faithful-copy error.

Two review questions to put to them explicitly. Both were sharpened by the plan-phase research —
question 1 now carries a recommendation rather than being open, and question 2 was reframed
because its original premise was wrong:

1. **Gating `dotcms` on `opensearch: service_healthy`** goes further than all four existing
   examples, which use `service_started` there. Is the stricter gate right for a stack driven by
   an unattended CLI, or should this match the house pattern? *(Recommendation: keep the stricter
   gate, using `single-node-os-migration`'s proven probe.)*
2. **Is `start_period: 180s` enough headroom** for a cold demo-starter import on a slow machine?
   This is above both precedents (lgtm 120s, metrics-monitoring 20s). The original question —
   whether `restart: unless-stopped` would flap the container — turned out to rest on a false
   premise: Compose restart policies react to container exit, not health status, so flapping
   cannot occur. The real exposure is that a too-short `start_period` makes
   `docker compose up --wait` block until timeout.

*(Publishing 8090 was the third open question. It is now settled in Fix Scope: bind to
`127.0.0.1`, because the management port is authorized by arrival port with no credential check
and no IP allowlist. Flagging it here so reviewers see the decision rather than having to
rediscover the reasoning.)*

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
  is discarded. Note the CLI does not write `.env` today at all — it only prints `touch .env`
  plus a block to paste — so this is new behavior, not a restoration. `.env` is written when
  absent; when the scaffolded example already ships one, it is left alone and the block is
  printed as today.
- **AC-005**: The UVE write is gated on a single successful `GET` of the same resource. Retry
  applies to `5xx` only. It must **not** retry or poll on 403: a 403 here means the instance's
  permissions were never written by an interrupted starter import, and that never clears — 193
  consecutive attempts over ~7 minutes all returned 403. On 403 the CLI skips the write and tells
  the user their instance is unrecoverable and must be recreated with `docker compose down -v`,
  rather than offering manual UVE setup steps that would fail identically.
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
- **AC-011**: Port 8090 is reachable on `127.0.0.1` and **not** on the host's LAN address —
  the management surface is not exposed to the network.
- **AC-012**: `npx @dotcms/create-app --starter <url>` still works against the edited compose
  file. The `CUSTOM_STARTER_URL` rewrite regex in `updateDockerComposeStarterUrl` must still
  match, or every already-installed CLI loses `--starter`.

- **Verification method**:
  - *Compose:* `docker compose -f docker/docker-compose-examples/single-node-demo-site/docker-compose.yml up -d --wait`
    from a cold state (no volumes, no cached images), asserting `dotcms` reaches healthy without
    manual start; plus a fault-injection run (`docker kill` the `dotcms` container) asserting it
    is restarted.
  - *CLI unit tests:* the package contains **no spec file**, but the Jest harness is already in
    place (`jest.config.ts`, `tsconfig.spec.json`, `@nx/jest/plugin`), so this fix adds specs
    rather than a harness. Run with `pnpm nx test sdk-create-app` — note `project.json` sets
    `passWithNoTests: true`, so an empty run reports green; confirm the new specs are actually
    collected before trusting a pass. Jest specs covering, at minimum: the `Result` truthiness fix, the non-fatal UVE
    path, the read-before-write gate with a mocked 403-then-200 sequence, the `try/finally`
    compose move, and the port-reuse probe. Per constitution Principle V these are written and
    confirmed failing (Red) before the implementation lands.
  - *Manual end-to-end:* the reproduction steps above, run cold on macOS + Docker Desktop,
    verifying AC-001 through AC-006, plus AC-011 (a `curl` to the host's LAN address on 8090
    must be refused) and AC-012 (a `--starter` run against the edited file).
  - The full fault-injection E2E suite is #35096's scope, not this fix's.
  - **Prerequisite**: `pnpm install` in `core-web/`. Without it `pnpm nx test` fails with
    `nx: command not found`, and constitution Principle V's Red gate cannot be demonstrated.

## Assumptions

- The reporter's finding that the three-call sequence returns 200 against a fully-started
  dotCMS is taken as given; the fix targets the race rather than re-deriving the permission
  analysis.
- `/dotmgt/livez` and `/dotmgt/readyz` are available on port 8090 on `dotcms/dotcms:latest` and
  respond unauthenticated. **Partly verified**: `server.xml` defines the connector on
  `${CMS_MANAGEMENT_PORT:-8090}`, so the port is live by default, and `curl` is present in the
  image for the healthcheck. But the two existing compose examples that probe `/dotmgt/livez`
  both run `dotcms/dotcms-test:1.0.0-SNAPSHOT`, not `latest` — so the endpoint must still be
  confirmed against the released image before the healthcheck depends on it. This is the first
  step of the plan's verification guide, and a failure there invalidates the compose design.
- Making UVE configuration optional is acceptable product behavior: a scaffolded project with
  one unset editor configuration, printed manual steps and a link to the headless UVE guide is
  strictly better than an empty directory. **Confirmed by the issue owner** — conditional on the
  warning telling the user how to finish the setup, which AC-003 now requires.
- The compose change shipping unversioned to all installed CLI versions is acceptable and
  desirable, given every affected version is currently broken on this path.
