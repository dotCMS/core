# Feature Specification: Bound user-cache eviction cost during folder listings

**Feature Branch**: `37148-drive-listing-user-cache`

**Created**: 2026-08-24

**Status**: Draft

**Type**: Defect / performance fix — issue [#37186](https://github.com/dotCMS/core/issues/37186). Parent epic: [#36814](https://github.com/dotCMS/core/issues/36814). Originally investigated as item 4 of [#37148](https://github.com/dotCMS/core/issues/37148).

**Input**: User description: "Issue #37148 item 4 ONLY — undersized user cache during folder listings in Content Drive / Site Browser (BrowserAPIImpl). Confirmed diagnosis: `cache.userdotcmscache.size` is commented out, falling back to `cache.default.size=1000`; the real instance has 1828 active users, more than cache capacity. Cache constantly evicts during listing navigation, and each eviction costs up to 8 DB round trips (not 1), because the same modUser/owner gets resolved multiple times per row. Confirmed empirically: raising the size to 4000 dropped `user_` lookups per request from ~688 avg to 1-2. Ruled out: NOT missing negative-caching, NOT a classic per-row N+1. Scope: (1) make the cache size reflect real user count instead of a fixed default, (2) optionally remove redundant same-user resolutions within one row. Do not spec items 1, 2, 3 of the issue. Related but out of scope: BrowserAPIImpl transforms all subfolders before paging (wastes owner resolution); two filterCollection calls use the non-batch overload. Open question, not blocking: latency stays bimodal even after the cache fix, cause unidentified."

---

## Scope Boundary

This spec covers **item 4 of issue #37148 and nothing else**. Items 1 (candidate-scan query
plan instability), 2 (field-filter chunk multiplier) and 3 (listing projection) are explicitly
out of scope.

Two earlier hypotheses for item 4 were investigated and **ruled out** before this spec was
written:
- Batching a per-row N+1 lookup — there is no such access pattern.
- Missing negative-caching for nonexistent users — zero nonexistent users were found in the
  **test dataset used**, and zero lookup-failure markers appeared in the response. This rules
  out negative caching as *this dataset's* bottleneck; it is not a claim that no instance could
  ever have nonexistent-user lookups.

**Root cause — revised.** The original diagnosis attributed the lookup volume to simple cache
undersizing (1,000-entry default vs. 1,828 active users) amplified by redundant same-user
resolution within a single row ("8x amplification"). Review (2026-08-28) found this mechanism
doesn't hold: `UserFactoryImpl.loadUserById` caches on a miss (`UserCache.add`), so a *second*
resolution of the same user within the same row hits the cache, not the database — it cannot by
itself produce extra round trips. The confirmed root cause is instead a **concurrent thundering
herd**: `hydrateContentletsInParallel` (`BrowserAPIImpl`) fans a page of rows out into ~4
parallel chunks, and `UserFactoryImpl.loadUserById` (`dotCMS/src/main/java/com/dotmarketing/
business/UserFactoryImpl.java:104-122`) has no in-flight de-duplication — if the same
uncached `modUser`/`owner` id is needed by rows in two different chunks running at the same
time, each chunk independently misses the cache and queries the database for it. This matches
the observed lookup counts (363-1025 per request) better than the simple undersizing model
(~120-160 expected). Cache capacity still matters (a too-small cache evicts recently-warmed
entries back into contention), but it is now the secondary fix — see FR-001/FR-002.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A single listing request doesn't race itself for the same user record (Priority: P1)

An administrator browses a folder listing (Content Drive or Site Browser) whose rows are
authored/owned by many distinct users. Today, rendering one page of results can fan out ~4
parallel chunks that each independently resolve modifying-user and owner ids; when two chunks
need the same not-yet-cached user at the same moment, both query the database for it. The fix
resolves every distinct user id a page needs **once, sequentially, before** parallel row
rendering starts, so the parallel chunks always hit an already-warm cache instead of racing
each other.

**Why this priority**: This is the confirmed root cause. Without it, raising the cache's
capacity alone does not stop concurrent chunks from racing on the same still-uncached id.

**Independent Test**: On a folder whose rows span many distinct, not-yet-cached authors, issue
one listing request and count `user_`-by-id database lookups. Before the fix, count should
scale with concurrent chunk contention (can exceed the number of distinct users involved).
After the fix, count should equal exactly the number of distinct user ids the page needs — no
more, regardless of parallel chunk count.

**Acceptance Scenarios**:

1. **Given** a folder whose content spans N distinct, not-yet-cached authors, **When** an
   administrator opens that folder, **Then** exactly N user-record database lookups occur — one
   per distinct id — not more, regardless of how many parallel hydration chunks the page is
   split into.
