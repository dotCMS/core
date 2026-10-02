# Feature Specification: Experiments List on the Server-Side Contract

**Feature Branch**: `oidacra/experiments-portlet-list-server-side-swap`

**Created**: 2026-08-26

**Status**: Draft

**Type**: Task (interim-to-final contract swap; no user-visible feature change intended)

**Epic**: [#36763 — Experiments: A/B Testing v2](https://github.com/dotCMS/core/issues/36763)

**Work item**: [dotCMS/core#37007 — Experiments Portlet — swap the List to the server-side contract](https://github.com/dotCMS/core/issues/37007)

**Input**: User description: "Swap the Experiments portlet list from the client-side interim to the server-side contract from #36823" — taken from issue #37007.

---

## Scope Note *(read this first)*

The Experiments portlet's site-wide list (#36989, merged via PR #37034) ships against
`GET /api/v1/experiments` **as it exists today**: the endpoint returns every experiment on every
site, and the list computes paging, sorting, text filtering, status filtering, status counts and
site scoping **client-side**, plus a bulk page lookup that resolves each experiment's page to a
URL and a site. That was an explicit, documented interim — the service method that fetches the
full set is annotated as "the single swap point" for this change.

This issue replaces that interim with the server-side contract defined by #36823: the list sends
`page`, `per_page`, `orderby`, `direction`, `filter`, `status` and `siteId` to the endpoint, reads
the total from the response body's `pagination.totalEntries`, and deletes the client-side
utilities that faked all of this. The URL parameter contract was designed to be final from day
one, so the swap changes **where** the parameters are applied — server instead of browser — not
what they are called, how they default, or how they deep-link.

The boundary is deliberate and is itself an acceptance criterion: only the data-access service
and the store's load path change. **No component should need touching.** If one does, #36989's
service boundary was drawn wrong — that is flagged in review, not patched around.

### Entry condition — verified 2026-08-26, NOT met

This spec was written against `origin/main` at commit `f2a25e925c`. Finding:

- **Issue #36823 is OPEN and its implementation is NOT on `main`.** No implementation PR exists
  (merged or open). On `main`, the list endpoint still accepts only `pageId`, `name` and `status`
  — no `page`/`per_page`, no `orderby`/`direction`, no `filter`, no `siteId`, no `pagination`
  block in the response, and no permission filtering.
- Consequently **#36823's two SQL fixes are also absent** (Bug A: `name`+`status` together emit
  malformed SQL; Bug B: `pageId` + two or more statuses lets every status after the first escape
  the scoping). Those fixes land as part of #36823's query rewrite; they cannot precede it.
- Additionally, #36823's **current issue body** excludes several things this issue consumes: row
  enrichment (page title/path/site on each row), per-status counts over the filtered set, a
  `goal` parameter, and sorting by page/goal/schedule. **Decision (coordinator, confirmed by the
  user, 2026-08-26): #36823's scope is expanded to include all of them** — no separate follow-up
  issues. #36823's body still reads the old way and needs updating (a backend/user action, not
  this spec's); until it is updated, this spec's Dependencies section is the authoritative
  statement of what this issue expects #36823 to deliver.

**Implementation of this issue MUST NOT start until the dependency state is re-verified**: the
expanded #36823 contract (pagination, sorting for every UI column, `filter`, `goal`, `siteId`,
creator and running-range filtering, both SQL fixes, permission filtering, row enrichment,
per-status and per-goal counts) must be on the target build. #37007 lands complete in one drop once expanded #36823 ships — no staged
delivery. The spec is written now so the swap is fully designed the moment the backend lands —
which is the point of filing #37007 together with #36989: the interim must not quietly become
permanent.

### Scope addition (user, 2026-08-27): two new filters

After the spec was first committed, the user added two capabilities that stakeholders asked for:
filtering the list **by the user who created an experiment**, and **by a running date range**
(experiments whose running window overlaps a chosen from/to range — orthogonal to the status
filter, which already answers "is it running now"). Both are new capabilities, not part of the
#36989 interim, so they extend this issue in three controlled ways: two new toolbar filter
controls (an explicit, enumerated exception to the no-component-change boundary), three new
additive URL parameters, and a further expansion of the #36823 backend contract. Everything else
about the swap's behavior-preserving framing stands: existing parameters, defaults and deep
links are untouched, and the new parameters appear in the URL only when the filters are used.

