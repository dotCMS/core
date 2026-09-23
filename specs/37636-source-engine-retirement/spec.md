# Issue Resolution Specification: Readiness report fails entirely when one search engine is unreachable

**Feature Branch**: `37636-source-engine-retirement`

**Created**: 2026-09-23

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: #37636

**Input**: User description: "https://github.com/dotCMS/core/issues/37636"

## Problem Statement *(mandatory)*

The migration readiness report (`GET /api/v1/index/migration/readiness`) is the operator's main
view of the Elasticsearch → OpenSearch migration: which content and Site Search indices exist on
each engine, how many documents each copy holds, and whether it is safe to advance a phase or roll
back. When either engine cannot be reached, the report does not degrade — it disappears. The
endpoint answers with only the connection error (`{"message":"elasticsearch: Name or service not
known"}`): no phase, no verdict, no rows for the engine that *is* reachable.

The runbook tells the operator to retire the old Elasticsearch cluster after a cooling-off period
at phase 3, and to keep reading this report in the closing checks and during troubleshooting. So
the instrument stops working at exactly the point the migration is meant to end, because of a
cluster the phase 3 report no longer depends on. The same happens in any phase whenever one engine
is down, which is precisely when an operator most needs to see the state of the other one.

**Severity / Impact**: Every operator running the migration, in every phase, whenever one engine is
unreachable — and deterministically for every operator who follows the runbook and switches the
old cluster off at phase 3. No data is lost; the harm is that the only migration-state view goes
dark, and the failure gives no hint that the report itself could still have answered for the other
engine.

### Scope change from the issue as filed

The issue reports a second defect: the deprecated `$estool.esSearch()` / `esRaw()` path failing
with a single, unsearchable log line (`ERROR DotRestHighLevelClientProvider$1 - [host=...]`).
Investigation on 2026-09-23 found that this does not hold on `main`:

- **At phase 3** — the issue's scenario — the path no longer reaches Elasticsearch at all. Since
  #37667 (merged 2026-09-23; the guard itself is commit `5d59ee16ef`, 2026-09-22),
  `ESSearchAPIImpl` throws a `DotStateException` on the phase, before any client call, with a
  message that names the vendor-neutral replacement. Switching the cluster off cannot produce a
  connection failure there. The lab run that found the issue (issue filed 2026-09-18) predates that
  guard.
- **In phases 0–2**, reproduced against an isolated stack with Elasticsearch stopped, the bare
  listener line does appear, but it is always immediately followed by a WARN that names the method,
  the exception and the cause, and carries the template position — on all three render paths
  tried:
  - page render through `VelocityServlet`: `WARN servlet.VelocityServlet - Invocation of method
    'esSearch' in class ...ESContentTool threw exception ...DotStateException: elasticsearch: Name
    or service not known at LIVE/...templatelayout[line 1, column 81]`, plus an `ERROR
    directive.Parse` line carrying URL, IP and user;
  - Page API `/api/v1/page/render`: the same message from `RuntimeExceptionMapper`, with the full
    stack trace;
  - `/api/vtl/dynamic`: the same WARN from `VTLResource`, and the error in the response body.

  Each of those lines matches the lab's own search (`esSearch`). Why the lab saw only the bare
  line is not established.

The deprecated-path half is therefore not treated as a defect here. What remains of it is the
listener line itself, which says nothing about what happened; improving its wording is in scope as
a small, separate item (see Fix Scope).

## Reproduction *(mandatory)*

**Environment**: dotCMS with both engines configured (`ES_ENDPOINTS`, `OS_ENDPOINTS`), any
migration phase; a user who is a CMS administrator and holds the migration support role (the role
named by `OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY`). Reproduced 2026-09-23 on the local
`dotcms/dotcms-test:1.0.0-SNAPSHOT` image at phase 0 with Postgres + Elasticsearch 7.10.2.

**Steps to Reproduce**:

1. Start dotCMS with both engines reachable and call `GET /api/v1/index/migration/readiness`
   as the migration support user — a full report is returned.
2. Stop one engine (e.g. `docker compose stop elasticsearch`). At phase 3 this is the runbook's
   "retire the old cluster" step.
3. Call `GET /api/v1/index/migration/readiness` again.

**Expected Behavior**: HTTP 200 with a usable report: the current phase, the rows for the reachable
engine populated, the unreachable engine explicitly marked unavailable (with the reason), and a
verdict that reflects only what could be measured — never a green verdict that depends on the
engine that could not be read.

**Actual Behavior**: HTTP 400, body `{"message":"elasticsearch: Name or service not known"}`. The
log shows the failure escaping the reconciler:

```
ESIndexAPI.getIndicesStats(ESIndexAPI.java:135)
  ContentIndexMirrorReconciler.statuses(...)
  MigrationReadinessService.evaluate(...)
  MigrationReadinessResource.readiness(...)
```

**Reproducibility**: Always, in any phase, whenever either engine is unreachable.

## Scope of Investigation *(mandatory)*

- **Affected area**: Search index migration — the readiness report (REST) and the two reconcilers
  that build its rows; secondarily, the Elasticsearch client's node-failure log line.
- **Suspected surface**: Modern — `com.dotcms.content.index.migration.*`
  (`ContentIndexMirrorReconciler`, `SiteSearchMirrorReconciler`, `MigrationReadinessService`,
  `MigrationReadiness`) and `com.dotcms.rest.api.v1.index.MigrationReadinessResource`. The listener
  lives in `com.dotcms.content.elasticsearch.util.DotRestHighLevelClientProvider`. No
  `com.dotmarketing.*` code is expected to change.
- **Related known decisions**: #37635 / #37667 established that the report must never claim a
  green verdict over zero measurements, that "could not read" is distinct from "empty"
  (`ContentMirrors.unreadableReason`), and that a failed per-index count is reported as `-1`
  (unmeasurable) rather than propagated. This fix extends the same rule to the engine-level calls.
  The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Both reconcilers make one engine-wide call per engine with no failure handling:

- `ContentIndexMirrorReconciler.statusesFor()` calls `esImpl.getIndicesStats()` and
  `osImpl.getIndicesStats()` unconditionally (`ContentIndexMirrorReconciler.java:148-149` on
  `main`).
- `SiteSearchMirrorReconciler` calls `esImpl.listIndices()` and `osImpl.listIndices()`
  unconditionally (`SiteSearchMirrorReconciler.java:56-57`).

Either call throws when its engine is unreachable, and nothing between it and the resource
catches it, so one engine's outage aborts the whole `MigrationReadinessService.evaluate()`. The
per-index document counts already degrade (`countQuietly` returns `-1`), which is why only these
engine-level calls take the report down.

The reproduction ran on an image built before #37667 (its stack trace shows the pre-#37667 line
numbers). On `main` the unconditional calls are unchanged, so the same failure is expected, but it
has not been run against `main`; the regression tests below will confirm it.

The listener half: `DotRestHighLevelClientProvider`'s `RestClient.FailureListener.onFailure(Node)`
logs only `node.toString()`. The exception is not available in that callback — it travels to the
caller — so the line can say *what* happened (a node failed a request) but not *why*.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- The readiness report answers (HTTP 200) when either engine is unreachable, in every phase.
- Each engine's side of the report is marked unavailable, with the failure reason, when that
  engine could not be read — distinct from "read and found nothing".
- The verdict reflects the partial view, and the reason appears in `blockers` or the summary:
  - `safeToRollback` compares the two engines, so it is `false` whenever either one could not be
    read.
  - `safeToAdvance` is `false` whenever either engine could not be read in phases 0–2 (every
    next phase writes to both). At phase 3 there is no next phase and nothing depends on
    Elasticsearch, so an unreachable Elasticsearch is reported but does not by itself make it
    `false`; an unreachable OpenSearch does (the active content store could not be measured).