2. **Given** the fix is applied, **When** the same listing request is repeated immediately
   after a first call, **Then** the second call issues zero user-record database lookups (all
   cache hits, since the warm-up already resolved every id on the first call and nothing
   evicted it).
3. **Given** the fix is applied, **When** compared against the pre-fix baseline on the same
   dataset and folder, **Then** the returned rows and their `modUserName`/`ownerUserName`
   values are unchanged — only the lookup volume and its timing (sequential warm-up before
   parallel hydration) change, not the results.

---

### User Story 2 - No repeated resolution of the same user within one row (Priority: P3)

While rendering a single listing row, the system resolves the content's modifying user and
owner. Today the same user id can be resolved multiple times for the same row through
different code paths (audit properties, version properties, URL building). Once a user id is
cached (via User Story 1's warm-up), each repeat resolution within a row is a cache hit, not a
database round trip — so this reduces call-count/CPU overhead only, not the lookup volume
counted by SC-001.

**Why this priority**: Optional hygiene, not a performance fix. The original hypothesis — that
this redundancy amplified the cost of each cache miss — did not hold up (see Scope Boundary):
`loadUserById` caches on first resolution, so a same-row repeat already hits the cache today.
This story is downgraded from the co-primary fix it was originally scoped as.

**Independent Test**: For a single listing row of a content type that carries both a modifying
user and an owner, count how many times the same user id is resolved while rendering that one
row; verify the count does not increase relative to today's behavior and, where feasible, is
reduced.

**Acceptance Scenarios**:

1. **Given** a single listing row whose content has both a `modUser` and an `owner` value,
   **When** the row is rendered, **Then** each distinct user id involved is resolved no more
   times than necessary to populate the fields that reference it.
2. **Given** the change is applied, **When** the same row is rendered before and after,
   **Then** the rendered `modUserName`, `ownerUserName`, and any locked-by user name are
   identical.

---

### User Story 3 - A listing doesn't fail entirely because of one orphan user reference (Priority: P2)

A folder contains content whose modifying-user id no longer exists in `user_` (a deleted or
otherwise orphaned reference). Today, `DefaultTransformStrategy.addVersionProperties` calls
`loadUserById` for that id without a fallback; the resulting `NoSuchUserException` (unchecked)
propagates uncaught through row hydration and fails the **entire** listing request, not just
that one row. The fix wraps this resolution the same way nearby code already does (`Try.of(...)`
with a `NOT_APPLICABLE` fallback), so a single orphan reference degrades gracefully instead of
taking down the whole page.

**Why this priority**: Confirmed availability bug found during this item's investigation, not
originally in scope of #37148 item 4 — included here because it lives in the same
user-resolution code path this item already touches, and leaving it unfixed means the warm-up
in User Story 1 would itself throw on the first orphan id it warms.

**Independent Test**: Construct a listing row whose `modUser` (or locked-by user) id does not
exist in `user_`. Confirm the listing request succeeds and that row's user-derived fields show
the existing fallback label, instead of the whole request failing.

**Acceptance Scenarios**:

1. **Given** a folder containing one row with an orphan `modUser` id, **When** the folder is
   listed, **Then** the request succeeds, that row shows `NOT_APPLICABLE` (or the existing
   fallback label) for the affected field, and every other row renders normally.
2. **Given** a folder containing one row with an orphan locked-by user id, **When** the folder
   is listed, **Then** the request succeeds and that row's `lockedBy` field is omitted or
   falls back, rather than throwing.

---

### Edge Cases

- What happens when a listing folder contains content whose author/owner user id does not
  exist in `user_` (a genuinely missing user, distinct from a cache miss)? This is **not**
  preserved as-is — it is a confirmed availability bug fixed by User Story 3: today it fails the
  whole request; after the fix, that row falls back to the existing "N/A" label and the rest of
  the listing renders normally.
- What happens on the very first listing request after a fresh startup, before any user is
  cached? The warm-up pass (User Story 1) itself constitutes this request's "first resolution" —
  its cost is expected and is not a regression; only steady-state repeat-request behavior is
  measured by SC-001.
- What happens if the instance's active user count grows over time past whatever cache capacity
  is configured? With the FR-002 resolution (a generous static default, not auto-sizing), this
  residual risk is not eliminated, only pushed far higher — an instance whose user count
  eventually exceeds the new default will see the warm-up's own cache entries evicted mid-page
  under concurrent load, reproducing a milder version of today's problem at a new threshold.
  Accepted as a known limitation of the chosen approach; an operator with an unusually large user
  base can still raise `cache.userdotcmscache.size` manually, same as today.
- **What happens when two different users browse the same cold folder at the same time?**
  FR-001's warm-up only de-duplicates lookups *within* one request — it does nothing for two
  concurrent requests racing on the same not-yet-cached user id. This is a narrower version of
  the same thundering-herd mechanism, explicitly **not** closed by this item. **Required
  follow-up, not optional**: a GitHub issue for a striped/keyed lock around `loadUserById`'s
  DB-query branch (e.g. one lock per user id, so concurrent callers for the *same* id block on
  each other instead of all querying the database, while callers for *different* ids remain
  unblocked) MUST be filed before this item is considered closed. This was raised once already
  during this item's own review and dropped between drafts. Filed as
  [#37335](https://github.com/dotCMS/core/issues/37335).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001 — RESOLVED (2026-08-31, revised): warm-up resolution is the primary fix.**
  Before `hydrateContentletsInParallel` fans a page's rows out into parallel chunks, the system
  MUST collect the distinct set of `modUser`/`owner` (and, where resolved, locked-by) user ids
  the page needs and resolve each one exactly once, sequentially, so every id is already
  cache-warm by the time parallel row hydration starts. This directly removes the concurrent
  thundering-herd race identified in Scope Boundary, without changing `UserCache`/
  `UserFactoryImpl`'s locking semantics — the fix is localized to the Content Drive/Site Browser
  listing path (`BrowserAPIImpl`), not the shared user-cache infrastructure used elsewhere in
  the product.
- **FR-002 — secondary hygiene, downgraded from primary.** The user cache capacity
  (`cache.userdotcmscache.size`, `dotmarketing-config.properties:518`) remains an
  operator-configured value, but its shipped default SHOULD change from its current
  effectively-1,000 fallback to a value with real headroom over a typical instance's active
  user count (reference point: 4,000, tested against 1,828 active users). This is no longer the
  primary fix for the observed lookup volume — FR-001 is — but still guards against eviction
  churn across a browsing session as the warm-up cache fills up with more distinct users than
  fit in 1,000 entries. The commented-out sibling default at line 519
  (`cache.useremaildotcmscache.size`) MUST NOT be raised alongside it: `UserCacheImpl.get()`
  looks up that region using the primary-group-prefixed key, never the raw email key
  `UserCacheImpl.add()` writes it under, so entries in that region are never read back by id —
  raising its size would reserve memory for a dead cache region. Fixing that key mismatch, if
  ever done, is a separate concern, not part of this item.
- **FR-003**: The system MUST continue to return identical listing content (rows, user display
  names, and fallback labels for missing users) after this change, **except** the orphan-user
  case fixed by FR-004a, which is an intentional correctness fix, not a preserved behavior.
- **FR-004**: Within a single row transformation, the system SHOULD avoid resolving the same
  user id more times than the number of distinct output fields that require a name for that
  id. This is optional hygiene (reduces call count / minor CPU overhead only, since a same-row
  repeat is already a cache hit post-warm-up) — a "not worth doing" outcome is acceptable if
  FR-001 already resolves the bulk of the observed lookup volume.
- **FR-004a**: `DefaultTransformStrategy.addVersionProperties` (and any other unwrapped
  `loadUserById` call in the same listing-rendering path, e.g. the locked-by resolution) MUST
  wrap user resolution the same way the nearby audit-properties code already does
  (`Try.of(...).getOrNull()` with a `NOT_APPLICABLE`/existing fallback), so a single orphan
  `modUser`/locked-by id degrades that one row gracefully instead of throwing an uncaught
  `NoSuchUserException` that fails the entire listing request.
- **FR-005**: The fix MUST NOT change the folder-listing permission-filtering behavior for
  either an administrator or a permission-restricted user — this item touches user *display
  name* resolution, not access control, and must not blur that boundary.


### Key Entities

- **User cache entry**: A cached mapping from user id to a resolved user record (used to
  produce `modUserName` / `ownerUserName` / locked-by display names in listing rows and
  folders), currently capacity-bounded by a single shared configuration value.
- **Listing row**: One content item or folder returned by a folder-listing request; may
  reference up to a small number of distinct user ids (modifying user, owner, locking user)
  that each require a resolved display name.
- **Warm-up set**: The distinct set of user ids a page of listing rows needs, computed once per
  request before parallel hydration begins, and resolved sequentially so every id is cache-warm
  before any parallel chunk needs it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 — mechanism named (2026-09-01, per review): no existing harness counts DB calls,
  so one must be built.** For a folder listing whose content spans N distinct, not-yet-cached
  authors, a single request issues exactly N user-record database lookups, down from the
  previously observed 363-1025 per request on the reference dataset. Verified via a new,
  always-on counter in `UserFactoryImpl.loadUserById`'s database-query branch (the `if
  (list.isEmpty())... else { user = ...; userCache.add(...) }` path at
  `UserFactoryImpl.java:104-122`): a `@VisibleForTesting` static `AtomicLong` incremented once
  per DB round trip, with a package-visible reset/read accessor. This is genuinely new test
  infrastructure — confirmed by searching `dotcms-integration` for any existing query-count
  assertion pattern (P6Spy, a counting `DataSource`/JDBC proxy, or Guava/Caffeine cache-stats
  based counting); none exists. Cache-stats-based counting was considered and rejected:
  `GuavaCache` builds its caches without `.recordStats()` (`GuavaCache.java:269-275`), so
  `CacheStats.REGION_LOAD` (Guava's `loadCount()`) always reports 0 today — enabling it would be
  a separate, unscoped change to cache behavior. The counter MUST be written, developer-approved,
  and its test confirmed red before any implementation change, per Constitution Principle V —
  this is not a parenthetical detail to leave to the plan phase.
- **SC-002**: Listing content (returned rows, display names, and fallback labels, including the
  orphan-user fallback from FR-004a) is verified unchanged before and after the fix on the same
  dataset and folder, except the orphan-user case itself, which changes from "request fails" to
  "row falls back."
- **SC-003**: For a typical/expected instance size, the fix requires no per-instance manual
  tuning step for the default behavior to be adequate — an operator should not need to hand-pick
  a cache size out of the box. This does not extend to instances whose active user count exceeds
  the new default with no margin left (see Edge Cases); those retain the existing manual
  `cache.userdotcmscache.size` override as today.
- **SC-004**: The new cache default's memory footprint is documented (approximate size of one
  cached `User` entry × the new default entry count) so the plan phase can confirm it's an
  acceptable trade-off, not an unbounded increase — this was not previously quantified.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The primary fix (FR-001, warm-up) is localized to
  `BrowserAPIImpl`'s Content Drive/Site Browser listing path — it does not change
  `UserCacheImpl`/`UserFactoryImpl.loadUserById`'s locking or caching semantics, so it carries no
  blast radius beyond these two listing endpoints. FR-002 (cache default) and FR-004a (orphan-id
  fallback) do touch the shared `UserCacheImpl`/`UserFactoryImpl`/`DefaultTransformStrategy`
  code paths used elsewhere in the product, so those two changes carry the wider blast radius a
  shared cache/transform change implies, even though the observed defect is Content-Drive-scoped.
- **Backward-compatibility expectations**: No API, admin workflow, or content behavior changes,
  except the intentional orphan-user fallback fix (FR-004a), which changes "request throws" to
  "row falls back" — a correctness improvement, not a behavior change consumers should have been
  relying on.
- **Known related decisions**: None identified specific to user-cache sizing. Related, but
  explicitly out of scope for this item: the subfolder-transform-before-paging inefficiency in
  `BrowserAPIImpl` and the two non-batch `filterCollection` call sites flagged in issue
  #37148's investigation comments — both are separate, unaddressed findings that happen to
  live near this code, not this item's concern. Sequencing note: #37148 asked that item 1
  (candidate-scan query plan instability, spec'd in #37230) close first before items 2-4 are
  re-evaluated; #37230 is still open as of this writing, so this item's plan phase should
  re-confirm its baseline measurements are still representative once #37230 lands.

## Assumptions

- The reference measurement (363-1025 lookups/request, expected ~120-160) was taken on a single
  test instance with 1,828 active users under concurrent parallel-chunk hydration. Per FR-002's
  resolution, the cache default is a fixed value chosen with headroom over that reference point,
  not derived per-deployment; deployments with meaningfully larger active user populations than
  the chosen default may still need a manual override, same as today.
- This item's win scales with the number of distinct authors/owners in a folder and with
  concurrent chunk contention — a folder with few distinct authors, or a single-chunk (small)
  page, sees a smaller win than a folder with many distinct authors split across several
  parallel chunks. The exact numeric improvement is left to the plan/implementation phase to
  measure against SC-001's per-request lookup count.
- The residual bimodal latency noted in the issue's investigation (some requests ~570ms, others
  ~180ms, even after the cache-size fix) is a known, unexplained, separate phenomenon. It is
  explicitly not attributed to this item and is not a blocking dependency for closing it.
- "Optional" in User Story 2 means: if implementing FR-001/FR-002 alone already satisfies
  SC-001, FR-004 may be descoped without failing this spec's acceptance — consistent with
  issue #37148's guidance to re-evaluate scope after the highest-leverage fix lands.