### What this issue is not

- Not a visual or interaction change **beyond the two new filter controls** added on 2026-08-27.
  The table, existing filters, empty states, row actions and their confirmations are untouched.
- Not an endpoint change. This is a pure consumer swap; the backend contract is #36823's
  (expanded).
- Not a change to the existing URL contract. Same parameter names, same defaults-omitted rule,
  same deep-link restoration; the three new parameters are additive and absent by default.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The list scales past what a browser download can carry (Priority: P1)

A content manager on an installation with thousands of experiments opens the Experiments portlet.
The list requests only the page being looked at, filtered, sorted and scoped by the server, and
arrives at interactive speed. Changing page, sort, search text, status filter or site issues one
request that carries the change as query parameters; the browser never downloads or processes the
full experiment set.

**Why this priority**: This is the entire reason the issue exists — the interim downloads every
experiment on every site on every load, plus a second bulk lookup, which stops scaling exactly
when Experiments adoption succeeds. It also closes the day-one exposure of the interim: the
un-scoped full listing is fetched by every portlet visitor.

**Independent Test**: With more experiments than one page can hold, open the list and observe a
single list request carrying the view's parameters; verify the response carries only one page of
rows and the pager reflects the full filtered total.

**Acceptance Scenarios**:

1. **Given** the portlet list in its default view, **When** it loads, **Then** exactly one list
   request is issued carrying the current site and the default paging/sorting, and the table
   renders only that page of rows.
2. **Given** a loaded list, **When** the user moves to page 3, **Then** one request is issued
   asking for page 3 and the rows shown are page 3 as the server ordered them.
3. **Given** a loaded list, **When** the user types a search term, **Then** one request (after
   the existing debounce) carries the term and the rows and total reflect the server's match over
   the whole set, not over previously loaded rows.
4. **Given** a loaded list, **When** the user selects statuses, sorts a column, or switches site,
   **Then** each change issues one request carrying the changed parameter and paging restarts
   where the current behavior restarts it today.
5. **Given** any view of the list, **Then** no client-side slicing, sorting, text filtering,
   status filtering or site narrowing runs over the experiment rows — the interim utilities that
   did so are deleted.

---

### User Story 2 - Nothing observable changes for the user who already had it working (Priority: P1)

A user who bookmarked a filtered, sorted, paged deep link — or who simply uses the list daily —
notices nothing. The URL reads and writes identically, defaults are still omitted from it,
back/forward still restores state, and the visible behavior of every filter, sort and pager
interaction is preserved.

**Why this priority**: The swap is only safe to land quietly if it is behavior-preserving. The
URL contract was designed to be final; regressing it breaks bookmarks and the E2E deep-link
coverage from #37006.

**Independent Test**: Run the existing deep-link E2E spec from #37006 unchanged against the
swapped list; exercise the URL matrix manually (each param present/absent, defaults, invalid
values) and compare with the pre-swap behavior.

**Acceptance Scenarios**:

1. **Given** the pre-swap list and the post-swap list, **When** the same sequence of filter,
   sort, page and site interactions is performed on both, **Then** the URL after every step is
   byte-identical between the two.
2. **Given** a deep link carrying `page`, `per_page`, `orderby`, `direction`, `filter`, `status`
   and/or `goal`, **When** it is opened, **Then** the list restores that exact view, as today.
3. **Given** a pristine default view, **Then** the URL carries no query parameters, as today.
4. **Given** the E2E deep-link spec from #37006, **When** it runs against the swapped list,
   **Then** it passes without modification.
5. **Given** a user pressing back/forward across view changes, **Then** the restored URL is
   re-applied to the list — under the swap this now includes refetching, since the view is no
   longer derivable client-side.

---

### User Story 3 - The Page column and the status counts tell the truth about the whole set (Priority: P2)

The Page column shows each experiment's real page title and path as delivered on the row itself,
and the status filter's per-status counts describe the entire filtered set — not just the rows
that happen to be loaded. The interim's second network round-trip (the bulk page lookup) and its
failure modes (rows silently hidden when the lookup misses a page) disappear.

**Why this priority**: Depends on the parts of the backend surface that #36823's original body
excluded and that its expanded scope now includes (row enrichment, count-by-status). Since
#37007 lands in one drop, this story ships with the others; it is P2 only because Stories 1 and
2 carry the scaling and compatibility value. It removes a whole class of interim defects: the
lookup fails closed, so an unresolvable page silently hides experiments today.

