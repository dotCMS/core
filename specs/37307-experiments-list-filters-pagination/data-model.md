# Phase 1 Data Model

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

No persisted entities. Everything below is either an in-memory view model or a wire shape already
served by an existing endpoint. Validation rules are stated where a requirement depends on them.

---

## Experiment (client view) — extended

`core-web/libs/dotcms-models/src/lib/dot-experiments.model.ts`

Two fields are added to the existing `DotExperiment` interface. Both are already served or will be
by #37304; neither exists on the client type today, which is the gap "State of the code as found"
row 5 records.

| Field | Type | Source | Used by |
|---|---|---|---|
| `createdBy` | `string` | Already serialized by `AbstractExperiment` | Creator narrowing (FR-005) |
| `createdByUserName` | `string` | Added by #37304; never null or empty, falls back to the raw id for an unresolvable creator | Created By column (FR-029) |

`createdByUserName` is typed optional until #37304 has shipped everywhere the portlet runs against,
because the column must tolerate an older backend (FR-031).

Existing fields this feature reads: `scheduling: RangeOfDateAndTime | null`, whose
`startDate: number | null` is an epoch. **Both** shapes mean "unscheduled" for the window filter —
a null `scheduling`, and a `scheduling` whose `startDate` is null (FR-022).

## Schedule window

A closed set of five values, one of which is the absence of a constraint.

| Value | Lower bound | In the address |
|---|---|---|
| none (default) | no constraint | absent |
| last 1 month | `subMonths(now, 1)` | present |
| last 3 months | `subMonths(now, 3)` | present |
| last 6 months | `subMonths(now, 6)` | present |
| last 12 months | `subMonths(now, 12)` | present |

Rules: at most one is in force (FR-018); the comparison is `startDate >= lowerBound` with no upper
bound, so future starts match every window (FR-020); the bound is recomputed at evaluation time, so
it drifts with the clock (spec assumption 4); `subMonths` clamps to the last valid day of a shorter
month, which is the FR-021 decision. Unrecognised values in the address are dropped (FR-048).

## Directory person (chip option)

| Field | Type | Notes |
|---|---|---|
| `value` | `string` | The user id. This is what the address stores and what narrowing compares against. |
| `label` | `string` | Display name. Never used for matching. |

Carries no count, deliberately (FR-010). Labels are cached across pages and searches by the shared
option list, so a person selected on page 1 stays labelled after a search resets the list (FR-009).

## Listing view state — extended

`DotExperimentsListViewState`, the bag the address serialises and every narrowing reads.

| Existing | `filter`, `selectedStatuses`, `selectedGoals`, `page`, `perPage`, `orderBy`, `direction`, `selectedPageId`, `selectedPageUrl`, `languageId` |
|---|---|
| **Added** | `selectedCreators: string[]` — user ids, empty means no constraint |
| **Added** | `selectedScheduleWindow` — one of the five values, default = none |

Every field in this bag is a query parameter (FR-045). Transient interface state is deliberately
**not** here: the search text inside the Created By popover narrows the option list rather than the
data and never reaches the address (FR-045a), and neither does the resolution-in-flight state
behind the chip's loading indicator (FR-009e).

## Narrowing chain — where the new links go

```
siteScopedExperiments
  -> searchedExperiments
  -> pageAssetFilteredExperiments
       |__ statusCounts, goalCounts     <-- counts are snapshotted HERE
  -> statusFilteredExperiments
  -> goalFilteredExperiments
  -> creatorFilteredExperiments         <-- NEW
  -> scheduleFilteredExperiments        <-- NEW
  -> filteredExperiments  ->  sortedExperiments  ->  pagedExperiments
```

Both new links sit **after** the counts snapshot, alongside status and goal. Placing either earlier
would move the Status and Goal chip numbers, which FR-050 forbids. Order between the two is
irrelevant to the result — all narrowings compose as a conjunction (FR-007, FR-023) — but is fixed
here so the chain reads in one direction.

`totalRecords` already derives from `filteredExperiments().length`, so it follows the new filters
with no change (FR-041).

## State transitions

Both filters change state only through dispatched events, like every other narrowing on this
screen. Each new event resets `page` to its default in the reducer, exactly as `filterChanged`,
`statusesChanged` and `goalsChanged` already do (FR-041).

| Event | Effect |
|---|---|
| creators changed | `selectedCreators` replaced, `page` reset |
| schedule window changed | `selectedScheduleWindow` replaced, `page` reset |
| hydrated from URL | both fields replaced from the parsed address |
| site changed | both survive, like search, status and goal; only paging and page scope reset (spec edge case) |
| page narrowing cleared / clear filters | both cleared |
