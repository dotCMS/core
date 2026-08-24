# Feature Specification: Content Drive Field-Filter Chunk Multiplier

**Feature Branch**: `37148-field-filter-chunk-multiplier`

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
its full database scan of the folder's contents multiple times before it has gathered enough
matches to fill a page — on a ~20,000-item folder this took roughly four repeats and made the
response six times slower than an equivalent search. The user should instead get their filtered
results back in roughly the time a comparable search takes, without repeated re-scanning.

**Why this priority**: This is the dominant remaining performance gap Content Drive has against
its baselines once the highest-cost item (item 1) is addressed; field filtering over large
folders is a normal, frequent authoring action.

**Independent Test**: Can be fully tested by applying a single content-type field filter (no Tag,
no Relationship, no workflow, no free-text term) against a folder with a large number of children
and confirming both that the number of times the underlying candidate scan runs drops to at most
one, and that the returned result set is unchanged from today's behavior.

**Acceptance Scenarios**:

1. **Given** a folder with ~20,000 children and a filter on a Text field that matches 40 items,
   **When** the user applies the filter, **Then** the system scans the folder's candidate content
   at most once (not four times as today) and returns the same 40 matching items.
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
- **FR-002**: When that condition holds, and the request has no workflow filter and no free-text
  or file-name term, System MUST resolve the request without repeatedly re-scanning the database
  candidate set — the underlying database scan MUST execute at most once per request, regardless
  of folder size or how sparse the matches are.
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
- **FR-009 — RESOLVED (2026-08-24): no dedicated kill switch.** No independently-toggleable
  operator flag for the single-pass behavior in FR-002. Decision reasoning: the freshness
  trade-off it would guard (see Assumptions) is narrow and scoped — it applies only when zero
  database-required criteria are present — and mirrors a trade-off ADR-0018 already accepts by
  default for free-text search, without a dedicated flag for that case either. The existing
  general search-strategy configuration (`BROWSE_API_HEURISTIC_TYPE`) remains the escape hatch of
  last resort if the single-pass path needs to be disabled entirely.

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

- **SC-001**: For a field filter with no database-required criteria over a folder with roughly
  20,000 items, the number of times the system re-scans the folder's candidate content drops from
  four to at most one.
- **SC-002**: Response time for that same case falls to within 20% of the response time of the
  closest equivalent content-search operation (matching the threshold already used to evaluate
  Content Drive elsewhere in this investigation).
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

- **Shared query with item 1 (separate spec, out of scope here)**: A separate, parallel effort
  (issue #37148 item 1) is reworking the same database candidate-scan query this fix's chunk loop
  currently calls repeatedly, to fix an unrelated planner-instability problem on very large
  folders. This fix reduces how often that query is invoked for the field-filter-only case
  addressed here, which lowers this case's exposure to item 1's problem, but does not fix it. Item
  1's eventual change to that query must continue to behave correctly when invoked at most once
  per request, as this fix will do for its case. Coordinate before either change merges independently.

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