**Independent Test**: Load a list whose experiments span several pages and sites; verify the Page
column renders title/path from the list response itself with no secondary lookup request, and
that per-status counts equal the server's counts over the whole filtered set.

**Acceptance Scenarios**:

1. **Given** a loaded list, **Then** each row's Page column shows the enriched page title/path
   from the list response, and no secondary page-lookup request is made.
2. **Given** experiments with a matching search term spread across many pages of results,
   **When** the list shows page 1, **Then** the per-status counts reflect every match, not the
   visible page.
3. **Given** the interim page-lookup utilities and their failure-handling code, **Then** they are
   deleted, along with the "experiment hidden because its page did not resolve" behavior.

---

### User Story 4 - Finding experiments by who created them and when they ran (Priority: P2)

A content manager reviewing the program filters the list to one teammate's experiments
("everything created by X"), or to a period ("everything that was running in March"), or both
together with the existing search, status, goal and site filters. The counts, pager and URL
behave exactly like the existing filters: server-applied, whole-set totals, shareable deep link.

**Why this priority**: Added by the user on 2026-08-27 from stakeholder feedback. It rides the
same server-side contract as the rest of the swap — landing it separately would mean touching
the same load path, service and backend contract twice.

**Independent Test**: Seed experiments by several creators with distinct running windows;
filter by one creator, then by a range overlapping only some windows, then combine both; verify
rows, totals and counts match the seeded expectation and the URL restores each view.

**Acceptance Scenarios**:

1. **Given** experiments created by several users, **When** the user filters by one creator,
   **Then** only that creator's experiments are listed and the totals/counts reflect the whole
   filtered set.
2. **Given** experiments with different running windows, **When** the user picks a from/to
   range, **Then** only experiments whose running window overlaps the range are listed —
   regardless of their current status (an ENDED experiment that ran during the range matches).
3. **Given** a running-range filter active, **When** an experiment has never run and has no
   schedule, **Then** it does not match; **and** an experiment still running (no end date)
   matches any range that overlaps its start-to-now window.
4. **Given** creator and range filters set, **When** the URL is shared and reopened, **Then**
   the same view is restored; **and Given** neither filter is set, **Then** neither parameter
   appears in the URL.
5. **Given** both new filters plus search, status, goal and site, **When** combined, **Then**
   they compose as intersections, server-side, in one request.

---

### Edge Cases

- **Empty status selection vs. the server's default**: today an empty status selection means
  "everything except Archived" (Archived is opt-in), while #36823's endpoint treats "no `status`
  param" as "every status". The swap must translate the empty selection into an explicit
  non-archived status set on the wire, or the default view silently starts showing archived
  experiments. The URL (no `status` param) must not change either way.
- **No current site yet**: today the list fails closed (renders nothing) until the current site
  is known. #36823's endpoint treats an omitted `siteId` as "all sites". The swap must not fetch
  (or must not render) before the site is known — omitting `siteId` by accident widens the list
  to every site.
- **Sort fields beyond #36823's original whitelist**: the URL/sort contract exposes `name`,
  `page`, `goal`, `schedule`, `status`, `modDate`; #36823's original whitelist covered only
  `name`, `status`, `creation_date`, `mod_date`. The expanded #36823 scope covers every UI sort
  field; the load path translates naming (`modDate` → server naming) without touching the URL.
  See FR-010.
- **Page beyond the last page**: a deep link to `page=99` of a two-page result. #36823 returns an
  empty page with the correct total; the list must render that state the way it renders an empty
  page today (and the URL keeps `page=99`, as today).
- **Last row of the last page acted on**: archiving/deleting the only row of the last page
  triggers the existing reload; the reload now happens server-side and may land on an empty page.
  Behavior must match today's (the current implementation reloads and re-derives; the page number
  is preserved).
- **Search matching a page path**: the server's `filter` matches experiment name OR page path,
  case-insensitively — same as the column the user reads. The interim also matched the
  experiment description; the server does not. See Assumptions.
- **Invalid URL values** (unknown status, non-numeric page, unknown orderby): parsed and
  defaulted exactly as today — the parsing utilities are part of the URL contract and stay.
