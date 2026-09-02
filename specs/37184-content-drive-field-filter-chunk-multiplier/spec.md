# Feature Specification: Content Drive Field-Filter Chunk Multiplier

**Feature Branch**: `issue-37184-content-drive-field-filter-chunk-multiplier`

**Created**: 2026-08-24

**Status**: Draft

**Type**: Performance Fix

**Related GitHub Issue**: [#37184](https://github.com/dotCMS/core/issues/37184). Parent epic: [#36814](https://github.com/dotCMS/core/issues/36814). Originally investigated as item 2 of [#37148](https://github.com/dotCMS/core/issues/37148); item 1 "candidate-scan query" ([#37183](https://github.com/dotCMS/core/issues/37183)) and item 4 "per-row user lookup" ([#37186](https://github.com/dotCMS/core/issues/37186)) are tracked and spec'd as separate sibling issues.

**Input**: User description: "Content Drive — Item 2: eliminate the field-filter chunk multiplier in BrowserAPIImpl. Field filters that route entirely to the search index still re-run the expensive database candidate-scan query multiple times per request instead of once, because the DB-first hybrid chunk loop keeps iterating even when there is no database-routed criterion left to protect."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Filtering a large folder by a content-type field returns promptly (Priority: P1)

A content author or administrator browsing a folder with thousands of items applies a filter on
a content-type field (for example, a Text, Select, Date-range, Multi-select, or Category field).
Today, once the filter is entirely resolvable against the search index, the system still repeats
its full database scan of the folder's contents — and, per matching chunk, a separate
Elasticsearch filtering call — before it has gathered enough matches to fill a page. On a
~20,000-item folder with sparse matches this can iterate up to ~23 times (chunk size 900); the
issue's own reference case measured roughly four repeats and a six-fold slowdown versus an
equivalent search, but that reference case is a specific measured point, not the worst case (a
folder with even sparser matches or a larger size iterates more). The user should instead get
their filtered results back in roughly the time a comparable search takes, without repeated
re-scanning or repeated ES round trips.

**Why this priority**: This is the dominant remaining performance gap Content Drive has against
its baselines once the highest-cost item (item 1) is addressed; field filtering over large
folders is a normal, frequent authoring action.

**Independent Test**: Can be fully tested by applying a single content-type field filter (no Tag,
no Relationship, no workflow, no free-text term) against a folder with a large number of sparse
matches and confirming both that the number of times the underlying candidate scan runs AND the
number of Elasticsearch filtering calls each drop to at most one, and that the returned result
set is unchanged from today's behavior.

**Acceptance Scenarios**:

1. **Given** a folder with ~20,000 children and a filter on a Text field that matches 40 sparse
   items, **When** the user applies the filter, **Then** the system scans the folder's candidate
   content at most once and issues at most one Elasticsearch filtering call (not up to ~23 of
   each as today's sparse worst case) and returns the same 40 matching items.
2. **Given** the same folder and filter, **When** the user applies a Date-range, Multi-select, or
   Category filter instead, **Then** results are identical to today's behavior and the same
   single-scan improvement applies.
3. **Given** a small folder (a few dozen children) with the same kind of field filter, **When**
   the user applies it, **Then** results are unchanged and response time does not regress.

---

### User Story 2 - Filters that must stay database-first keep working exactly as before (Priority: P2)

A user filters by a Tag field, a Relationship field, a workflow scheme/step, or a free-text
search term — alone or combined with a content-type field filter. These criteria must keep being
resolved against the database so that content the user (or a teammate) just saved is immediately
visible and correctly filtered, even if the search index has not caught up yet. This behavior must
not change as a side effect of speeding up the pure-field-filter case.

**Why this priority**: This protects the read-your-writes guarantee that Content Drive depends on
for Tag, Relationship, workflow, and text filtering; regressing it would reintroduce the exact
"I saved it and it vanished" complaint the current database-first design exists to prevent.

**Independent Test**: Can be fully tested by combining a content-type field filter with a Tag
filter (and separately with a Relationship filter, and separately with a workflow filter, and
separately with a free-text term) and confirming the result set and the number of database scans
are unchanged from today's behavior in every combination.

**Acceptance Scenarios**:

1. **Given** a folder and a search that combines a content-type field filter with a Tag filter,
   **When** the user applies it, **Then** the result set is unchanged from today and the request
   still resolves through the existing database-first path.
2. **Given** the same setup but with a Relationship filter instead of Tag, **When** the user
   applies it, **Then** behavior is unchanged.
3. **Given** a search that adds a workflow scheme/step filter on top of a content-type field
   filter, **When** the user applies it, **Then** behavior is unchanged.
4. **Given** a search that adds a free-text search term on top of a content-type field filter,
   **When** the user applies it, **Then** behavior is unchanged (the free-text path continues to
   combine database and index results as it does today).

### Edge Cases

- What happens when the field filter matches zero items in a large folder? The system should
  still resolve this in a single pass rather than exhausting every chunk before concluding there
  are no matches.
- What happens when a field filter is combined with pagination (deep pages)? Paging behavior and
  page-to-page consistency must be unchanged from today.
- What happens when the folder itself is very large and permission filtering removes most
  candidates? The mandatory read-permission check still runs after candidates are retrieved, on
  every path, so a permission-restricted user never sees content they cannot read.
- What happens on an empty folder, or a folder with no items matching the filter at all? The
  system returns an empty result without unnecessary repeated scanning.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST determine, for each field-filter search request, whether every
  requested field criterion is resolvable purely against the search index (i.e., no criterion
  requires the database to preserve immediate visibility of recent writes).
- **FR-002 — implementation named (2026-08-31) and mechanism corrected (2026-09-01, per
  review): hybrid single-pass, bounding both DB scans and ES round trips as one criterion.**
  When that condition holds, and the request has no workflow filter and no free-text or
  file-name term, System MUST resolve the request in a **single pass** over the folder's
  candidate content: at most one database scan AND at most one Elasticsearch filtering call for
  the whole request, regardless of folder size or how sparse the matches are. This is a
  **hybrid** fix: the existing database-first candidate scan still runs, it does not switch to
  `buildPureESQuery`/index-only resolution. That alternative was considered and rejected:
  `buildPureESQuery` (`BrowserAPIImpl.java:605`) has no folder/parentPath filter today (only
  host-level scoping), and moving structural filtering there would conflict with this spec's own
  cited ADR-0018 requirement that structural/metadata filtering stay database-first always (see
  Legacy Considerations). FR-003, FR-004, and FR-007 below describe *preserving* existing
  database-sourced behavior, not building new index-side folder/permission/pagination logic.
  **Correction to the mechanism, and why both round trips need one bound**:
  `getContentByChunks` (the loop this fix changes) already has an early exit — it stops as soon
  as `accumulatedContent.size() >= maxRows` (`BrowserAPIImpl.java:291`) or the DB chunk comes
  back partial (`:298`). The previously-observed "~4 repeats" is the **sparse-match worst
  case**, not today's universal behavior — a folder with dense matches can already exit after
  chunk 1. What the early exit does *not* bound is the **ES side**: each loop iteration calls
  `processESDirectly` once per chunk (`BrowserAPIImpl.java:343-344`), so a sparse-match,
  20,000-item folder at the default `BROWSER_CONTENT_CHUNK_SIZE` (900,
  `BrowserAPIImpl.java:547`) can still iterate up to `20,000 / 900 ≈ 23` times before either
  exiting or hitting `BROWSER_DB_MAX_SCAN_ROWS` (default 50,000, `:734-735`) — 23 DB round trips
  *and* 23 ES round trips, not the "~4" the issue measured on its specific reference case. A fix
  that only forces the DB scan itself into one query but leaves the surrounding chunked-ES-call
  loop in place would not close this gap. FR-002 is therefore one bound covering both: the
  database candidate scan for this case MUST assemble the full candidate set in a single query
  (no chunking), and Elasticsearch filtering over that candidate set MUST run as a single call,
  not one call per artificial chunk.
- **FR-003**: The single-pass resolution in FR-002 MUST still be scoped to the folder (and site)
  the user is browsing — it must never return content from outside the requested folder.
- **FR-004**: System MUST still apply the mandatory read-permission filter, sourced from the
  database, after candidates are retrieved, for every request — this fix MUST NOT weaken or skip
  permission filtering to gain speed.
- **FR-005**: When any criterion requires database resolution to preserve immediate visibility of
  recent writes (a Tag filter or a Relationship filter), OR a workflow filter is present, OR a
  free-text/file-name term is present, System MUST behave exactly as it does today: full
  database-first resolution, unchanged result sets, unchanged number of scans.
- **FR-006**: The database-first resolution strategy MUST remain the default behavior for every
  request shape not covered by FR-002; this fix MUST NOT change what the default search strategy
  is for the general case.
- **FR-007**: Result sets returned for field filters on Text, Date-range, Multi-select/Checkbox,
  Tag, and Category fields MUST be unchanged from current behavior (same items, same order, same
  pagination behavior).
- **FR-008**: Combining a content-type field filter with a Tag filter MUST continue to return the
  same correct results as today.
- **FR-009 — RESOLVED (2026-08-24): no dedicated kill switch. Reasoning corrected
  (2026-09-01, per review) — `BROWSE_API_HEURISTIC_TYPE` DOES apply here, and it is not a safe
  substitute.** No independently-toggleable operator flag for the single-pass behavior in
  FR-002. Decision reasoning: the freshness trade-off it would guard (see Assumptions) is narrow
  and scoped — it applies only when zero database-required criteria are present — and mirrors a
  trade-off ADR-0018 already accepts by default for free-text search, without a dedicated flag
  for that case either. **Correction**: `BROWSE_API_HEURISTIC_TYPE` (`SearchHeuristicType`,
  `BrowserAPIImpl.java:465-470`) is **not** limited to free-text filtering — despite its name,
  `doElasticSearchTextFiltering` is the dispatcher for every request `isUseElasticSearchForFiltering`
  routes to ES, and that includes a pure field-filter request with **zero** free-text/file-name
  term, as long as it has at least one index-routed field criterion
  (`isUseElasticSearchForFiltering`, `BrowserAPIImpl.java:1577-1584`: `hasIndexFieldCriteria`
  alone is sufficient). So this config *does* gate the FR-002 case. It is still not usable as a
  kill switch, though: setting it to `PURE_ES` is not inert for this case — `doPureESQuery`'s
  guard explicitly throws `DotRuntimeException` for any request with field criteria present
  (`BrowserAPIImpl.java:496-504`: *"Content Drive field filters (userSearchable) are not
  supported under the PURE_ES heuristic"*), i.e. it would break every field-filter request, not
  disable just the single-pass optimization. There is genuinely no existing config-level escape
  hatch for *this specific fix*; if the single-pass path ever needs disabling, that requires a
  new flag added at that time. The spec previously stated the opposite conclusion for the wrong
  reason (claiming the config didn't apply at all) — corrected here so an operator doesn't reach
  for `BROWSE_API_HEURISTIC_TYPE=PURE_ES` expecting it to be a safe no-op.

### Key Entities

- **Field-filter search request**: A Content Drive request that narrows results by one or more
  content-type field values (e.g., a Text field "contains", a Date range, a Multi-select "in
  list", a Tag, or a Category), scoped to a folder/site, language, and the requesting user's
  permissions.
- **Field criterion**: A single field-level condition within a request, distinguished by whether
  it must be resolved against the database (Tag, Relationship) to preserve immediate visibility of
  recent writes, or may be resolved against the search index (Text, Select, Boolean, Date,
  Category).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 — corrected (2026-09-01, per review) to bound both round-trip types; unit of
  measure resolved (2026-09-01, during planning).** For a field filter with no database-required
  criteria over a folder with roughly 20,000 items and sparse matches (the worst case, not the
  previously-cited "four" reference point — dense-match cases can already exit early today), the
  number of database scans drops to **at most one** AND the number of **logical** Elasticsearch
  filtering calls (invocations of `processESDirectly`) drops to **at most one**, down from up to
  ~23 of each at the default chunk size on this folder size (see FR-002). **Resolved
  clarification**: "Elasticsearch filtering calls" counts logical invocations of the filtering
  step, not physical ES HTTP round trips. `processESDirectly` already internally re-splits any
  candidate set above ~900 inodes into multiple physical ES queries (Lucene's 1024-boolean-clause
  limit) — this fix does not change that internal splitting, and a 20,000-candidate worst case
  can still produce ~20-23 physical ES round trips after this fix ships. Bounding the physical
  round-trip count too would require switching the query mechanism itself (inode enumeration →
  a `terms` filter), which is a separate, larger change and explicitly out of scope here.
- **SC-002**: Response time for that same case falls to within 20% of the response time of the
  closest equivalent content-search operation (matching the threshold already used to evaluate
  Content Drive elsewhere in this investigation). **Dependency**: because FR-002 is a
  hybrid single-scan fix (the database candidate-scan query still runs once, per FR-002's
  resolution above), this target is only reachable once that single scan itself is fast on
  large folders — i.e., once issue #37148 item 1's fix (spec'd separately in #37230, materialized
  folder-first CTE) has landed. Before #37230 lands, a single scan over a ~20,000-item folder is
  still expected to take roughly the same ~470-490ms that motivated #37230, so SC-002 cannot be
  validated in isolation from that dependency.
- **SC-003**: 100% of tested combinations of a field filter with a Tag filter, a Relationship
  filter, a workflow filter, or a free-text term return results identical to today's behavior.
- **SC-004**: 100% of tested field-filter-only cases (Text, Date-range, Multi-select, Category)
  return the same result sets as today, across small, medium, and large folders.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Content Drive / Site Browser search backend
  (`com.dotcms.browser` package — modern, not legacy code) that decides whether a search is
  resolved against the database, the search index, or a hybrid of both.
- **Backward-compatibility expectations**: The API contract, response shape, and result
  correctness for every existing filter combination must be unchanged. Only the internal number of
  database round-trips for the specific case described in FR-002 changes. No database schema, no
  index mapping, and no REST contract changes are anticipated.
- **Known related decisions**: ADR-0018 (database-first search for Content Drive, with text
  filtering deferred to the search index) is binding on this work. It requires that structural and
  metadata filtering stay database-first always, that the database-first hybrid strategy remain
  the default, and that the index-only strategy never become the default. This fix operates
  strictly inside that contract: it changes how many times the existing hybrid strategy's database
  step runs for one already-correctly-routed case, not which source of truth is authoritative for
  any criterion. Whether structural/metadata filtering should ever move to the index is already
  settled by ADR-0018 and is out of scope for this spec.

## Dependencies & Coordination

- **Shared query with item 1 (separate spec, out of scope here) — hard dependency for SC-002.**
  A separate, parallel effort (issue #37148 item 1, spec'd in #37230) is reworking the same
  database candidate-scan query this fix's chunk loop currently calls repeatedly, to fix an
  unrelated planner-instability problem on very large folders. This fix reduces how often that
  query is invoked for the field-filter-only case addressed here (from up to ~23 in the
  sparse-match worst case to 1), which is a real win regardless of #37230's status — but SC-002's
  search-comparable latency target additionally
  requires that single remaining scan itself to be fast, which is exactly what #37230 delivers.
  In other words: FR-002 (scan count) can ship and be verified independently of #37230; SC-002
  (latency target) cannot. Item 1's eventual change to that query must continue to behave
  correctly when invoked at most once per request, as this fix will do for its case. Coordinate
  before either change merges independently, and sequence SC-002's validation after #37230 lands.

## Assumptions

- A field filter is considered fully index-resolvable when none of its criteria are a Tag or a
  Relationship field (the two field types the codebase already resolves against the database to
  preserve immediate visibility of recent writes); this mirrors the existing, documented routing
  rule and is not a new judgment call introduced by this fix.
- **RESOLVED (2026-08-24), no product sign-off required.** Accepting a brief, index-lag-bounded
  delay before a just-written item appears in a field-filter-only search (no Tag, no
  Relationship, no workflow, no free-text term) is a deliberate, scoped trade-off for this one
  case, mirroring the trade-off ADR-0018 already accepts by default for free-text search — it is
  not a general relaxation of Content Drive's read-your-writes guarantee, which continues to hold
  for every other filter combination. Treated as a technical decision, not a product one, because
  it extends an already-accepted architectural trade-off to one additional, narrowly-scoped case
  rather than introducing a new one. Ships as default behavior (see FR-009 — no dedicated kill
  switch either).
- The existing per-field index query logic (already used by the current hybrid strategy) is
  assumed to be reusable as-is for the single-pass case; no new field-to-index translation logic is
  expected to be needed.
- Test-first development (Constitution Principle V) applies: expected home for these tests is
  `BrowserAPITest` (integration — scan-count assertions for FR-002, result-set parity for
  FR-005/FR-007/FR-008) plus a dedicated timing assertion for SC-002 that is only meaningful
  once #37230 lands — exact test design deferred to the planning phase.
