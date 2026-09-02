# Feature Specification: Content Drive Folder-Listing Candidate Scan Stabilization

**Feature Branch**: `[37229-content-drive-folder-cte]`

**Created**: 2026-08-25

**Status**: Draft

**Type**: Defect Fix (Performance)

**Input**: User description: "Content Drive: use a materialized folder-first CTE for the candidate-scan query on large folders (implements the validated direction from spike #37183, follow-up to issue #37229 — this is a performance defect fix, not a spike)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browsing a large folder in Content Drive stays fast (Priority: P1)

A content author or admin opens Content Drive and navigates into a folder holding thousands of
items. Today, on the largest folders in the reference dataset, this listing can take roughly
half a second to render a page of results — long enough to feel like the browser is hanging —
while browsing a small or medium folder is near-instant. The delay is not proportional to folder
size in a way users (or admins tuning the system) can predict: two folders of similar size can
behave very differently. The fix must make worst-case folder browsing consistently fast without
making any currently-fast folder noticeably slower.

**Why this priority**: This is the reported defect and the reason the issue exists. Every other
scenario in this spec exists to guard that fixing it doesn't silently break something else.

**Independent Test**: Load a folder with several thousand items (in the range where the defect
reproduces) and confirm the page renders in roughly the same time class as a small folder does
today, with the exact same items shown in the exact same order as before the fix.

**Acceptance Scenarios**:

1. **Given** a folder with a large number of children that today loads slowly, **When** a user
   opens that folder in Content Drive, **Then** the page of results appears markedly faster than
   before, landing in the same rough time class as the fix's validated reference case (spike
   #37183: ~470-490ms → ~120-136ms on the largest tested folder).