- **Site switch mid-flight**: a site switch during an in-flight load must cancel/supersede the
  stale request (the current load path already cancels; the swap keeps that property so a slow
  response for site A cannot overwrite site B's list).
- **Analytics health gate**: nothing is fetched until the health gate passes, exactly as today —
  the swap changes what the load requests, not when loading is allowed.
- **Running-range boundaries**: a range whose `from` equals a window's end (or `to` equals a
  start) still matches — bounds are inclusive, calendar-date granular. A `from` after `to` is
  rejected/normalized by the control, never sent. An experiment with several separate runs
  matches if **any** run overlaps the range.
- **Creator no longer resolvable**: a `created_by` value naming a deleted or unknown user simply
  matches nothing — an empty page with zero totals, not an error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every change of the list's view state (page, page size, sort field, sort
  direction, search text, status selection, goal selection, creator selection, running date
  range, current site) MUST result in exactly one list request whose query parameters express
  that view state using the server-side contract: `page`, `per_page`, `orderby`, `direction`,
  `filter`, `status`, `goal`, `created_by`, `running_from`, `running_to`, `siteId`.
- **FR-002**: The list MUST NOT slice, sort, text-filter, status-filter, goal-filter or
  site-narrow the experiment rows client-side. The interim utilities that did so (client
  comparators, paging slices, search/status/goal/site predicates over the loaded array) MUST be
  deleted, not bypassed.
- **FR-003**: The browser URL contract MUST remain byte-identical to the pre-swap behavior for
  every pre-existing parameter: same names and value formats, same "defaults are omitted" rule,
  same deep-link restoration, same back/forward behavior. The deep-link E2E coverage from
  #37006 MUST pass unchanged. The three new parameters (`created_by`, `running_from`,
  `running_to`) are additive: absent unless their filter is active, and following the same
  defaults-omitted and deep-link rules.
- **FR-004**: The total row count driving the pager MUST come from the list response body's
  pagination total (`pagination.totalEntries`), not from response headers and not from the
  length of any loaded array.
- **FR-005**: The Page column MUST render the page title/path enrichment carried on the list
  rows themselves; the interim bulk page lookup (its request, its state, its fail-closed
  hiding of unresolved rows and its diagnostics) MUST be deleted.
- **FR-006**: The per-status counts shown by the status filter MUST come from the server and
  describe the whole filtered set (every active filter applied except the status selection
  itself, as today), not the loaded page. The same rule holds for per-goal counts relative to
  the goal selection.
- **FR-007**: Site scoping MUST use the `siteId` parameter carrying the current site; switching
  site MUST refetch with the new `siteId` and restart paging, preserving search/sort/status
  selections, exactly as the current behavior does. The list MUST NOT issue a request without
  `siteId` before the current site is known.
- **FR-008**: The default view (empty status selection) MUST continue to exclude Archived
  experiments and MUST continue to produce a parameter-free URL. Whatever the load path sends on
  the wire to achieve that (e.g. an explicit status set) MUST NOT leak into the URL.
- **FR-009**: The swap's diff MUST touch only: the shared experiments data-access service, the
  list store's load path (the event handlers/reducers that fetch and fold in the list), and
  deletions of interim code (utilities, constants, state fields, and their tests). The **only**
  component-level changes permitted are the two new filter controls of FR-015/FR-016 (their
  toolbar UI plus the store state, events and URL wiring they need). Any other component,
  template or style change is raised in review as a service-boundary defect of #36989 — not
  silently patched.
- **FR-010**: Sorting MUST be applied by the server for every sort the UI offers (`name`,
  `page`, `goal`, `schedule`, `status`, `modDate`). The expanded #36823 scope grows the server
  whitelist to cover all of them (decision: coordinator, confirmed by the user, 2026-08-26).
  Where the UI's sort fields and the server's differ only in naming (`modDate` vs `mod_date`),
  the load path MUST translate; the URL keeps the UI's names. No column loses sorting and no
  column stays client-sorted.
- **FR-011**: The goal filter (`goal` URL parameter, goal chips and per-goal counts) MUST keep
  its current observable behavior, applied server-side. The expanded #36823 scope adds a `goal`
  parameter and per-goal counts over the whole filtered set (decision: coordinator, confirmed by
  the user, 2026-08-26). The load path sends the goal selection as a request parameter exactly
  as it does statuses; no goal predicate runs client-side.
