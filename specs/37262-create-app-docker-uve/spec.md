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
     This is the least hardened example in that directory. **Four** files in it use
     `condition: service_healthy`; **three** of them gate `dotcms` directly
     (`lgtm-observability` L88–96, `single-node-metrics-monitoring` L86–90, `experiments`
     L137–141), and all three use `db: service_healthy` + `opensearch: service_started`.
     `lgtm-observability/docker-compose.yml` is the model for the healthcheck shape — but note
     it publishes 8090 on the **wildcard** (L195), which this fix deliberately does not copy.
     The fourth, `single-node-os-migration`, gates `dotcms` on two provision jobs
     (`service_completed_successfully`, L229–235) that each require
     `opensearch: service_healthy` (L167–169, L189–191) — so it **does** gate on OpenSearch
     health, transitively. This fix's stricter gate therefore has prior art in this repo, one
     step removed; it is not the clean break from precedent an earlier draft of this spec
     claimed. See Fix Scope.
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
  filed as **#37268**.

**On the readiness signal.** Switching to `/dotmgt/readyz` on 8090 is still worth doing — it is
the purpose-built probe and does not depend on the web app — but it is a correctness tidy-up,
not the fix for the 403. Experiment 1 shows `/api/v1/appconfiguration` is not meaningfully late.

*Evidence limits*: one host, one starter, one image; the kill point was fixed at 25s. Which
kill-points corrupt and which do not is unmapped, and why a re-run import leaves permissions
missing is a backend question, not a CLI one.

**Cause 3 — the CLI discards recoverable state.** Independent of causes 1 and 2, and the reason
a transient failure becomes total loss. Verified in-repo:

- UVE setup exits at `src/index.ts:369–371` (the `process.exit(1)` is `:371`, the `spinner.fail`
  is `:370`); the compose move at `:376`, the clone and `npm install` at `:377` never run.
- **The same fatal block exists twice.** `src/index.ts:226–228` is byte-for-byte the same check on
  the existing-instance (`--dotcms-url`) path, with `startScaffoldingFrontEnd()` at `:232` and
  `displayFinalSteps()` at `:235` equally downstream of it. Both sites discard a working token.
  The two paths need *opposite* 403 advice, however: on the local-Docker path a 403 means the
  bricked boot and `docker compose down -v` is the fix, while on the existing-instance path the
  user supplied their own server, there is no compose stack to recreate, and a 403 means a genuine
  token or site-permission problem. See Fix Scope for the single owner that resolves this.
- Token and site ID are obtained successfully but only printed by `displayFinalSteps()`, which
  is downstream of the exit on both paths.
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

*Compose — the CLI ships its own file (P0):*

The CLI gets its **own** compose file, bundled in the npm package at
`core-web/libs/sdk/create-app/assets/docker-compose.yml`. The shared
`docker/docker-compose-examples/single-node-demo-site/docker-compose.yml` is **not changed** —
it keeps serving README readers and already-installed CLIs exactly as today.

This reverses the original plan, which hardened the shared file. Owning the file removes the
largest risk in this work: every hardening step we want (gating on OpenSearch health, publishing
8090, healthchecks that `--wait` depends on) was otherwise a behavior change shipped unversioned
to consumers who never asked for it — and gating on OpenSearch in particular introduced a way for
dotCMS to **never start** if that probe later broke, e.g. an `opensearch:1` → `:2` bump
invalidating `admin:admin`.

- `db` and `opensearch` both get healthchecks and `restart: unless-stopped`. The OpenSearch probe
  is the one proven in `single-node-os-migration` (L61–65),
  `curl -sk https://localhost:9200 -u admin:admin | grep -q cluster_name`,
  **verified on this stack at ~15s**. **Decided** over a credential-free variant (accept `200` or
  `401` from the HTTP layer, which would drop the `admin:admin` coupling entirely): the coupling is
  the probe's only real exposure, and it is contained — the image tag is pinned to major `1`, so
  the `opensearch:1` → `:2` bump that would invalidate the default credentials requires a
  deliberate edit to *this* file by someone who then owns the probe. A proven probe beats an
  unproven one on the critical path.
- `dotcms` `depends_on` gates on `condition: service_healthy` for **both**. Safe here in a way it
  was not on the shared file: nothing else reads this one. There is also transitive prior art for
  it in `single-node-os-migration` — see Scope of Investigation.
