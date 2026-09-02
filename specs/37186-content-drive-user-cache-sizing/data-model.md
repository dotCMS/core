# Phase 1 Data Model: Bound user-cache eviction cost during folder listings

This item introduces no new persisted entity, DB table, or ES mapping. The "entities" below are
the in-memory/request-scoped concepts the spec's Key Entities section names, made concrete for
implementation. Nothing here requires a migration.

## User cache entry

- **Representation**: `com.dotmarketing.business.User`, keyed by user id (`userid` from
  `user_`), held in the Guava-backed `UserCacheImpl` (region name `UserDotCMSCache`).
- **Capacity**: Bounded by `cache.userdotcmscache.size` (FR-002 changes the shipped default
  from the `cache.default.size=1000` fallback to `4000`; no change to the eviction policy
  itself — still Guava `maximumSize` with the existing `concurrencyLevel`).
- **Lifecycle**: Unchanged by this item. Populated on first `loadUserById` DB miss
  (`UserFactoryImpl.java:104-122`), evicted per Guava's existing size-based policy. FR-001 does
  not add a new cache or region — it only changes *when*, within one request, entries get
  populated (sequentially, before parallel fan-out, instead of racily, during it).
- **Fields relevant to this item**: none beyond what `User` already carries (id, full name,
  first/last name) — this item does not add fields to the cached record.

## Listing row

- **Representation**: One `Contentlet` (pre-hydration) → one `Map<String,Object>` row
  (post-hydration), as already produced by `BrowserAPIImpl.hydrate(...)`.
- **User-id references per row**: up to three distinct user ids may need resolution —
  `modUser` (audit + version properties), `owner` (audit properties), and locked-by (version
  properties, only if the row is locked). All three ultimately resolve through the same
  `UserFactoryImpl.loadUserById`.
- **No schema change**: The row's output shape (`modUserName`, `ownerUserName`, `lockedBy`) is
  unchanged (FR-003) — this item changes *how many DB round trips* those fields cost across a
  page, and *how gracefully* an orphan id degrades (FR-004a), not what the fields contain for a
  resolvable user.

## Warm-up set (new, request-scoped, not persisted)

- **Representation**: A `Set<String>` of distinct user ids, computed once per listing request
  from the page's `List<Contentlet>` (the `modUser`/`owner` — and, where derivable without
  extra cost, locked-by — id of every row about to be hydrated).
- **Scope**: Exists only for the duration of one `getContentUnderParentFromDB(...)` call inside
  `BrowserAPIImpl`. Not cached, not shared across requests, not persisted anywhere — this is
  the object FR-001 introduces to drive the sequential `loadUserById` warm-up loop.
- **Relationship to the user cache entry**: Every id in the warm-up set is resolved via the
  existing `userAPI.loadUserById(id)` (same method, same cache, same locking semantics as
  today) — the warm-up set is purely an ordering/collection mechanism in `BrowserAPIImpl`, not
  a new caching layer. On completion, every id in the set is guaranteed cache-warm (a Guava
  cache entry exists for it, subject to the region's normal eviction policy) before
  `hydrateContentletsInParallel` starts.
- **Failure handling**: An id resolution failure during warm-up (orphan user, `NoSuchUserException`)
  must not abort the warm-up loop for the remaining ids — it should be handled the same way
  FR-004a handles it downstream (skip/soft-fail that one id, continue), so a single orphan
  reference doesn't turn the warm-up itself into a second place the whole request can fail.

## No new contracts

No REST request/response shape changes, no new DB columns, no new ES fields. See plan.md
"Project Structure" — no `contracts/` directory for this item.
