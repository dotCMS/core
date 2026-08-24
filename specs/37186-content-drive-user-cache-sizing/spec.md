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
  test dataset, and zero lookup-failure markers appeared in the response.

The confirmed root cause is a **cache capacity mismatch**: the user cache region defaults to
1,000 entries while the reference instance has 1,828 active users, so the cache evicts
continuously during normal listing navigation, and each eviction is amplified by redundant
same-user resolution within a single row.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Listing stays fast regardless of how many users exist (Priority: P1)

An administrator browses a folder listing (Content Drive or Site Browser) in an instance with
a large user base. The listing should not repeatedly re-fetch user records from the database
for users it already resolved earlier in the same browsing session.

**Why this priority**: This is the confirmed root cause. Without it, every other change in
this item is irrelevant — the cache will keep evicting regardless of how many redundant
lookups per row are removed.

**Independent Test**: On an instance with more distinct content-authoring users than the
default cache capacity, repeated listing requests for different folders should show the
`user_`-by-id lookup count per request converge to a small, stable number after a warm-up
pass, instead of remaining in the hundreds.

**Acceptance Scenarios**:

1. **Given** an instance with more active users than the default user-cache capacity, **When**
   an administrator repeatedly lists folders whose content spans many distinct authors,
   **Then** the number of user-record lookups per request stabilizes to a low count after
   warm-up, rather than staying elevated indefinitely.
2. **Given** the fix is applied, **When** the same listing request is repeated immediately
   after a first call, **Then** the second call issues at most a handful of user-record
   lookups (cache hits), not hundreds.
3. **Given** the fix is applied, **When** compared against the pre-fix baseline on the same
   dataset and folder, **Then** the returned rows and their `modUserName`/`ownerUserName`
   values are unchanged — only the lookup volume changes, not the results.

**Assumption**: "converge to a small, stable number" and "low count" are qualitative because
this fix's goal is bounding cache churn, not hitting a specific numeric target — the target
depends on instance size and is not itself a product requirement. See FR-001.

---

### User Story 2 - No repeated resolution of the same user within one row (Priority: P2)

While rendering a single listing row, the system resolves the content's modifying user and
owner. Today the same user id can be resolved multiple times for the same row through
different code paths (audit properties, version properties, URL building). Reducing that
redundancy lowers the amplification factor of each cache miss, independent of cache sizing.

**Why this priority**: This is the secondary, optional half of the fix. It reduces the *cost
per eviction* rather than the *eviction rate*, so it has value even with User Story 1 done,
but it is not required to close the item — User Story 1 alone is expected to resolve almost
all of the observed lookup volume.

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

### Edge Cases

- What happens when a listing folder contains content whose author/owner user id does not
  exist in `user_` (a genuinely missing user, distinct from a cache miss)? Existing behavior
  (falling back to "N/A" / "unknown" / "system" labels) must be preserved — this item does not
  change error-handling semantics, only cache sizing and redundant-resolution behavior.
- What happens on the very first listing request after a fresh startup, before any user is
  cached? The initial warm-up cost is expected and out of scope for reduction — only steady-
  state (post warm-up) behavior is in scope for User Story 1's acceptance criteria.
- What happens if the instance's active user count grows over time past whatever capacity is
  configured? With the FR-001 resolution (a generous static default, not auto-sizing), this
  residual risk is not eliminated, only pushed far higher — an instance whose user count
  eventually exceeds the new default will reproduce today's problem at a new threshold. Accepted
  as a known limitation of the chosen approach; an operator with an unusually large user base can
  still raise `cache.userdotcmscache.size` manually, same as today.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001 — RESOLVED (2026-08-24): option B, a saner static default, not auto-sizing.**
  The user cache capacity remains an operator-configured value (`cache.userdotcmscache.size`),
  but the shipped default MUST change from its current effectively-1,000 fallback to a value
  with real headroom over a typical instance's active user count (the tested value, 4,000,
  resolved the reference instance's 1,828 users with margin; the plan phase picks the exact
  documented default). No automatic sizing (e.g., counting `user_` at startup) is implemented —
  that path was rejected as disproportionate engineering effort (start-up-time counting logic,
  recalculation-on-growth behavior, failure handling if the count query itself is slow or fails)
  for a problem a documented, generous default already resolves in practice. This is a
  technical call, consistent with how items 2 and 3 of this issue were resolved without
  escalating to product sign-off.
