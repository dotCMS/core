# Issue Resolution Specification: Phase 2 ES read fallback never fires for content search

**Feature Branch**: `37413-phase2-read-fallback`

**Created**: 2026-09-07

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: #37413

**Input**: User description: "37413"

## Problem Statement *(mandatory)*

The ES→OpenSearch migration design promises an automatic **read fallback to Elasticsearch in
Phase 2**: OpenSearch serves reads, Elasticsearch is still live and in sync, and if OpenSearch
throws on a read the error is logged at `ERROR` and the read is retried against Elasticsearch.
That fallback is implemented, but the **content read path never reaches it**, so it never fires.

With OpenSearch unavailable in Phase 2 and Elasticsearch up and holding a complete copy of the
data, `POST /api/content/_search` does not fall back. It returns one of two things:

- **HTTP 200 with `resultsSize: 0`** — a well-formed empty result. A caller cannot tell this
  apart from "this content type has no content", so a live site renders as **missing content**
  with no error surfaced anywhere and nothing for 5xx monitoring to catch.
- **HTTP 500** — for content types whose count was not already cached.

**Severity / Impact**: High. Every customer running Phase 2 loses the documented safety net —
a transient OpenSearch blip, a slow node, or a missing OpenSearch index takes down content
delivery instead of degrading to Elasticsearch. The silent-empty variant is the more dangerous
of the two. Beyond the outage case, this also blocks recommending Phase 2 to customers at all:
Cloud and Support are currently being told "Phase 2 is safe, OpenSearch failures fall back to
Elasticsearch", and for content search that is false today.

**This is not a regression.** With its read engine down dotCMS has always behaved this way —
Phase 0 with Elasticsearch down does the same thing through the same legacy code. What is
broken is the *promise*, not previously-working behavior. For content search the fallback
wiring was never written.

## Reproduction *(mandatory)*

**Environment**: `main` @ `788795e915` (2026-09-03). Local instance in Phase 2
(`DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE=2`), Elasticsearch 7.10.2 and OpenSearch 3.4.0, real
content indexed and in sync on both engines.

**Steps to Reproduce**:

1. Confirm the live phase from `GET /api/v1/jvm` (`DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE` = `2`)
   and that `GET /api/v1/index/migration/readiness` reports `readEngine: OpenSearch`.
2. Stop the OpenSearch node (stop the container or block its port). Leave Elasticsearch running
   and healthy.
3. Call `POST /api/content/_search` for several content types that **do** have live content:
   `{"query":"+contentType:Profile +live:true","limit":7,"offset":1}`
4. **Vary `offset` on every call.** An identical request body is served from the query cache and
   returns the pre-outage result, which looks exactly like a working fallback. Only novel,
   uncached queries expose the defect — this is how it was nearly missed in QA.
5. Observe the responses, then restart OpenSearch and repeat to confirm the content was present
   the whole time.

**Expected Behavior**: Every query returns the result set Elasticsearch holds, and each fallback
is logged once at `ERROR` naming the failing OpenSearch operation and its cause.

**Actual Behavior**:

| Content type | Actual with OpenSearch down | Same call, OpenSearch up |
|---|---|---|
| Profile | **200, `total=0`** | 200, `total=184` |
| Testimonials | **200, `total=0`** | 200, `total=317` |
| JobPosting | **200, `total=0`** | 200, `total=27` |
| Hero | **500** | 200, `total=215` |
| ContactCard | **500** | 200, `total=106` |

Failures are logged at **`WARN`**, not the `ERROR` the design specifies, so the "early-warning
signal per read" is weaker than documented.

**Reproducibility**: Always, on demand — provided the query is not already in the query cache
(see step 4).

## Scope of Investigation *(mandatory)*

- **Affected area**: Content search / delivery read path (`/api/content/_search`, `ContentletAPI`
  index reads, Velocity `$dotcontent.pull`), under the ES→OpenSearch migration phase routing.
- **Suspected surface**: **Both.** The routing gap is in modern code
  (`com.dotcms.content.elasticsearch.business.ESContentFactoryImpl`,
  `com.dotcms.content.index.PhaseRouter`), but the reason the failure becomes an empty `200` is
  legacy swallow behavior in `com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils`
  and `com.dotcms.rest.ContentHelper`.
- **Related known decisions**: `docs/backend/OPENSEARCH_MIGRATION.md` lines 105, 402, 676 and 901
  all state the Phase 2 read fallback as designed behavior — the document is the contract being
  violated. Phase 3 must keep propagating read failures (no fallback; Elasticsearch is
  decommissioned). The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Two independent layers. Both were verified in the current tree.

### Layer 1 — the router is never called (the actual defect)