2. **Given** a folder that already loads quickly today, **When** a user opens that folder after
   the fix ships, **Then** it still loads quickly — any added latency is small and bounded (the
   spike's reference point: +25-30ms on an already-fast folder), not a multi-fold regression.
3. **Given** any folder size (empty, small, medium, large, or the largest in the test matrix),
   **When** the listing is fetched before and after the fix, **Then** the exact same items are
   returned in the exact same order, with ties in `mod_date` broken deterministically — the fix
   changes speed and adds ordering determinism, not the result set (outside of the host_inode
   correctness fix in FR-004a).

---

### User Story 2 - Filtered and permission-scoped browsing keeps working exactly as before (Priority: P1)

Content Drive supports filtering a folder's contents by tag, by workflow scheme/step, and by
per-field search criteria, and every listing is scoped to what the requesting user is allowed to
read. These are the query's other moving parts, and the fix must not change what any of them
returns — only how fast the underlying scan runs.

**Why this priority**: The fix touches the query that every one of these filters and the
permission check builds on. If any of them silently changes behavior, the fix has broken
something more visible than the slowness it fixed. Equal priority to User Story 1 because a fast
but incorrect listing is not an acceptable outcome.

**Independent Test**: Run the same filtered/permission-scoped requests before and after the fix
(tag filter, workflow filter, field filter, each combined with an administrator and with a
permission-limited user) and confirm identical results.

**Acceptance Scenarios**:

1. **Given** a folder listing filtered by tag, **When** the fix is applied, **Then** the same
   tagged items are returned as before.
2. **Given** a folder listing filtered by workflow scheme and/or step (including the
   archive-target-step branching that governs whether archived items are included), **When**
   the fix is applied, **Then** the same items are returned as before.
3. **Given** a folder listing filtered by a per-field search criterion (Tag field or
   Relationship field, the two field types resolved in the database today), **When** the fix is
   applied, **Then** the same items are returned as before.
4. **Given** the same folder request issued by an administrator and by a permission-limited
   user, **When** the fix is applied, **Then** each user still sees exactly the set of items they
   were permitted to see before the fix.
5. **Given** a deep-pagination request (a page far from the start of a large folder), **When**
   the fix is applied, **Then** the same page of items is returned as before, at the same cursor
   position.

---

### Edge Cases

- An empty folder (zero children) must behave identically before and after — no error, no
  spurious rows, same (empty) result.
- A folder so small that the current query already runs in well under a millisecond must not
  regress into measurably slower territory just because a different query shape now always
  applies.
- A folder-listing request that has no folder scoping at all (browsing a whole site, or a
  request that explicitly skips the folder predicate) does not go through the folder-scoped path
  this fix changes, and must continue to behave exactly as it does today — this fix is scoped to
  requests that do have a folder predicate.
- A request combining several of the dynamic predicates at once (for example: a tag filter, a
  workflow filter, and a language restriction, all against the largest folder) must still return
  correct results, and its latency — while not the primary target of this fix — must not regress
  by more than the same small, bounded amount called out in User Story 1.
- A folder whose content spans more than one site sharing the same relative path is the case
  where today's query applies no host filter at all — this is itself a correctness defect
  identified in #37148, not an intentional behavior. This fix's scope now includes adding the
  missing `host_inode` filter (see FR-004a), so this case is **expected to change**: after the
  fix, only items under the requested folder's own site are returned, not items sharing the same
  relative path across sites.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001 — contradiction fixed (2026-09-01, per review).** The system MUST return identical
  result sets (same items, same pagination cursors) for every folder-scoped listing request
  before and after the fix, across empty, small, medium, large, and extreme-sized folders, and
  across at least one deep-pagination case. **Order is identical with two named exceptions**:
  (1) the cross-site correctness gap addressed by the host_inode fix in FR-004a, where the
  *result set itself* is expected to change on purpose (see FR-004a); and (2) the relative order
  **among rows that share the same `mod_date`** (per #37148, roughly 1.2% of rows in the
  reference dataset), where today's order is an unspecified, planner-dependent artifact — not a
  guarantee this fix preserves. Because `ORDER BY mod_date` alone has no tiebreaker, the fix MUST
  add a deterministic tiebreaker column (e.g. `inode` or `identifier`) to the sort, so that after
  the fix, order among tied rows becomes a **guarantee** (stable, reproducible run to run) — it is
  not required to match whatever arbitrary order those specific tied rows happened to come back
  in before the fix. Outside of these two named exceptions (cross-site result-set change,
  tied-row ordering), every other row's position in the result set MUST be unchanged.
- **FR-002**: The system MUST resolve the folder-scoped candidate scan through a query shape that
  does not exhibit the current unstable-planner behavior — the same request against the same
  folder must not swing between a fast and a pathologically slow access path depending on
  incidental factors like page size or how the folder's content happens to be distributed.
- **FR-003**: The system MUST NOT introduce any database schema change (no new index, no new
  column) to achieve this fix; it relies on the indexing already available today.
- **FR-004 — sub-case count corrected (2026-09-01, per review): three SQL variants, not
  two.** The system MUST preserve every existing filtering behavior available on the
  folder-scoped listing today — language, base type / content type inclusion and exclusion,
  site/host scoping, workflow scheme/step matching (including archive-target-step branching),
  free-text and filename filtering, per-field Tag/Relationship criteria, show-on-menu,
  archived/deleted exclusion, and MIME type filtering — with identical results for each, with
  the single deliberate exception called out in FR-004a. Site/host scoping specifically produces
  **three distinct SQL shapes** today (`appendSiteQuery`/`appendSystemHostQuery`,
  `BrowserAPIImpl.java:2113-2125`), not two: (a) explicit site, no forced system host —
  `id.host_inode = ?`; (b) explicit site, forced system host — `id.host_inode = ? OR
  id.host_inode = 'SYSTEM_HOST'`; (c) no explicit site, forced system host —
  `id.host_inode = 'SYSTEM_HOST'` only. All three MUST keep producing identical results after
  the fix. (A fourth state — no site and no forced system host, or `ignoreSiteForFolders=true` —
  appends no host filter at all; that is FR-004a's case, not one of these three.)
- **FR-004a**: The system MUST add the missing `host_inode` filter to the folder-scoped candidate
  scan for the "no host restriction at all" sub-case, closing the cross-site correctness gap
  identified in #37148 (today, a folder whose relative path exists under more than one site
  incorrectly returns items from every matching site, not just the requested one). This is an
  intentional, in-scope behavior change, not a preserved behavior — it MUST be called out as such
  in the PR description and MUST NOT be silently absorbed into FR-001's "identical results"
  guarantee.
- **FR-005**: The system MUST preserve the existing READ-permission filtering behavior exactly,
  for both an administrator and a permission-limited user, for every folder size and filter
  combination in scope.
- **FR-006**: The system MUST NOT change which criteria are resolved against the database versus
  the search index — this fix changes only how the database resolves the criteria that already
  belong to it today.
- **FR-007**: The system MUST NOT change any public method signature, request contract, or
  response contract consumed by existing callers of the folder-listing capability — this is an
  internal change to how the database candidate scan is resolved, not an API change.
- **FR-008**: The fix MUST NOT depend on, or require as a prerequisite, resolution of the
  separate, already out-of-scope latency-bimodality investigation (the suspected
  prepared-statement plan-cache mechanism) — that remains a distinct, optional follow-up.
- **FR-009**: The fix MUST NOT itself increase how many times the underlying scan query executes
  per request — it changes the query's internal shape, not its execution count. Note: today,
  `getContentByChunks` already loops the scan roughly 4 times per request under the field-filter
  execution-count multiplier defect tracked separately in #37184; this fix's compatibility target
  is the *post-#37184* single-scan-per-request state, not the current multi-scan behavior, so the
  two fixes are expected to compose without conflict once #37184 lands.
- **FR-010 — validation query scope corrected (2026-09-01, per review): include the tiebreaker
  and host_inode fix in the EXPLAIN ANALYZE, not just the spike's original predicate set.**
  Before this fix's design is finalized, the plan phase MUST verify — by running `EXPLAIN
  ANALYZE` on the real candidate-scan query, with workflow (scheme/step) and per-field
  Tag/Relationship sub-queries present, **and with the FR-001 deterministic tiebreaker column
  added to the `ORDER BY`, and the FR-004a `host_inode` filter added to the CTE**, against a
  folder already confirmed to trigger today's slow plan — that the materialized-CTE shape's
  measured improvement (FR-002/SC-001) still holds with all of that in play, not just the
  narrower predicate set the original spike measured. This is a measurement, not a design
  choice: if it holds, proceed as specified; if it does not, the plan must address the gap
  before implementation. Both the tiebreaker and the host_inode filter are new query surface
  added by this spec's own resolutions (FR-001, FR-004a) — neither was part of the spike's
  original measurement, so neither is validated by that measurement on its own. This is the
  single highest-risk unknown in this spec and must not be deferred to post-implementation
  discovery.

*Resolved scoping decisions:*

- **Filename filter is in scope.** `appendFileNameQuery` adds a single predicate,
  `LOWER(id.asset_name) = ?`, on the same `identifier` row the folder/host predicate already
  constrains — not a new table, not a new join. It goes inside the same materialized CTE. This
  also narrows the risk surface for the validation step below: the fewer predicates left to
  evaluate against the *un-materialized* candidate set outside the CTE, the less exposure to
  plan instability from those remaining predicates (workflow, tags, content type, MIME).
- **Test-matrix folder sizes follow the pattern already used for issues #37184/#37185/#37186**:
  empty, small, medium, large, and extreme folders, sized relative to whatever dataset the test
  environment has — not pinned to the spike's specific reference numbers (0 / ~100 / ~2,000 /
  ~4,000-6,000 / ~20,000+), since those describe *that* dataset, not a requirement. What matters,
  per the spike's own finding, is that "large"/"extreme" cases are chosen to include folders
  known to trigger the slow plan today (verify via `EXPLAIN` before trusting a candidate folder
  as "large" for this purpose) — child count alone doesn't guarantee that.

*Not a decision — a required validation step before this fix's success criteria can be trusted:*

- **Whether the measured improvement holds once workflow (scheme/step) and per-field Tag/
  Relationship sub-queries run against the real predicate set is not something anyone needs to
  choose — it's an unmeasured fact.** The spike measured `parent_path + host_inode + deleted +
  lang` only; the real query can add workflow `EXISTS` sub-queries and tag/relationship `IN`
  sub-queries outside the CTE, never exercised together with the materialized-CTE shape. This
  must be the **first thing checked when implementation starts** — run `EXPLAIN ANALYZE` on the
  real query, with those predicates present, against a folder already known to trigger the slow
  plan. Two outcomes, not a choice between them: it holds (proceed as planned), or it doesn't
  (then a real design question opens — e.g., whether to exclude that filter combination from the
  optimization — which is not yet a live question because the underlying fact isn't known yet).
  Until this check runs, the 470-490ms → 120-136ms target in Success Criteria is exactly that — a
  target, not a confirmed result.

### Key Entities *(include if feature involves data)*

- **Folder listing request**: The set of filtering criteria (folder, site, language, type,
  workflow, tags, permissions, sort, pagination) that together determine which items a Content
  Drive request returns. This fix changes only how the underlying candidate set for such a
  request is gathered from the database, not what criteria it can carry.
- **Candidate set**: The set of items structurally scoped to a given folder (and, where
  applicable, a given site/host) before every other filter and the permission check are applied.
  This is the specific piece this fix reshapes for stability at scale.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Opening the largest folder in the test matrix renders its first page of results in
  the same rough time class the fix's validated reference case demonstrated (roughly a 3-4x
  improvement over the current worst case), rather than the multi-hundred-millisecond delay
  observed today — treated as a target informed by the spike, to be confirmed against the real
  query's full predicate set, not a guaranteed outcome stated as fact ahead of that confirmation.
- **SC-002**: No folder anywhere in the test matrix — including folders that already load quickly
  today — becomes noticeably slower after the fix; added latency on an already-fast folder stays
  within roughly +25-30ms, the spike's measured reference point, not an open-ended "small and
  bounded" claim.
- **SC-003 — split into three separate oracles (2026-09-01, per review), one per case.**
  Across the full size matrix, both user-permission levels, and every supported filter
  combination:
  - **SC-003a (no ties)**: For any two rows with distinct `mod_date` values, their relative
    order after the fix is identical to their relative order before the fix.
  - **SC-003b (tied rows)**: For rows sharing the same `mod_date`, their relative order after
    the fix is deterministic and reproducible run-to-run (guaranteed by the new tiebreaker) —
    it is explicitly **not** required to match whichever order those same tied rows happened to
    return in before the fix (see FR-001).
  - **SC-003c (cross-site case)**: For the "no host restriction at all" sub-case (FR-004a), the
    result *set* is expected to change after the fix (items outside the requested folder's own
    site are no longer included) — this is the one case where SC-003a's "same items" premise
    does not apply, by design.
- **SC-004**: Users browsing a permission-restricted folder see exactly the same set of items
  they were permitted to see before the fix — no over-exposure, no under-exposure.
- **SC-005**: The fix introduces no dependency on the search index for any criterion that is
  resolved against the database today — the routing of what is resolved in the database versus
  the index is unchanged.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Content Drive folder-listing candidate scan in
  `BrowserAPIImpl` (`dotCMS/src/main/java/com/dotcms/browser/BrowserAPIImpl.java`,
  `selectQuery`/`buildSelectBaseQuery` and the family of `append*` predicate builders it calls).
  This is a database-query-shape change inside an actively-used, actively-evolving authoring
  surface — not legacy code in the "untouched for years" sense, but a shared, heavily-parameterized
  query that several other in-flight and already-shipped changes (the field-filter multiplier fix
  in #37184, the user-cache-sizing fix, the archive-step workflow branching already in the code)
  also depend on.
- **Backward-compatibility expectations**: No change to any public API, request shape, or
  response shape. No schema or migration required. Every existing filter combination and every
  existing permission behavior must keep working unchanged — this is a defect fix on an internal
  data-access path, not a behavior change.
- **Known related decisions**: ADR-0018 (database-first Content Drive search, with only
  free-text/searchable-field matching deferred to the search index) governs this area. This fix
  was checked against the actual predicate set built by `selectQuery()` today — not just the
  spike's simplified representative query — and every predicate that ADR-0018 requires to stay
  database-resolved (folder, site, language, type, workflow, permissions, sort, pagination)
  remains database-resolved after this fix; nothing moves to the index. No conflict with
  ADR-0018 was found. Coordination note: issue #37184 (field-filter execution-count multiplier)
  shares this same query — this fix changes the query's shape, not how many times it executes per
  request, so the two are expected to compose without conflict, though that composition should be
  re-verified once #37184's change actually lands.

## Assumptions

- The existing unique index `identifier_parent_path_asset_name_host_inode_key` (on `parent_path,
  asset_name, host_inode`) is available in every environment this fix targets, and no new index
  or schema change is required to realize the fix's benefit.
- "Folder-scoped" listing requests (those with a folder set and not explicitly skipping the
  folder predicate) are the only requests this fix needs to change; requests with no folder
  predicate at all are out of scope and keep their current query shape untouched.
- The explicit-site and forced-system-host sub-cases are assumed to remain exactly as they are
  today. The "no host restriction" sub-case is the one deliberate exception (FR-004a): it gains
  the `host_inode` filter it was missing, correcting the cross-site over-return defect from
  #37148 — this fix reshapes how the folder predicate is evaluated and closes that one known
  correctness gap, not what site scoping applies in the other sub-cases.
- The prepared-statement plan-cache latency-bimodality mechanism flagged in spike #37183 is a
  separate, not-yet-confirmed concern and is explicitly not a prerequisite or a blocker for this
  fix — its resolution, if pursued, is tracked independently.
- Test-first development (Constitution Principle V) applies: acceptance and success criteria in
  this spec are written to be concrete enough that tests can be designed against them, written,
  developer-approved, and confirmed failing before any implementation change is made — the actual
  test design is deferred to the planning phase. Expected home for these tests:
  `BrowserAPITest` (integration, query-shape and result-set assertions) and `BrowserAPIImplTest`
  or equivalent (unit-level, if the CTE construction is unit-testable in isolation) — to be
  confirmed in the planning phase.