- `dotcms` healthcheck on `http://127.0.0.1:8090/dotmgt/livez`, `start_period: 180s` (~4× the
  measured ~46s boot), plus `restart: unless-stopped`. Overshooting `start_period` is free — the
  first successful probe ends the window immediately — while undershooting it is not: see
  Regression Risk.
- Management port published **loopback-only**: `127.0.0.1:8090:8090`. It is authorized purely by
  arrival port — no credential check, no IP allowlist — so a wildcard binding would put
  `/dotmgt/health` and `/dotmgt/metrics` on the local network.
- The file is **bundled, not downloaded**, removing `downloadFile`'s missing timeout, absent
  redirect handling and lack of retry from the default path. A `ComposeSource` interface keeps
  remote fetching one env var away (`DOTCMS_COMPOSE_URL`) for field hotfixes.
- The dotCMS image tag stays `latest` for now, so the drift the report flagged remains open.

**Accepted consequence**: users on `@dotcms/create-app` ≤1.2.5 keep fetching the old shared file and
are not repaired. This is a tool for starting fresh local instances, not a CI dependency, no known
users have it in CI, and `npx @dotcms/create-app` resolves to the latest published version anyway —
so only a warm npx cache stays behind.

*CLI (P0):*

- **A single owner for UVE configuration.** Both call sites — `src/index.ts:226` (existing
  instance) and `:369` (local Docker) — are replaced by one
  `configureUVE({ host, siteId, token, mode })`, where `mode` is `'local' | 'remote'`. It owns the
  probe, the retry policy, the non-fatal contract and the cause-specific messaging, and it **never
  calls `process.exit`** — it returns an outcome the caller warns on and continues past.
  **Decided** over fixing the two sites in place: the exit contract then lives in one place rather
  than two that have already drifted once, and a third call site cannot silently miss it. This is
  the only structural change in scope — the `Result` type, prompt flow and scaffolding logic stay
  untouched (see non-goals).
- The CLI never exits without printing recoverable state (host, token, site ID), and writes the
  `.env` it already has every value for. This holds on **both** paths.
- UVE setup failure is non-fatal: warn and continue to scaffolding. The message depends on why it
  failed **and on which path is running**, because the cases need opposite advice:
  - **403, `mode: 'local'` (terminal)** — the instance's permissions were never written by an
    interrupted first boot. Manual UVE configuration would fail identically, so **do not** offer
    the manual steps. Tell the user the instance is unrecoverable and to run
    `docker compose down -v && docker compose up -d --wait`, and reference #37268.
  - **403, `mode: 'remote'`** — the user supplied their own server. There is no stack to recreate,
    so `docker compose down -v` would be nonsense advice here. Report that the API token lacks
    permission on the resolved site, name the site ID and app key `dotema-config-v2`, and link the
    guide below so the setting can be applied by hand — which on this path will work.
  - **anything else** — link the official headless UVE configuration guide,
    <https://dev.dotcms.com/docs/author/pages-and-visual-editing/universal-visual-editor/uve-headless-config>,
    alongside the concrete values needed (host, site ID, app key `dotema-config-v2`), so the one
    unset setting is self-serviceable rather than a dead end.
- Probe before the UVE write — a **single** `GET` on the UVE app endpoint; on 200, `POST` with
  retry on `5xx` **only**. Never poll, and never retry a 403: measurement showed a 403 here is
  terminal (193 consecutive failures over ~7 minutes), so a poll would spin forever. On 403 the
  CLI reports the instance as unrecoverable and stops — see the terminal-403 message below.

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
  legacy `com.dotmarketing.*`, not a CLI one) and is filed as **#37268**. This fix removes the
  *trigger* by stopping the crash.
- **Pinning the dotCMS image tag alongside `CUSTOM_STARTER_URL`.** Real drift risk
  (`latest` + hardcoded `starter-20260630`). **Deferred on scope, not on ADR grounds.** An earlier
  revision said it "intersects binding ADR-0019, so it deserves its own decision"; reading
  ADR-0019 in full, that is backwards. The ADR's *motivating problem* is precisely a client pinned
  to `latest` against a mismatched instance producing "a cryptic runtime failure", and under
  date-lockstep `@dotcms/create-app@X` corresponds to dotCMS release `X` by construction — so the
  ADR supplies the version to pin rather than obstructing the choice. The honest reason to defer
  is that `latest` is what ships today, so leaving it preserves the status quo instead of
  introducing new drift inside a bug fix. P2 follow-up.