`ESContentFactoryImpl.java:273` picks an engine with a bare ternary and calls the chosen
provider **directly**:

```java
ContentFactoryIndexOperations indexOperationsDelegate(){
    return isMigrationComplete() || isReadEnabled() ? indexOperationsOS : indexOperationsES ;
}
```

Its five read call sites — `:1352` `search`, `:1607` `indexCount`, `:1616` `searchHits`,
`:1634` `indexSearchScroll`, `:1669` `createScrollQuery` — all invoke the selected provider with
no router in between. `PhaseRouter.read` (`PhaseRouter.java:187-199`) and `readChecked`
(`:298-310`) implement the fallback correctly and are simply unreachable from this path; the
class contains no reference to `PhaseRouter` at all.

The stack proves it by omission — no `PhaseRouter` frame between
`ESContentFactoryImpl.indexCount` and `ContentFactoryIndexOperationsOS`. The same outage through
`IndexAPIImpl` (the health check) *does* carry `PhaseRouter.read(PhaseRouter.java:192)`. Same
failure, two paths, one routed.

### Layer 2 — why the same outage yields 200/total=0 for some types and 500 for others

> This supersedes the explanation in the GitHub issue body, which attributes the `200/total=0`
> to the `OpenSearchException → ERROR_HIT` branch. That is **wrong** for the observed case: a
> `ConnectException` is not an `OpenSearchException`, so it never reaches that branch. The
> issue body needs correcting.

1. `ContentHelper.pullContent:310` runs the **count first**. A `CountRequest` carries no
   offset/limit, so varying the offset does not change the count cache key — content types
   queried before the outage had a cached count and did not throw; types not previously queried
   (Hero, ContactCard) missed the cache, the count threw, → **500**.
2. `ContentUtils.java:302` has a `catch (Throwable)` that logs a one-line-truncated `WARN` and
   returns an empty list, swallowing the search failure entirely.
3. `ContentHelper.java:318` — `if (contentlets.isEmpty() && offset <= resultsSize) { resultsSize = 0; }`
   overwrites the real cached count with `0` → **200 / `total=0`**.

The `ERROR_HIT` branch (`ContentFactoryIndexOperationsOS.java:109-121`) is nonetheless a real
second-order hazard: it converts an `OpenSearchException` into a legitimate-looking empty result,
leaving the router nothing to catch. It is **inherited Elasticsearch behavior**, structurally
identical in `ContentFactoryIndexOperationsES.java:56` and `:151-161` — so any change to it must
be scoped carefully. Cache poisoning is bounded: `shouldQueryCache(exceptionMsg)` only caches
`ERROR_HIT` for `parse_exception` / `search_phase_execution_exception`, so a connection failure
is not cached.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Route the five read call sites of `ESContentFactoryImpl.indexOperationsDelegate()` through
  `PhaseRouter.read` / `readChecked`, following the router pattern already documented in
  `PhaseRouter`'s class Javadoc and used by `IndexAPIImpl`.
- Ensure each Phase 2 fallback occurrence is logged at `ERROR` (the router already does this;
  confirm no `WARN`-only path remains upstream of it).
- Make the OpenSearch read path in Phase 2 surface a *throwable* failure to the router rather
  than an empty result, for the failure classes the router is meant to catch — including a
  missing OpenSearch counterpart index.
- Tests: Phase 2 falls back and logs `ERROR`; Phase 3 propagates; the query cache does not mask
  a live failure.

**Explicitly out of scope / non-goals**:

- **No change to Elasticsearch behavior.** `ContentFactoryIndexOperationsES` keeps its current
  `ERROR_HIT` semantics; Phase 0 and Phase 1 read behavior is untouched.
- **No fallback in Phase 3.** Failures must keep propagating there by design.
- **No broad rewrite of the legacy swallow.** `ContentUtils.java:302`'s `catch (Throwable)` and
  `ContentHelper.java:318`'s `resultsSize = 0` overwrite are pre-existing and affect all phases,
  including pure Elasticsearch. They explain the *symptom*, and fixing Layer 1 removes the
  Phase 2 case. Changing them repo-wide is a separate, larger issue — file it, do not fold it in.
- **No `IndexAPI<F>` generic parameterization** — that belongs in its own PR.
- No write-path changes. Write failure semantics per phase are settled elsewhere.
- No new configuration property or feature flag to switch the fallback on and off — it is
  documented, unconditional Phase 2 behavior.

## Regression Risk *(mandatory)*

- **Blast radius**: `ESContentFactoryImpl`'s five read call sites are the funnel for essentially
  all content search — `/api/content/_search`, `ContentletAPI.search`/`searchIndex`/`indexCount`,
  Velocity `$dotcontent.pull`, URL maps, Site Search, scroll/pagination consumers, and the
  admin UI content browser. Any behavior change here is felt site-wide in every phase, so the
  routing change must be a pure pass-through in phases 0, 1 and 3.