- **FR-002**: The default configuration MUST NOT silently leave the user cache smaller than a
  typical or realistic instance's active user count when the feature ships (i.e., the
  commented-out / effectively-1000 default must change).
- **FR-003**: The system MUST continue to return identical listing content (rows, user display
  names, and fallback labels for missing users) after this change — this is a cache-behavior
  and redundancy-reduction fix, not a behavior or data change.
- **FR-004**: Within a single row transformation, the system SHOULD avoid resolving the same
  user id more times than the number of distinct output fields that require a name for that
  id (optional half of the fix; a "not worth doing" outcome for this half alone is acceptable
  if FR-001/FR-002 already resolve the bulk of the churn, per the issue's own
  attribution-first guidance).
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

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For a folder listing whose content spans a number of distinct authors up to the
  instance's real active-user count, repeated listing requests after an initial warm-up show
  user-record lookup volume per request drop by at least an order of magnitude compared to the
  pre-fix baseline measured for this item (previously observed averaging in the hundreds per
  request).
- **SC-002**: Listing content (returned rows, display names, and fallback labels) is verified
  unchanged before and after the fix on the same dataset and folder.
- **SC-003**: For a typical/expected instance size, the fix requires no per-instance manual
  tuning step for the default behavior to be adequate — an operator should not need to hand-pick
  a cache size out of the box. This does not extend to instances whose active user count exceeds
  the new default with no margin left (see Edge Cases); those retain the existing manual
  `cache.userdotcmscache.size` override as today.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The shared user-resolution cache (`UserCacheImpl` /
  `UserFactoryImpl.loadUserById`) used across folder listings (Content Drive and Site Browser)
  and, incidentally, anywhere else in the product that resolves users by id through the same
  cache region. This is a shared, low-level cache, not a Content-Drive-specific mechanism —
  changes here have a wider blast radius than the two listing endpoints that surfaced the
  problem.
- **Backward-compatibility expectations**: No API, admin workflow, or content behavior changes.
  This is purely a cache-sizing / internal-redundancy change; existing content, listings, and
  admin screens must behave identically, only faster/cheaper under cache churn.
- **Known related decisions**: None identified specific to user-cache sizing. Related, but
  explicitly out of scope for this item: the subfolder-transform-before-paging inefficiency in
  `BrowserAPIImpl` and the two non-batch `filterCollection` call sites flagged in issue
  #37148's investigation comments — both are separate, unaddressed findings that happen to
  live near this code, not this item's concern.

## Assumptions

- The reference measurement (avg ~688 lookups/request dropping to 1-2 after raising cache size
  to 4000) was taken on a single test instance with 1,828 active users. Per FR-001's resolution,
  the new default is a fixed value chosen with headroom over that reference point, not derived
  per-deployment; deployments with meaningfully larger active user populations than the chosen
  default may still need a manual override, same as today.
- This item's win is expected to be small for a single user (a few milliseconds) per issue
  #37148's own measurements; its value is in avoiding proportionally larger cost under
  concurrent multi-user load, which has not been measured and is not re-measured as part of
  this spec — validating that is left to the implementation/plan phase or a follow-up.
- The residual bimodal latency noted in the issue's investigation (some requests ~570ms, others
  ~180ms, even after the cache-size fix) is a known, unexplained, separate phenomenon. It is
  explicitly not attributed to this item and is not a blocking dependency for closing it.
- "Optional" in User Story 2 means: if implementing FR-001/FR-002 alone already satisfies
  SC-001, FR-004 may be descoped without failing this spec's acceptance — consistent with
  issue #37148's guidance to re-evaluate scope after the highest-leverage fix lands.