- **FR-012**: Row enrichment (FR-005) and per-status counts (FR-006) MUST be consumed from the
  expanded #36823 contract, which now includes both (decision: coordinator, confirmed by the
  user, 2026-08-26 — no separate follow-up issue). #37007 lands complete in one drop once the
  expanded #36823 ships; there is no staged delivery. Note: #36823's issue body still lists both
  as out of scope and needs updating (a backend/user action); this spec's Dependencies section
  states the expanded expectation explicitly.
- **FR-013**: The existing load-adjacent behaviors MUST be preserved unchanged: the Analytics
  health gate owns the first fetch; every successful row action (archive, delete, end, abort,
  cancel schedule) reloads through the same load path; a failed row action leaves the list
  usable; load errors surface through the shared HTTP error handling.
- **FR-014**: The list store's unit specs MUST be updated so the load path asserts the exact
  outgoing query parameters for each view state (defaults, each filter individually — search,
  status, goal, creator, running range, site — combined filters, site switch, empty status
  selection, page resets on filter/sort change), and the deleted utilities' specs MUST be
  deleted with them.
- **FR-015** *(added by the user, 2026-08-27)*: Users MUST be able to filter the list by the
  experiment's **creator**. The filter is applied server-side via a `created_by` parameter, is
  mirrored to the URL only when active, resets paging when changed (like every other filter),
  and composes with all other filters as an intersection.
- **FR-016** *(added by the user, 2026-08-27)*: Users MUST be able to filter the list by a
  **running date range** (`running_from` / `running_to`, calendar dates, inclusive). An
  experiment matches when any of its running windows overlaps the range — actual run periods
  when it has run (an open-ended run extends to the present), the scheduled window when it has
  not. The filter is orthogonal to status: it answers "what ran (or will run) during this
  period", while status answers "what state is it in now". Either bound MAY be used alone
  (open-ended range). Applied server-side; mirrored to the URL only when active; resets paging;
  composes as an intersection.

### Key Entities

- **Experiment list row**: one experiment as listed — name, status, goal, schedule, page
  reference; after the swap it additionally carries the page's human-readable title/path and its
  site, delivered by the server rather than looked up by the client.
- **List view state**: the user-controlled view — page, page size, sort field and direction,
  search text, status selection, goal selection, creator selection, running date range —
  mirrored to the URL (defaults omitted) and, after the swap, expressed as query parameters on
  every list request instead of applied in the browser.
- **Running window**: the period(s) an experiment actually ran (it may have several, and an
  open-ended one extends to the present) or, when it has not run yet, its scheduled window. The
  running-range filter matches on overlap with any of them. An experiment with neither never
  matches an active range filter.
- **Pagination envelope**: the server's description of the result set — current page, page size
  and total entries — which becomes the single source of the pager's total.
- **Status counts**: per-status totals over the whole filtered set (site + search applied),
  displayed by the status filter chips, sourced from the server after the swap.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Opening the list transfers one page of experiments, never the full set: on an
  installation with 10,000 experiments, the list request payload and time-to-interactive are
  indistinguishable from an installation with 100 (identical `per_page`), where today load
  scales linearly with the total.
- **SC-002**: Every view change (page, sort, search, status, site) produces exactly one list
  request and zero secondary lookups — down from one full-set request plus one bulk page lookup
  today.
- **SC-003**: The E2E deep-link suite from #37006 passes with zero modifications, and a URL
  captured before the swap restores the same view after it, for the full parameter matrix.
- **SC-004**: 100% of the interim client-side list utilities (paging slice, comparators, search
  and status and site predicates, bulk page lookup and its state) are deleted from the codebase
  — none remain reachable or exported.