- **Landing the E2E suite from #35096.** That issue owns it. This fix adds unit-level tests for
  the logic it changes; the fault-injection E2E case (kill dotCMS mid-run) belongs to #35096.
- Rewriting the CLI's `Result` type, prompt flow, or framework-scaffolding logic beyond the
  specific defects listed above. The one exception, stated in Fix Scope, is extracting
  `configureUVE()` so the two duplicated UVE call sites share a single owner — that is a
  precondition for AC-003 holding on both paths, not a general refactor.
- Hardening the other compose examples in `docker/docker-compose-examples/`. Only
  `single-node-demo-site` is fetched by CLI versions ≤1.2.5; the rest are out of this fix's blast
  radius, and from this version on the CLI fetches none of them.
- Any change to the Apps REST API contract or the UVE app definition itself.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - *Compose:* **substantially reduced by the rescope.** The CLI's compose file is bundled in its
    own npm package, so it reaches only users of the version that ships it. The shared
    `single-node-demo-site` example — which is fetched from `main` by every installed CLI and read
    directly by README users — is **not modified**, so it carries no risk at all. This was
    previously the single largest regression risk in the work; owning the file removes it.
  - *What the rescope costs:* users on ≤1.2.5 keep the old, broken shared file. Accepted (see Fix
    Scope) because this starts fresh local instances rather than serving CI, and `npx` resolves to
    the latest published version anyway.
  - *New risk introduced by bundling:* if the compose asset is not listed in `package.json` `files`
    **and** `project.json`'s esbuild `assets`, it ships missing and **every** local-Docker run fails
    at the first step. Covered by AC-013 — this is the most likely way to break the release.
  - *New risk introduced by strict gating:* `dotcms` now waits for `opensearch` healthy. If that
    probe ever breaks — an `opensearch:1` → `:2` bump invalidating `admin:admin` is the realistic
    case — dotCMS will not start at all, where today it would. Contained to this CLI's own stack,
    and the probe is verified working at ~15s, but it is a genuine new failure mode.
  - *CLI:* making UVE failure non-fatal changes the exit contract — a run that previously exited 1
    now exits 0 with a warning. Any CI or script asserting the old behavior would see it.
  - Fixing the truthy-`Result` check at `src/index.ts:597` makes a previously-unreachable failure
    branch reachable: runs with a broken npm that silently "succeeded" before will now correctly
    fail. Intended, but a visible behavior change.
  - *`CUSTOM_STARTER_URL`:* the file must keep a line matching
    `/^(\s*["']?CUSTOM_STARTER_URL["']?\s*:\s*).+$/m`, or `updateDockerComposeStarterUrl` throws
    and `--starter` breaks. Substantially de-risked by the rescope: the regex and the file it
    rewrites now ship in the same package, same repo, same version, so they cannot drift
    independently the way they could when the file was fetched from `main` at runtime. Guarded by a
    Jest spec that runs the rewrite against the real bundled asset — see AC-012.
  - *`start_period` too short:* if the window elapses while dotCMS is still booting, probe failures
    begin counting toward `retries` and the container is marked `unhealthy`, at which point
    `docker compose up --wait` **aborts** rather than waiting — the CLI gives up on an instance that
    would have been healthy moments later, and `restart: unless-stopped` will not rescue it because
    restart policies react to exit, not health. This is why `start_period` is set generously at
    `180s`: the first successful probe ends the window immediately, so overshooting costs nothing.
- **Backward compatibility**: No dotCMS API contract, serialized state, DB schema or ES mapping
  changes. Publishing 8090 exposes an **unauthenticated** management surface
  (`/dotmgt/health`, `/dotmgt/metrics`) to whoever can reach the binding — which is why it is bound
  to `127.0.0.1` rather than the wildcard the issue originally proposed, and must be stated in the
  CLI's README rather than introduced silently. Per ADR-0019, a CLI change ships as part of a
  dotCMS release, not a standalone SDK publish. ADR-0019 alignment of the **image tag** is
  explicitly deferred — the bundled file still uses `latest`, so the drift the report flagged
  (`latest` + hardcoded `starter-20260630`) remains open.
- **Data considerations**: None. No migration, no repair of existing data. Users left with an
  empty directory by the old behavior simply re-run; the P1 port-reuse work is what makes that
  re-run possible without tearing down a healthy instance.