- **Callers that depend on empty-instead-of-exception**: making the OpenSearch read throw where
  it currently returns `ERROR_HIT` changes the contract for anything that today receives an
  empty result. These callers must be enumerated before that change lands, and each confirmed
  either unaffected or explicitly handled. If the enumeration turns out to be large, the routing
  fix (Layer 1) still stands on its own and can ship without touching `ERROR_HIT`.
- **Scroll and pagination**: `indexSearchScroll` and `createScrollQuery` hold engine-specific
  cursor state. A mid-scroll fallback cannot resume an OpenSearch scroll on Elasticsearch — the
  plan must decide whether these two sites fall back at all or only propagate, and say so.
- **Backward compatibility**: no API contract, response shape, serialized state, DB schema or
  index mapping changes. Not rollback-unsafe.
- **Data considerations**: none — no data repair needed. Content missing from a read during an
  outage was never lost; it is in Elasticsearch the whole time.
- **Performance**: a fallback doubles the work for a failing read. Bounded to the outage window,
  and the failing engine fails fast, but the plan should confirm no retry storm (one fallback
  attempt per read, no nesting).

## Acceptance & Verification *(mandatory)*

- **AC-001**: The reproduction above produces the expected behavior — in Phase 2 with
  OpenSearch stopped and Elasticsearch healthy, `POST /api/content/_search` returns the same
  non-zero result set Elasticsearch holds for **every** content type that has live content. No
  `total=0`, no 500.
- **AC-002**: All five read call sites of `indexOperationsDelegate()` (`:1352`, `:1607`, `:1616`,
  `:1634`, `:1669`) go through `PhaseRouter.read` / `readChecked` instead of invoking the
  selected provider directly — or, for a site the plan excludes (see scroll/pagination above),
  the exclusion is documented in code with its reason.
- **AC-003**: Each fallback occurrence is logged at `ERROR`, once per read, naming the failing
  OpenSearch operation and the cause — so an outage is visible to log-based monitoring per the
  design's early-warning signal. No `WARN`-only swallow remains on the Phase 2 fallback path.
- **AC-004**: Phase 0, 1 and 3 read behavior is byte-for-byte unchanged: 0/1 read from
  Elasticsearch with no fallback; 3 reads from OpenSearch and propagates failures. Verified by
  test, not by inspection.
- **AC-005**: A missing OpenSearch counterpart index in Phase 2 is served from Elasticsearch
  rather than returning empty, matching `OPENSEARCH_MIGRATION.md:402` — this also covers the
  reactivated-backup-index scenario.
- **AC-006**: If the `OpenSearchException`/`ERROR_HIT` branch is changed, Elasticsearch's
  equivalent branch (`ContentFactoryIndexOperationsES.java:151-161`) is **not** changed, Phase 3
  still propagates, and the enumeration of callers relying on the empty result is recorded in
  the plan with each one confirmed unaffected.
- **AC-007**: A repeated identical query does not mask a live OpenSearch failure by serving a
  stale pre-outage result from the query cache — the caller either gets fallback results or a
  surfaced error, never a silent stale hit presented as current.

**Verification method**:

- Integration test — Phase 2 with the OpenSearch provider stubbed to throw: a content search
  returns the Elasticsearch result set and logs at `ERROR`.
- Integration test — the same stubbed failure in Phase 3 propagates and does **not** fall back.
- Integration test — phases 0/1 unchanged (AC-004).
- Integration test — the query-cache path (AC-007).
- All of the above live in `dotcms-integration`, are named `*Test` (not `*IT`), and are
  **registered in the matching `@SuiteClasses` suite** — an unregistered class compiles, passes
  CI green, and never runs.
- Run: `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=<TestClass>`
  (requires the WAR to be installed first, not just compiled).
- Manual re-run of the reproduction on the Phase 2 real-data rig, varying `offset` per call.

## Assumptions

- The intended behavior is what `docs/backend/OPENSEARCH_MIGRATION.md` states; no ADR overrides
  it. The plan will confirm against `dotCMS/platform-adrs`.
- Elasticsearch being in sync in Phase 2 is a given — Phase 2 is dual-write, so ES is
  continuously written. A fallback returning stale-but-present ES data is strictly better than
  returning nothing.
- The scroll/pagination question (AC-002 exclusion) is a plan-phase decision, not a
  specification gap: either answer satisfies the design as long as it is deliberate and
  documented.
- The GitHub issue body will be corrected to match Layer 2 above before implementation, so the
  issue and this spec do not disagree on root cause.