- **SC-005**: Status counts and the pager total are correct for filtered sets larger than one
  page (counts equal the server's totals, verified against a seeded set), where today they are
  only as correct as the downloaded array.
- **SC-006**: The review diff contains changes to exactly two production areas (data-access
  service, store load path) plus deletions and the two new filter controls of FR-015/FR-016;
  no other component/template/style file is modified.
- **SC-007**: Filtering by creator, by running range, or both restores correctly from a shared
  URL and returns whole-set totals/counts, verified against a seeded matrix of creators and
  running windows (including an open-ended run and a never-run draft).

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: the Experiments portlet list (new in #36989) and the shared
  experiments data service that also serves the legacy per-page Experiments UI inside the page
  editor. The legacy UI's calls (per-page listing by `pageId`, by `pageId`+status) MUST keep
  working unchanged — #36823 preserves those parameters, and this swap must not alter or remove
  the service methods the legacy UI uses.
- **Backward-compatibility expectations**: the portlet's URL contract is public behavior
  (bookmarks, shared links) and must not change. The endpoint itself is consumed more strictly
  but not modified. No data migration, no rollback-unsafe surface: rolling back the frontend
  swap restores the interim, which keeps working against the upgraded endpoint (the new
  parameters are all optional).
- **Known related decisions**: #36989's review record established the interim explicitly and
  named the data-access service as the single swap point; #36823 defines the target contract
  including its security fix (permission filtering) — after the swap the list shows only
  experiments whose page the user can edit, which is a deliberate behavior change owned by
  #36823, not by this issue. The plan phase will formally consult `dotCMS/platform-adrs`.

## Assumptions

- **Search field narrowing is accepted**: the server's `filter` matches experiment name OR page
  path; the interim also matched the experiment description. The swap adopts the server's
  semantics — searching by description stops matching. This follows the issue's "server-side
  params from #36823" scope; if description search must survive, that is a backend contract
  change, not a client one.
- **Permission narrowing is accepted and owned by #36823**: post-swap, users see only
  experiments on pages they can edit, and totals/counts reflect that. Today's interim shows
  every experiment to every portlet visitor. This spec treats the new behavior as correct.
- **The empty-status default is expressed on the wire, not in the URL**: to keep "Archived is
  opt-in" (FR-008) the load path sends an explicit status set when the selection is empty. The
  alternative — the server adopting the same default — would be a #36823 contract change; this
  spec assumes the client translation.
- **`direction` values and casing** on the wire follow #36823/`PaginationUtil` conventions; the
  URL keeps its current values. Any mismatch is translated in the load path, never surfaced.
- **The reload-after-action and health-gate flows are load-path concerns** and therefore inside
  this issue's boundary even though their observable behavior must not change.
- **New-filter details chosen as reasonable defaults** (flag in review if wrong): "user" means
  the experiment's **creator** — the field the API already returns per row — not its last
  editor; the URL parameter names are `created_by`, `running_from`, `running_to` (matching the
  existing lowercase/snake_case parameters); range values are calendar dates (ISO `YYYY-MM-DD`),
  inclusive at both bounds; timezone interpretation follows the backend contract and is settled
  in the plan phase. How the creator control presents users (free id, picker, suggestions) is a
  design detail for the plan phase — the spec only fixes the filter's semantics.
- **Entry condition re-verification is a hard gate**: the findings in the Scope Note (contract
  absent from `main` as of 2026-08-26) are expected to change; `/speckit-plan` MUST re-verify
  the dependency state and MUST NOT proceed to implementation planning against an absent
  contract.

## Dependencies

- **#36823, expanded scope** (open as of 2026-08-26; **blocking for implementation, not yet on
  `main`**): the server-side contract this issue consumes. The expansion was decided by the
  coordinator and confirmed by the user on 2026-08-26; #36823's issue body predates it and still
  lists some of these as out of scope (updating the body is a backend/user action). This issue
  expects #36823 to deliver, in full:
  - Pagination (`page`, `per_page`) with `pagination.totalEntries` in the response body.
  - Sorting (`orderby`, `direction`) covering **every UI sort field**: name, page, goal,
    schedule, status, modification date.
  - Canonical free-text `filter` (name OR page path, case-insensitive).
  - `status` (repeatable) and a **new `goal` parameter** (repeatable) with the same
    whole-filtered-set semantics.
  - **Creator filtering** (`created_by`, exact match on the experiment's creator) and
    **running date range filtering** (`running_from`/`running_to`, overlap semantics per
    FR-016) — added to the expectation by the user on 2026-08-27.
  - `siteId` site scoping.
  - Permission filtering with totals/counts that agree with it.
  - The two SQL fixes (Bug A: `name`+`status` malformed SQL; Bug B: `pageId`+multi-status
    precedence).
  - **Row enrichment**: page title/path/site on each list row.
  - **Per-status and per-goal counts** over the whole filtered set.
- **#36989 / PR #37034** (merged): the interim list this issue replaces, including the URL
  contract and the swap-point service method.
- **#37006**: the deep-link E2E spec that acts as the unchanged regression gate for FR-003.