### Compose design decisions

**No `docker/` reviewers are required.** An earlier draft carried a `git blame`-derived reviewer
list for `single-node-demo-site/docker-compose.yml`, chosen back when this work modified that file.
The rescope means it does not: what ships is a new compose file inside
`core-web/libs/sdk/create-app/assets/`, reviewed as SDK code along with the rest of the CLI change.
There is no change to infrastructure anyone owns, so the list is dropped rather than carried as
courtesy CCs. (`.github/CODEOWNERS` covers neither path, so PR 2 draws no automatic reviewer
either way — worth knowing when requesting review.)

One piece of guidance from that analysis survives, because it is about the code and not about who
signs off: **the plan phase should read `lgtm-observability/docker-compose.yml` directly as the
reference for correct `condition: service_healthy` usage**, rather than assume the pattern was
copied faithfully. Its author is no longer a collaborator, so there is nobody to ask — the file is
the specification. Note it binds 8090 on the wildcard (L195), which this design deliberately does
not copy.

**All open questions are now settled.** Both questions this section previously left open have been
decided by the issue owner, and the decisions are recorded in Fix Scope. Kept here with their
reasoning so a reviewer can see what was chosen and object, rather than having to rediscover it:

1. **Gate `dotcms` on `opensearch: service_healthy`** — **decided: keep the stricter gate**, using
   `single-node-os-migration`'s proven probe (L61–65). The framing this question originally carried
   was wrong: it is not a clean break from precedent, because `single-node-os-migration` already
   gates `dotcms` on OpenSearch health transitively, via provision jobs. A credential-free probe was
   considered and rejected — the `admin:admin` coupling is contained by the major-version tag pin,
   and a proven probe beats an unproven one on the critical path. See Scope of Investigation.
2. **`start_period`** — **decided: `180s`**, ~4× the measured ~46s boot and above both precedents
   (lgtm 120s, metrics-monitoring 20s). Two earlier premises here were both wrong. The original
   question, whether `restart: unless-stopped` would flap the container, rests on a false premise:
   Compose restart policies react to container exit, not health status, so flapping cannot occur.
   Its replacement — that a too-short `start_period` makes `docker compose up --wait` block until
   timeout — is also wrong, and backwards: a too-short window marks the container `unhealthy` and
   makes `--wait` **abort early**, which is the worse failure. Because the first successful probe
   ends the window immediately, overshooting is free. See Regression Risk.
3. **Publishing 8090** — **decided: bind to `127.0.0.1`**, because the management port is authorized
   by arrival port with no credential check and no IP allowlist. Note this deviates from
   `lgtm-observability`, which binds the wildcard (L195); the deviation is deliberate.

What remains is not a decision but a **confirmation**: `/dotmgt/livez` must be
verified on the released `dotcms/dotcms:latest` image before the healthcheck can depend on it. Both
in-repo examples that probe it run `dotcms/dotcms-test`. This is the first step of the plan's
verification guide, and a failure there invalidates the compose design. See Assumptions.

## Acceptance & Verification *(mandatory)*

- **AC-001**: On a cold machine with no dotCMS containers, `npx @dotcms/create-app` on the
  local-Docker path brings the stack up **without manual intervention** — `dotcms` starts only
  after `db` and `opensearch` report healthy, and is restarted if it exits. Reproduction steps
  3 and 4 no longer occur.
- **AC-002**: The CLI's "containers started successfully" message is only printed when the
  containers are actually running and healthy.
- **AC-003**: When the UVE configuration call fails for any reason, the CLI prints a warning,
  **continues to scaffolding**, and exits 0 with a complete project. This holds on **both** entry
  paths — local Docker and existing instance (`--dotcms-url`) — because both go through the single
  `configureUVE()` owner, which contains no `process.exit`. A grep for `process.exit` in the UVE
  path returning nothing is part of this criterion. Reproduction step 5 no longer aborts the run.
  The warning includes the headless UVE configuration guide
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
  consecutive attempts over ~7 minutes all returned 403. On 403 the CLI skips the write; the
  message it then prints is **mode-dependent**. In `mode: 'local'` it reports the instance
  unrecoverable and to recreate it with `docker compose down -v`, and does **not** offer manual UVE
  setup steps, which would fail identically. In `mode: 'remote'` it reports instead that the API
  token lacks permission on the resolved site and links the manual steps, which on a user-supplied
  server will work — `docker compose down -v` must never be suggested there, as there is no stack
  to recreate.
