# Phase 0 Research: Content Drive Status Filter

**Feature**: [spec.md](./spec.md) | **Issue**: [#37066](https://github.com/dotCMS/core/issues/37066) | **Date**: 2026-08-24

Every finding below was verified against the code at `origin/main` (`e46da2b187`), not inferred
from the issue text. Line numbers are from that commit.

---

## R1: What exists today for each of the three statuses

**Decision**: Two of the three are net-new; the third exists in a form that answers a different
question and must be left alone.

**Findings**:

| Status | Present in `BrowserQuery`? | Detail |
|---|---|---|
| Archived | Partially, and **inclusive** | `showArchived` (`BrowserQuery.java:55`, builder `:470`) merely *skips* `appendExcludeArchivedQuery` (`BrowserAPIImpl.java:2006`). It returns archived content **plus everything else** — the opposite of what an "Archived" chip means. |
| Unpublished | No | `showWorking` and the form's `live` flag select *which version* to show. That is not the inverse of "has a live version". |
| Locked | No | No field, no builder method, no SQL clause, no ES clause. Nothing under `com.dotcms.rest.api.v1.drive` mentions `locked`. |

Confirmed against the full field list (`BrowserQuery.java:44-83`) and all builder methods.

**There is no raw query passthrough to lean on**: `BrowserQuery.luceneQuery` (`:66`) is written
only by `withFilter` / `withFileName` and is never read by `BrowserAPIImpl`. The clauses must be
built explicitly.

**Rationale for keeping `showArchived` as-is**: the legacy Site Browser's "Show Archived" checkbox
(`view_browser.jsp:145`) depends on the inclusive meaning. Both questions are legitimate and both
must stay expressible, so the exclusive behavior is added *alongside* rather than replacing.

**Alternatives considered**: redefining `showArchived` to be exclusive and adding an
`includeArchived` for the legacy path. Rejected — it inverts the meaning of a flag with callers
outside this feature's blast radius, for no gain.

---

## R2: AND, not OR — and why that differs from every other Content Drive filter

**Decision**: Selected statuses combine with **AND**.

**Rationale**: the three are independent boolean facts about the *same* `contentlet_version_info`
row. An item can be unpublished *and* locked at once, so intersecting them is meaningful and is the
driving use case (US4). The existing multiselects — base type, content type, language — are OR
because each is a *single-valued* attribute: an item has exactly one base type, so an AND across
two would always be empty.

**Precedent**: the legacy Content Search portlet has always combined these three additively
(`ContentletAjax.java:1010-1021`):

```java
if (!showDeleted) "+deleted:false" else "+deleted:true"
if (filterLocked)    "+locked:true"
if (filterUnpublish) "+live:false"
```

Its UI exposes them as a mutually exclusive dropdown (`view_contentlets.jsp:695-698`), but that was
a presentation simplification, never a domain constraint. Content Drive keeps the additive backend
and gives it a control that can actually express it.

**Consequence**: each status is a flat, independent SQL clause. No composed OR group is needed, so
this lands well under the footprint of the archive-step work (`f92f939296`, 184 impl lines).

---

## R3: The DB predicates

**Decision**:

| Status | SQL clause | ES term |
|---|---|---|
| Archived | `cvi.deleted = <DBTrue>` | `+deleted:true` |
| Unpublished | `cvi.live_inode is null` | `+live:false` |
| Locked | `cvi.locked_by is not null` | `+locked:true` |

**Verification**: all three columns exist on `contentlet_version_info` (`postgres.sql:550-552`:
`live_inode varchar(36)`, `deleted bool not null`, `locked_by varchar(100)`). The `locked` field is
mapped into the index (`ESMappingAPIImpl.java:527`). `cvi` is already the alias in
`buildSelectBaseQuery` (`BrowserAPIImpl.java:2040`), and `DbConnectionFactory.getDBTrue()/getDBFalse()`
is the established way to write a boolean literal in this file.

**Alternatives considered**: `live_inode <> working_inode` for Unpublished. Rejected — that is
"has unpublished changes", a third distinct question, and it is false for content that was never
published at all.

---

## R4: The `showWorking` derivation must learn about the new statuses

**Decision**: `BrowserQuery`'s constructor line

```java
this.showWorking = builder.showWorking || builder.showArchived;   // :151
```

must also be true when `ARCHIVED` or `UNPUBLISHED` is selected.

**Rationale**: `selectQuery` (`:1947`) picks the joined inode column from this flag:

```java
final String workingLiveInode = browserQuery.showWorking || browserQuery.showArchived
        ? "working_inode" : "live_inode";
```

and the base query joins `c.inode = cvi.<that column>` (`:2043`). Archived and unpublished rows have
**no live version by definition**, so under `live_inode` the join can never match and the filter
would silently return nothing. The same flag drives `buildPureESQuery`'s `+working:true` vs
`+live:true` (`:615`), where `+live:true` alongside `+live:false` would be self-contradicting.

The Content Drive path happens to be safe today (the form's `live()` defaults to `false`), but the
flag has to be correct for **any** caller of `BrowserQuery`, and relying on a coincidence in one
caller is exactly the kind of drift ADR-0018 was written to stop.

---

## R5: Two existing branches already have an opinion about `cvi.deleted`

This is the only genuinely delicate part of the change. Both branches must learn about `ARCHIVED`.

### R5a — The global archived exclusion

Today (`BrowserAPIImpl.java:2006`):

```java
if (!browserQuery.showArchived && archiveStepIds.isEmpty()) {
    appendExcludeArchivedQuery(selectQuery);   // and cvi.deleted = false
}
```

**Decision**: add `&& !statuses.contains(ARCHIVED)`.

Without it, `cvi.deleted = false` **and** `cvi.deleted = true` would both be emitted and `ARCHIVED`
would return nothing, always. With it, `UNPUBLISHED`/`LOCKED` selected alone still keep the
exclusion — which is precisely FR-004's "excludes archived content unless Archived is also
selected". The requirement and the code shape line up exactly; no extra logic is needed to get it.

### R5b — Archive-target workflow steps

`archiveStepIds` is deliberately emptied when `showArchived` (`:1980`), and the existing comment
says why: `appendWorkflowQuery` otherwise **owns** `cvi.deleted` per branch (`:2342-2360`), forcing
`cvi.deleted = false` on the live branch and hiding the archived content the caller asked for.

**Decision**: `ARCHIVED` gets identical treatment:

```java
final boolean admitsArchived = browserQuery.showArchived || statuses.contains(ARCHIVED);
final Set<String> archiveStepIds = admitsArchived
        ? Set.of()
        : resolveArchiveTargetSteps(browserQuery.workflowStepIds);
```

This is the FR-011 / `ContentDriveWorkflowArchiveStepTest` regression case. It reuses the mechanism
the archive-step work already built rather than inventing a second reconciliation, and with no
status selected every generated query stays **byte-identical**.

**Alternatives considered**: folding the status clauses into `appendWorkflowQuery`'s branch
structure. Rejected — status and workflow are orthogonal filters, and coupling them would make each
one harder to reason about for no behavioral gain.

---

## R6: Which query path actually runs, and why both must be patched

**Decision**: implement in the SQL path (`selectQuery`) **and** in `buildPureESQuery`.

**Findings**: `doElasticSearchTextFiltering` (`:479`) switches on `BROWSE_API_HEURISTIC_TYPE`,
defaulting to `HYBRID_SINGLE_CHUNKED_QUERY_ES` (`:697`).

- Under the **default hybrid** heuristic the SQL query supplies the ordered candidate inode set and
  the index only narrows each chunk by the text term. So the SQL clauses apply **with or without**
  text in the search box — this is what satisfies FR-009's "identical with and without a keyword".
- `buildPureESQuery` — with its hardcoded `+deleted:false` at `:612` — runs **only** under
  `BROWSE_API_HEURISTIC_TYPE=PURE_ES`.

`PURE_ES` is not the default and ADR-0018 says it must not become one, but it is a **supported
configuration**. Leaving it unpatched would make all three filters silently no-op there, returning a
wider set than the user asked for. That is FR-009 and SC-005.

---

## R7: Rejecting an invalid status value

**Decision**: the form declares `List<String> status()`; `ContentDriveHelper` parses it into the
enum and throws `BadRequestException` naming the accepted values.

**Rationale**: FR-010 asks for a 400 "consistent with how the drive already rejects unknown
field-filter keys", and that precedent is an explicit `BadRequestException` thrown in
`ContentDriveHelper.driveSearch` (the `userSearchable` guard, ~line 180). Keeping the same shape
means one error path, one message style, and a status code we control directly.

**Alternatives considered**: declaring the field as `List<ContentStatus>` and letting Jackson
reject. Rejected — deserialization failures surface as `InvalidFormatException` from the immutables
layer, whose mapping to a 400 with a useful message is less direct than throwing it ourselves. The
typed form field is marginally prettier; the deterministic error is worth more.

---

## R8: Where the new enum lives

**Decision**: `com.dotcms.browser.ContentStatus` — `ARCHIVED`, `UNPUBLISHED`, `LOCKED`.

**Rationale**: `BrowserQuery` is the real consumer, and `com.dotcms.browser` already holds this
kind of query-shaping type (`FieldSearchCriteria`, with its own `RoutingBucket` enum). Placing it in
`com.dotcms.rest.api.v1.drive` would make the browser layer depend on the REST layer.

Constitution Principle I is satisfied: entirely modern `com.dotcms.*`, nothing added to
`com.dotmarketing.*`.

---

## R9: Frontend — reuse, don't rebuild

**Decision**: model the control on the workflow filter, but single-column and static.

**Findings** — everything needed already exists:

| Need | Existing thing to reuse |
|---|---|
| Chip + active state + overflow label | `DotChipFilterComponent` (`@dotcms/ui`), `mode="dropdown"` |
| Popover + listbox styling | `CHIP_FILTER_POPOVER_PT`, `CHIP_FILTER_LISTBOX_PT`, `PANEL_SCROLL_HEIGHT` |
| Row label with truncation | `DotFilterListItemComponent` |
| Read / write / clear a filter | store `getFilterValue` / `patchFilters` / `removeFilter` |
| URL encode | `encodeFilters` — already comma-joins any array value, no change needed |
| URL decode | one line in `decodeByFilterKey`: `status: multiSelector` |
| "Clear all" visibility + clearing | `hasNonDefaultFilters` / `clearFilters` — both work unchanged |

The workflow filter carries a service, two caches, a request-id guard and a reconcile pass, because
its options are fetched and can disappear between loads. The status options are a fixed set of
three. **None of that machinery should be copied** — the component is a `linkedSignal` over the
filter value plus a checkbox list.

**Decision on defaults**: `status` is deliberately **not** added to `withFilterDefaults`. Unlike
`languageId` and `sharedAssets` — where "absent" is not a neutral state and must be seeded — an
empty status set genuinely means "no status filtering". Leaving it unseeded makes
`hasNonDefaultFilters` return `true` whenever a status is present (so "Clear all" appears) and makes
`clearFilters()` drop it, both for free.

---

## R10: Removing the hardcoded `archived: false`

**Decision**: delete the `archived: false` pin from `$request` in `dot-content-drive.store.ts:135`
and send `status` instead.

**Verification**: safe to simply remove — `AbstractDriveRequestForm.archived()` is
`@Value.Default default boolean archived() { return false; }`, so omitting it from the payload
produces the identical query. This is FR-019, and it is what stops the request from contradicting
the user's Archived selection.

---

## Open questions

**None.** No `NEEDS CLARIFICATION` markers were carried out of the spec, and every technical
uncertainty above was closed by reading the code rather than by assumption.