- Both reconcilers — content and Site Search — degrade the same way.
- The Elasticsearch client's node-failure log line states what happened in words an operator would
  search for (e.g. that an Elasticsearch node failed a request and was marked dead), alongside the
  host it already prints.

**Explicitly out of scope / non-goals**:

- Any change to the deprecated `esSearch()` / `esRaw()` path or to Velocity's exception handling:
  it already logs a descriptive WARN (see Scope change) and fails clearly at phase 3.
- Probing engine health or permissions beyond what the report already reads (the separate
  permission-probe idea stays separate).
- Deleting or cleaning up the retired Elasticsearch index at phase 3 (known gap left by #37667).
- The readiness report's wording contradictions (#37638) and `PhaseRouter` misclassifying a
  malformed query as an availability failure (#37637).
- Changing how phases advance or any startup gate.

## Regression Risk *(mandatory)*

- **Blast radius**: The readiness endpoint and anything that consumes `MigrationReadinessService`
  (the resource only, today). The listener change touches every Elasticsearch client failure log
  line, but only its text.
- **Backward compatibility**: The response model gains an "engine unavailable" signal. It must be
  additive — existing fields keep their names and meaning when both engines are reachable, so
  current consumers (support scripts, the tester guide's examples) keep working. `@Schema` and the
  regenerated `openapi.yaml` must match. Anyone grepping the old bare listener line keeps matching
  on the `[host=...]` part if it is preserved.
- **Data considerations**: None. The report is read-only.

## Acceptance & Verification *(mandatory)*

- **AC-001**: With Elasticsearch unreachable, `GET /api/v1/index/migration/readiness` returns 200
  with the current phase, the OpenSearch rows populated, and the Elasticsearch side marked
  unavailable with its reason — in every phase.
- **AC-002**: With OpenSearch unreachable, the same holds with the roles reversed.
- **AC-003**: In phases 0–2, with either engine unreachable, both `safeToAdvance` and
  `safeToRollback` are `false` and the reason names the engine; the report never returns a green
  verdict that depends on an engine it could not read.
- **AC-004**: At phase 3 with Elasticsearch unreachable, the report still shows the OpenSearch
  content and Site Search rows, `safeToRollback` is `false` with a reason naming Elasticsearch, and
  the unreachable Elasticsearch does not by itself make `safeToAdvance` false. With OpenSearch
  unreachable at phase 3, `safeToAdvance` is `false` with a reason naming OpenSearch.
- **AC-005**: A failure listing Site Search indices on one engine degrades the Site Search rows the
  same way and does not take down the content rows, and a failure reading content index stats does
  not take down the Site Search rows.
- **AC-006**: With both engines reachable, the report is unchanged (existing tests pass untouched).
- **AC-007**: The node-failure log line contains a descriptive phrase an operator would search for,
  plus the host.
- **Verification method**:
  - Unit, `ContentIndexMirrorReconcilerTest` / `SiteSearchMirrorReconcilerTest`: the engine mock's
    `getIndicesStats()` / `listIndices()` throws → rows for the other engine are still produced and
    the failing engine is flagged unreadable with its reason.
  - Unit, `MigrationReadinessServiceTest`: per phase (0–3), each engine unreachable → verdict,
    blockers and summary as in AC-003/AC-004.
  - Unit, `MigrationReadinessResourceTest`: an unreachable engine yields 200, not an error.
  - Manual, against a running stack: stop one engine and call the endpoint (the reproduction
    above), on `main`, at phase 0 and at phase 3.

## Assumptions

- "Unreachable" covers any failure of the engine-level call (connection refused, unknown host,
  timeout, auth failure); the report does not need to distinguish them beyond carrying the message.
- The deprecated-path observation (descriptive WARN on all three render paths) generalises to the
  lab's setup; the lab's missing WARN is assumed to be a search-window or render-path artifact, not
  a code path that swallows the exception. If a path that does swallow it is found later, it gets
  its own issue.