- **AC-006** *(P1)*: With a healthy dotCMS already answering on 8082, a second run offers to
  reuse it instead of aborting with "Required ports are already in use". Reproduction step 6 no
  longer blocks.
- **AC-007** *(P1)*: A failed `npm install` causes the CLI to report failure — the branch at
  `src/index.ts:597` is reachable and correct.
- **AC-008** *(P1)*: If scaffolding fails after `moveDockerComposeOneLevelUp()`, the compose
  file is restored to the project directory (no orphan in the parent).
- **AC-009**: Feedback is **continuous for the entire wait**, which may be up to ten minutes
  (`--wait-timeout 600`). Both required: `docker compose up --wait`'s own per-container
  `Waiting → Healthy` transitions are streamed rather than swallowed, and a ticker shows elapsed
  time plus per-service state, refreshed every ~2s. Image-pull progress is visible. Retry messages
  do not interleave with an active `ora` spinner.
- **AC-010**: `docker/docker-compose-examples/*` is **unchanged** by this work — verified by diff,
  not by inspection. The CLI's README documents the bundled compose file, the loopback 8090 port,
  and the `DOTCMS_COMPOSE_URL` override.
- **AC-011**: Port 8090 is reachable on `127.0.0.1` and **not** on the host's LAN address —
  the management surface is not exposed to the network.
- **AC-012**: `npx @dotcms/create-app --starter <url>` still works against the bundled compose
  file — the `CUSTOM_STARTER_URL` rewrite regex in `updateDockerComposeStarterUrl` must still match.
  Enforced by **two guards, neither of which subsumes the other**:
  `core-web/libs/sdk/create-app/scripts/verify-cold-start.sh --static` asserts the **file** still
  matches the shape installed CLIs depend on (check T008), and a Jest spec runs
  `applyStarterUrl()` against the real bundled asset and asserts the **function's** output. The
  `--static` mode needs no Docker daemon and completes in well under a second, so both run on
  every PR. An earlier revision of this spec described that script as a phantom reference that
  nothing created; **that was wrong** — it exists, it ships in this package, and it was already
  written against the bundled asset. What was genuinely wrong was the path: it is under the
  package's own `scripts/`, not the repository root.
- **AC-013**: The bundled compose file is actually present in the published package. `package.json`
  `files` and `project.json`'s esbuild `assets` must both list it, or it ships missing and every
  local-Docker run fails at the first step.

- **Verification method**:
  - *Compose:* against **the bundled asset**,
    `docker compose -f core-web/libs/sdk/create-app/assets/docker-compose.yml up -d --wait --wait-timeout 600`
    from a cold state (no volumes, no cached images), asserting `dotcms` reaches healthy without
    manual start; plus a fault-injection run (`docker kill` the `dotcms` container) asserting it
    is restarted. **Not** the shared `single-node-demo-site` file — AC-010 requires that one to be
    unchanged, and it has no `dotcms` healthcheck for `--wait` to assess, so it cannot satisfy this
    assertion. (An earlier draft named it here; that was pre-rescope language.)
  - *CLI unit tests:* the package contains **no spec file**, but the Jest harness is already in
    place (`jest.config.ts`, `tsconfig.spec.json`, `@nx/jest/plugin`), so this fix adds specs
    rather than a harness. Run with `pnpm nx test sdk-create-app` — note `project.json` sets
    `passWithNoTests: true`, so an empty run reports green; confirm the new specs are actually
    collected before trusting a pass. Jest specs covering, at minimum: the `Result` truthiness fix;
    `configureUVE()` returning a non-fatal outcome instead of exiting, exercised for **both**
    `mode: 'local'` and `mode: 'remote'` so the two 403 messages are asserted separately; the
    read-before-write gate with a mocked 403-then-200 sequence; the `try/finally` compose move; the
    port-reuse probe; and the `CUSTOM_STARTER_URL` rewrite run against the real bundled asset
    (AC-012). Per constitution Principle V these are written and confirmed failing (Red) before the
    implementation lands.
  - *Manual end-to-end:* the reproduction steps above, run cold on macOS + Docker Desktop,
    verifying AC-001 through AC-006, plus AC-011 (a `curl` to the host's LAN address on 8090
    must be refused).
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
