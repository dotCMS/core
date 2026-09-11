# Feature Specification: Experiments Listing — Created By Filter, Schedule Time Filter, Created By Column, Content Drive-Aligned Pagination

**Feature Branch**: `issue-37307-experiments-list-filters-pagination`

**Created**: 2026-09-10

**Status**: Draft

**Type**: Task (four additive changes to the new Experiments listing screen)

**Epic**: [#36763 — Experiments: A/B Testing v2](https://github.com/dotCMS/core/issues/36763)

**Work item**: [dotCMS/core#37307 — Experiments listing: user and time filters, Created By column, and Content Drive-aligned pagination](https://github.com/dotCMS/core/issues/37307)

**Input**: User description: "Experiments listing: user and time filters, Created By column, and Content Drive-aligned pagination" — taken from issue #37307, which collects feedback from the Global Sprint demo of the new Experiments portlet.

---

## Scope Note *(read this first)*

Four separate additions to one screen — the Experiments listing. They share a screen and a state
owner, and nothing else. Each is independently shippable and independently valuable, and one of
them is blocked on another team's work.

1. **Created By filter** — narrow the list to experiments created by one or more chosen people.
   The people come from the whole user directory, not just from the creators present in the list.
2. **Schedule time filter** — narrow the list to experiments whose scheduled start falls inside a
   chosen window. Single-select, five fixed options, lower bound only.
3. **Created By column** — show the creator's name in the table. **Blocked by
   [#37304](https://github.com/dotCMS/core/issues/37304)**, which adds the name to the API payload.
   The two filters are not blocked: creator matching uses the user id the payload already carries.
4. **Pagination alignment** — make the listing's paginator behave like Content Drive's, so two
   listing screens in the same admin do not disagree about what a page is.

Two boundaries are worth stating up front.

**This is not the server-side list swap.** Narrowing stays client-side, over the full set the
listing already holds. `GET /v1/experiments` returns every experiment in one response and accepts
only `pageId`, `name` and `status`, so there is nothing to push to the server without changing that
endpoint — which is [#37007](https://github.com/dotCMS/core/issues/37007)'s job, itself parked on
[#36823](https://github.com/dotCMS/core/issues/36823). This feature therefore extends the existing
client-side narrowing chain. Requirement FR-052 records what #37007 has to preserve when it lands.

**This is not a rewrite of the paginator.** The listing already paginates, and it already agrees
with Content Drive on the two things the issue asks it to adopt (see "State of the code as found"
below). What is left is three smaller behaviors, one of which turns out to be a no-op and one of
which cannot be copied literally. All three are specified as behavior, not as attributes.

---

## State of the code as found

The issue describes the current code in several places. Six of those descriptions no longer match
what is on the branch, and one is true of the API but not of the client. They are recorded here
because they change what the work is, not just how it is done. Requirements below are written
against the code, not against the issue.

| # | Issue says | Code says | Effect on scope |
|---|---|---|---|
| 1 | Default rows per page is 25; adopt 20 | Default is already **20** | No work. The constant already carries a docblock citing Content Drive as its source |
| 2 | Rows-per-page options are 10/25/50; adopt 20/40/60 | Already **20/40/60** | No work, same constant, same docblock |
| 3 | Bind `lazyLoadOnInit` to the lazy flag "so the first page loads once and not twice" | The table is unconditionally lazy, and the framework default for that flag is already on | No behavior change available here. The described defect does not exist |
| 4 | Match Content Drive's paginator visibility rule | Content Drive's rule is "always show while lazy, otherwise show only above one page". The Experiments table **is** lazy, so copying the rule literally shows the paginator always — the opposite of the issue's own acceptance criterion | Adopt the **intent** (hide on a single page), not the expression. See FR-037 |
| 5 | Creator matching uses `createdBy`, "which the API already returns" | True at the wire: the backend serializes `createdBy`. **Not** true of the client's experiment type, which declares no such field | The filter needs a model change, not only store work. See FR-005 |
| 6 | Expect a new sibling component for the user popover, since no existing one does search + infinite scroll | A shared component that does exactly this already exists and is a public export of the shared UI library: server-side search, debounce, virtual-scroll infinite paging, multi-select checkboxes, no counts, a label cache that keeps earlier selections labelled, an explicit "no more pages" stop, and distinct loading/empty/error states | Reuse rather than write. Two divergences to settle — see FR-013 and FR-014 |
| 7 | Search "resets to page 0" | The users endpoint's page parameter is 1-based; a value of 0 is coerced to the first page | Wording only. "Resets to the first page" is the requirement |

Three further facts about the code that the issue does not mention, and that requirements depend on:

- **The chip counts are computed before the status and goal narrowing**, deliberately, so that
  picking a status never moves the numbers beside the statuses you have not picked. Both new
  filters must sit on the same side of that snapshot as status and goal, or they would move those
  numbers. See FR-050.
- **A column is not only a column.** The table's skeleton row draws a fixed number of placeholder
  cells, and the table's minimum width is documented in the source as the sum of its fixed column
  widths plus a floor for the elastic Name column. Both are commented "keep in step"; a column
  added without updating them shrinks Name below its floor and draws one placeholder cell too few.
  See FR-033 and FR-034.
- **The URL is written by exactly one function and read by exactly one function**, and a value
  equal to its default is written as absent so a pristine listing has no query string at all. Both
  new filters have to join that contract on both sides. See FR-045 through FR-049.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find the experiments a given person created (Priority: P1)

An editor opens the Experiments listing on a site with more experiments than fit on a screen. They
want the ones a particular colleague set up — to review that person's work, or to find the
experiment they were told about by name of author rather than by name of experiment. They open a
Created By chip, type part of the colleague's name, tick them, and the table narrows. They tick a
second colleague and the table widens to both people's experiments. The chip's remove control puts
the list back.

**Why this priority**: The most-requested of the four at the demo, and the only one that answers a
question the screen cannot answer at all today. It is also unblocked.

**Independent Test**: Ship the Created By chip alone. Fully testable by selecting people and
observing which rows survive, with the other three parts absent.

**Acceptance Scenarios**:

1. **Given** a listing with experiments from several creators, **When** the Created By chip is
   opened, **Then** the first page of users from the directory is offered, including users who
   have created no experiments.
2. **Given** the chip is open, **When** the option list is scrolled to its end, **Then** the next
   page of users is appended and the already-ticked users stay ticked.
3. **Given** the chip is open, **When** a search term is typed, **Then** the option list is
   replaced by users matching that term, fetched from the server, after a short idle pause rather
   than per keystroke.
4. **Given** two users are ticked, **When** the table is read, **Then** it shows experiments
   created by either of them and no others.
5. **Given** two users are ticked and a status is also selected, **When** the table is read,
   **Then** it shows only experiments that satisfy both — creator in the ticked set, and that
   status.
6. **Given** users are ticked, **When** the chip's remove control is used, **Then** the whole
   creator selection is dropped and the table returns to what it showed before.
7. **Given** a user with no experiments is ticked, **Then** the table shows the "nothing matched"
   empty state with its clear-filters action, not an error and not a blank table.
8. **Given** the user directory request fails, **Then** the failure is reported the way every
   other failed request on this screen is reported, the option list says it failed rather than
   reading as an empty directory, and the table keeps the rows it already had.
9. **Given** the option list has reached the last page of users, **When** scrolling continues,
   **Then** no further requests are made.
10. **Given** an experiment whose creator id resolves to no user, **When** any creator is ticked,
    **Then** that experiment is simply not matched, and nothing on the screen breaks.

---

### User Story 2 - Find the experiments scheduled to start recently (Priority: P1)

An editor wants the experiments that have been put on the calendar lately — the last quarter, say —
without reading every row's schedule. They pick a window from a chip and the table narrows to
experiments whose start date falls at or after the start of that window, including ones scheduled
to start next week. Experiments with no schedule at all drop out, because "scheduled in the last 3
months" is not a claim an unscheduled draft can satisfy. Picking a different window replaces the
first; returning to "Any schedule" restores everything.

**Why this priority**: Also unblocked, also demo feedback, and it is what makes a long list
navigable in time. Independent of US1.

**Independent Test**: Ship the time chip alone. Fully testable by choosing windows against
experiments with start dates on either side of each boundary.

**Acceptance Scenarios**:

1. **Given** the listing, **When** the time chip is opened, **Then** exactly five options are
   offered: Any schedule, and the last 1, 3, 6 and 12 months.
2. **Given** no choice has been made, **Then** the chip applies no date constraint and reads as
   unfiltered.
3. **Given** "last 3 months" is chosen, **When** "last 12 months" is then chosen, **Then** only
   the 12-month window applies — the two never both apply.
4. **Given** "last 3 months" is chosen, **Then** an experiment whose start is one day inside the
   window is shown and one whose start is one day outside it is not.
5. **Given** "last 3 months" is chosen, **Then** an experiment scheduled to start next month is
   shown, because the window has no upper bound.
6. **Given** "last 3 months" is chosen, **Then** an experiment with no schedule is not shown.
7. **Given** "Any schedule" is chosen, **Then** experiments with no schedule are shown again.
8. **Given** a window is chosen together with a search term, a status, a goal and a creator,
   **Then** the table shows only experiments satisfying all of them.

---

### User Story 3 - See at a glance who created each experiment (Priority: P2)

Reading the listing, an editor can tell what each experiment tests, where it runs and how it is
doing, but not whose it is. A Created By column puts the creator's name in the row, so the question
"who set this up" is answered without opening anything.

**Why this priority**: Valuable but not urgent, and **blocked by #37304** — the name is not in the
payload yet. Sequenced after the two filters, which is also why the creator *filter* was specified
to match on the user id rather than on a name.

**Independent Test**: Once #37304 has landed, ship the column alone and read a row.

**Acceptance Scenarios**:

1. **Given** the payload carries the creator's name, **Then** the table has a Created By column
   showing it.
2. **Given** a creator name longer than its column, **Then** it is truncated and the full value is
   available on hover, exactly as the Name and Page columns already do.
3. **Given** a payload with no creator name — an older backend, before #37304 — **Then** the cell
   shows the listing's existing placeholder rather than an empty or literal-undefined cell.
4. **Given** the column is added, **Then** the table's skeleton row still draws one placeholder
   per column and the Name column still holds its documented minimum width.

---

### User Story 4 - A paginator that behaves like the rest of the admin (Priority: P2)

An editor who has just used Content Drive comes to Experiments. The rows-per-page control should
open fully rather than clipped, and a result set of four rows should not carry pagination furniture
it has no use for. Everything else about the paginator should already feel the same, because it
already is.

**Why this priority**: Polish on a working control. Lowest risk, and independent of the other three.

**Independent Test**: Ship alone; verify by opening the page-size control and by filtering down to
fewer rows than one page.

**Acceptance Scenarios**:

1. **Given** the listing shows enough rows to page, **When** the rows-per-page control is opened,
   **Then** its full option list is visible and not cut off by the table's scroll area.
2. **Given** a filter has narrowed the result set to fewer rows than one page holds, **Then** no
   paginator is drawn.
3. **Given** the same result set grows back past one page, **Then** the paginator returns.
4. **Given** a page size and a page are in force, **When** any filter changes, **Then** the listing
   returns to the first page and the total reflects the newly filtered set.
5. **Given** a sort is in force, **When** a later page is opened, **Then** the rows are that page of
   the whole sorted, filtered set — not the visible page re-sorted — and every active filter still
   applies.
6. **Given** a page or page size changes, **Then** no new request is made to the server.

---

### Edge Cases

- **A creator is selected, then the page is reloaded.** The selection lives in the address as a
  user id, and nothing has fetched the directory yet, so the chip holds an id and no name. The
  selected ids are resolved to names on hydration — see FR-009a.
- **A creator is selected and the site is switched.** A site switch already restarts paging and
  drops the page narrowing. Whether it should also drop a creator selection: it should not — the
  same person can have experiments on both sites, and status, goal and search all survive a switch
  today, so the creator selection behaves like them.
- **An experiment has a schedule object but no start date within it.** The schedule can be absent
  *or* present with an empty start. Both are "unscheduled" for the purposes of the time filter.
- **A window is chosen and left in force across midnight, or across a month boundary.** The window
  is anchored to "now", so its lower bound moves as the clock moves; a row can therefore leave the
  filtered set without the user touching anything.
- **"Last month" against months of different lengths.** Subtracting one month from the 31st lands
  on a shorter month; the anchor has to be defined rather than left to chance. See FR-021.
- **The user directory search matches nothing.** The option list says so, distinctly from a
  directory that failed to load.
- **The address names an unknown user id, or a window value that is not one of the five.** Both are
  hand-editable. Unknown values are dropped rather than trusted, which is the rule the existing
  status and goal parameters already follow.
- **Every filter is cleared from the empty state.** The clear-filters action has to clear the two
  new filters as well, or it leaves the list still narrowed while claiming to have widened it.
- **A creator selection is in force and every experiment of that creator is deleted from the list
  by a row action.** The list reloads; the selection stays; the empty state appears with its way out.
- **The Created By column is added while #37304 has not shipped to the running backend.** Version
  skew — the column renders placeholders rather than breaking.

---

## Requirements *(mandatory)*

### Created By (user) filter

- **FR-001**: The listing MUST offer a Created By filter as a chip in its toolbar, alongside the
  existing Status and Goal chips, using the same chip presentation as those two.
- **FR-002**: The filter's options MUST be people from the dotCMS user directory, including people
  who have created no experiments.
- **FR-003**: Options MUST be fetched a page at a time and appended as the option list is scrolled.
  The full directory MUST NOT be fetched up front.
- **FR-004**: The option list MUST offer a text search that is applied by the server, after a short
  idle pause rather than per keystroke, and that restarts the option list from its first page.
- **FR-005**: An experiment MUST be matched by comparing the ids of the selected people against the
  id of the experiment's creator as carried in the experiment payload. The client's experiment type
  MUST be extended to declare that field, which it does not today (see "State of the code as
  found", row 5).
- **FR-006**: Several people MUST be selectable at once, and an experiment MUST match when its
  creator is any one of them.
- **FR-007**: The Created By filter MUST compose with the search box and with the Status, Goal and
  schedule filters as a conjunction: an experiment is shown only when it satisfies every active
  filter.
- **FR-008**: The chip's remove control MUST clear the entire creator selection in one action.
- **FR-009**: People already selected MUST stay selected, and MUST stay labelled, as further pages
  are loaded and as the search term changes — including when the selected person is not present in
  the page currently displayed.
- **FR-009a**: When a creator selection arrives from the address rather than from a click — a
  reload, or a shared link — the selected ids MUST be resolved to their display names so the chip
  labels people rather than opaque ids. The resolution MUST be a lookup of the selected ids, not a
  scan of the loaded experiments: a person can be selected while having no experiment in view,
  which is precisely the case that produces the empty state, and that is the case an
  experiment-derived label would fail to name. A failed resolution MUST leave the filter working —
  the filter matches on ids and does not depend on the labels — and MUST fall back to showing the
  id rather than blanking the chip.
- **FR-010**: The filter's options MUST NOT show result counts. This is a deliberate divergence
  from the Status and Goal chips: a count cannot be computed for a person whose page of the
  directory has not been fetched, and a count shown for some options and not others is worse than
  none.
- **FR-011**: Requesting further pages MUST stop once the directory has no more to give, so
  scrolling at the end of the list issues no requests.
- **FR-012**: A failed directory request MUST be reported through the screen's shared error
  reporting, MUST leave the option list showing that it failed rather than showing an empty
  directory, and MUST leave the table's current rows untouched.
- **FR-013**: The idle pause before a search is applied MUST be consistent with the rest of this
  screen. The shared option-list component's pause is fixed at a longer value than the listing's
  own search box uses; the implementation MUST either make that value configurable or adopt one
  value for both, and MUST NOT leave the two controls on the same screen debouncing differently.
- **FR-014**: The option list's own copy — its "nothing matched" and "failed to load" text — MUST
  read as being about people. The shared component's current copy is worded for content-type
  fields and is wrong here, so it MUST be parameterised or replaced rather than inherited.
- **FR-015**: The capability that searches the user directory MUST live in the shared data-access
  library, and the Experiments portlet MUST consume it from there. It MUST NOT be reached by
  importing another portlet's library: two portlets already hold an equivalent, and neither
  publishes it, which is a boundary rather than an oversight.
- **FR-015a**: Consolidating the two existing portlet-local user searches onto that shared
  capability is explicitly OUT OF SCOPE. Both keep working untouched. This knowingly leaves three
  implementations of the same directory call in the tree until a follow-up consolidates them; the
  follow-up MUST be filed rather than left implied.
- **FR-016**: The option list MUST be created only once its popover is actually visible, so that a
  virtualised list measures a real viewport rather than a collapsed one and renders its first page.
- **FR-016a**: The chip's collapsed label MUST follow the behaviour the shared chip already gives
  the Status and Goal chips — the unfiltered label while nothing is selected, the selected names
  while there are few, and an overflow summary beyond that. A count badge MUST NOT be introduced
  for this chip alone.

### Schedule time filter

- **FR-017**: The listing MUST offer a schedule time filter as a chip, offering exactly five
  options: no constraint, and the last 1, 3, 6 and 12 months.
- **FR-018**: The filter MUST be single-select: choosing an option replaces any previous choice,
  and two windows can never be in force at once. No existing chip in the portlet's chip family is
  single-select, so this is a new mode.
- **FR-019**: No constraint MUST be the default state, and in that state the filter MUST read as
  unfiltered and MUST apply no date constraint.
- **FR-020**: With a window in force, an experiment MUST match when its scheduled start is at or
  after the window's lower bound. There MUST be no upper bound, so an experiment scheduled to start
  in the future matches every window.
- **FR-021**: The lower bound MUST be computed by subtracting the option's month count from the
  current instant, and the rule for months of unequal length MUST be stated in the implementation
  and asserted by a test, so the boundary is a decision rather than an accident.
- **FR-022**: An experiment with no scheduled start MUST be excluded whenever a window is in force,
  and MUST reappear when the filter returns to no constraint. Both shapes count as having no
  scheduled start: no schedule at all, and a schedule carrying no start date.
- **FR-023**: The filter MUST compose with every other active filter as a conjunction (as FR-007).
- **FR-024**: Clearing the filter MUST return it to no constraint and restore the rows the window
  was hiding.
- **FR-025**: The default state MUST be represented as "nothing selected" rather than as a selected
  "Any schedule" option, so that the chip reads as neutral while unfiltered and cannot enter the
  self-contradicting state of "Any schedule" ticked alongside a specific window. The words "Any
  schedule" MUST still be shown as the chip's unfiltered label. (Rationale: the shared chip derives
  "active" from having selections, and its own documentation argues against an "All" row for
  exactly this reason.)
- **FR-026**: The five option labels MUST come from the message catalogue.
- **FR-027**: The filter MUST NOT change the Status or Goal chip counts (see FR-050).

### Created By column *(blocked by #37304)*

- **FR-028**: The table MUST have a Created By column showing the creator's name as delivered by
  the experiment payload.
- **FR-029**: The column MUST bind to the creator's display-name field and render whatever it
  carries — including the raw user id that #37304 deliberately falls back to for an unresolvable
  creator, which is how that field is guaranteed never to be null or empty. "Not the id" forbids
  binding the column to the creator-id field instead; it does not ask the screen to detect and
  suppress #37304's fallback. The client's experiment type MUST be extended to declare the field
  when #37304 lands.
- **FR-030**: A name too long for its column MUST be truncated with the full value available on
  hover, consistent with the Name and Page columns.
- **FR-031**: When the payload carries no creator name — a backend older than #37304 — the cell
  MUST show the listing's existing placeholder rather than an empty cell or a literal "undefined".
- **FR-032**: The column MUST sit between Variants and Schedule, per the design prototype, giving
  the header order: Name, Page, Goal, Variants, Created By, Schedule, Status, Modified, actions —
  nine columns. It is an **addition**: the Modified column stays, even though the prototype does
  not draw one.
- **FR-033**: The table's skeleton row MUST draw one placeholder cell per column after the column
  is added — nine rather than eight.
- **FR-034**: The table's documented minimum width MUST be recomputed to include the new column, so
  the elastic Name column keeps the floor its source comment specifies. The comment carries the
  arithmetic, not just the total, so the sum itself MUST be rewritten rather than the number
  silently bumped. The new column's own width is an implementation choice, bounded by the two
  columns it sits between.
- **FR-034a**: The Created By column MUST be display-only. It MUST NOT be sortable, MUST NOT add a
  sort field, a sort URL value or a comparator. Every column beside it is sortable, so this has to
  be said: the screen's sort fields are a closed set whose values double as the table's sort key,
  the address's sort parameter and the comparator key, so adding one would reach into the address
  contract this feature otherwise promises to leave alone. Sorting by creator is not asked for.

### Pagination alignment

- **FR-035**: The rows-per-page control's overlay MUST render outside the table's scroll area, so
  its option list is never clipped.
- **FR-036**: The default page size MUST be 20 and the offered sizes MUST be 20, 40 and 60. Both
  already hold; this requirement exists so a later change cannot silently undo them, and no work is
  expected.
- **FR-037**: The paginator MUST NOT be drawn when the filtered result set fits on a single page,
  and MUST be drawn when it does not. This is the intent of Content Drive's rule, not its literal
  expression: Content Drive shows the paginator unconditionally while its table is server-paged,
  and this table is flagged as paged even though it holds every row, so copying the expression
  verbatim would show the paginator always. Since this listing knows its own true total, the
  single-page test is available and is what MUST be used.
- **FR-038**: The first page MUST be loaded exactly once on entry. Note that the flag the issue
  asks to bind is already effectively set, so this requirement is expected to be satisfied by the
  code as it stands; the implementation MUST verify that rather than assume it, and MUST NOT
  introduce a second initial load while making the binding explicit.
- **FR-039**: The paginator MUST keep its existing appearance in every other respect — no
  first/last icons, no page links, and the existing "Page N" report.
- **FR-040**: Changing the page or the page size MUST re-slice rows already held and MUST NOT issue
  a request to the server.
- **FR-041**: Applying or clearing any filter — search, Status, Goal, Created By or schedule — MUST
  return the listing to its first page and MUST recompute the total from the newly filtered set.
- **FR-042**: Sorting MUST order the whole filtered set and then page it, never order only the
  visible page.
- **FR-043**: Moving between pages MUST preserve the active sort and every active filter.
- **FR-044**: A page size arriving in the address that is not one of the offered sizes MUST
  continue to be honoured, as it is today, rather than being snapped to an offered value.

### Cross-cutting: address, clearing, and the existing narrowing chain

- **FR-045**: **Every filter of this list is part of the address.** The listing's view state —
  each narrowing, the sort and the paging — lives in the route and nowhere else, so a filtered
  list can be linked, reloaded and stepped through with browser back and forward without losing
  what it was showing. This is a standing rule, stated here as a principle so that filters added
  after these two inherit it rather than re-deciding it. Applied to this work: both new filters
  MUST be represented in the address and MUST rehydrate from it on reload and on back and forward,
  exactly as the search term, Status and Goal selections already do.
- **FR-045a**: The rule covers filters **of the list**. It MUST NOT be over-applied to transient
  interface state — in particular, the search box inside the Created By popover narrows the option
  list rather than the data, and MUST NOT be written to the address. Routing it would also rewrite
  the address on every settled keystroke for as long as the popover is open.
- **FR-046**: Both MUST join the existing single-writer/single-reader address contract rather than
  writing to the address independently.
- **FR-047**: A filter in its default state MUST be absent from the address, so a pristine listing
  still carries no query string. This satisfies FR-045 rather than bending it: an absent parameter
  means its default, so the view state stays fully derivable from the route. The stricter reading
  — always write every parameter — is deliberately not adopted, because it would change all ten
  existing parameters and the deep links #37005 built on them.
- **FR-048**: Values in the address that are not recognised MUST be dropped rather than applied —
  an unknown window value, and a malformed creator entry — matching the rule the Status and Goal
  parameters already follow. An unrecognised creator id is not necessarily invalid, so it MUST be
  retained and simply match no experiment.
- **FR-049**: The two new parameter names MUST be chosen so they can survive the move to
  server-side filtering (#37007) without a rename, and MUST be recorded in the plan alongside the
  existing ten.
- **FR-050**: Neither new filter may change the Status or Goal option counts. Those counts are
  computed from the set narrowed by site, search and page-scope only — deliberately before the
  status and goal selections — so both new filters MUST narrow on the same side of that computation
  as status and goal do.
- **FR-051**: The screen's "are any filters active" condition and its clear-filters action MUST both
  account for the two new filters, so the "nothing matched" empty state appears when one of them is
  the reason nothing matched, and its action actually widens the list.
- **FR-052**: Filtering MUST remain client-side over the already-loaded set. When #37007 moves the
  listing to server-side paging and filtering, it MUST preserve the observable behaviour specified
  here — the option sets, the conjunction, the lower-bound-only windows, the exclusion of
  unscheduled experiments, and the address contract.
- **FR-053**: Every new user-facing string — chip titles, the five window labels, the column header,
  the option list's empty and failure copy — MUST come from the message catalogue. No literal
  English in templates or components.

### Key Entities

- **Experiment (client view)**: gains two declared fields it does not have today — the creator's
  **id**, which the API already sends and which the Created By filter matches on, and the creator's
  **display name**, which #37304 will add and which the Created By column renders. Its **schedule**
  already carries an optional start instant, which the time filter compares against.
- **Directory person (option)**: an id and a display label, fetched a page at a time and searched
  by the server. Carries no count. Selected people are held as ids, which is what the address
  stores.
- **Schedule window**: one of five values — none, and 1, 3, 6 or 12 months — resolving to a single
  lower bound relative to now. At most one is in force.
- **Listing view state**: the existing bag of search term, status selection, goal selection, page,
  page size, sort field, sort direction and page scope, extended by the creator selection and the
  chosen window. It is what the address serialises and what every narrowing reads.

---

## Success Criteria *(mandatory)*

- **SC-001**: An editor can restrict the listing to a named colleague's experiments in under 15
  seconds from the listing, without knowing any experiment's name.
- **SC-002**: Opening the Created By chip presents its first page of people without a perceptible
  wait, and does so on an installation with thousands of users — the wait does not grow with
  directory size.
- **SC-003**: Typing in the Created By chip issues one directory request per settled term, not one
  per keystroke.
- **SC-004**: Scrolling to the end of the directory issues no further requests: the request count
  stops increasing once the last page has been received.
- **SC-005**: For each of the four windows, an experiment one day inside the boundary is listed and
  one day outside it is not; an experiment scheduled to start in the future is listed under all
  four; an unscheduled experiment is listed under none of them and under no constraint is listed.
- **SC-006**: Any combination of the five narrowings — search, Status, Goal, Created By, schedule —
  yields exactly the experiments satisfying all of them, verified for at least one combination of
  all five at once.
- **SC-007**: Selecting a filter that matches nothing always ends on the "nothing matched" state
  with a working way out, never on a blank table and never on an error.
- **SC-008**: A listing narrowed by both new filters can be copied from the address bar, opened in a
  new tab, and shows the same rows; browser back returns to the previous narrowing.
- **SC-009**: The Status and Goal option counts are byte-identical before and after applying either
  new filter.
- **SC-010**: The rows-per-page control's full option list is reachable at every viewport height the
  listing supports.
- **SC-011**: A filtered result set of fewer rows than one page shows no paginator, and the same set
  widened past one page shows one.
- **SC-012**: Sorting a column while filtered and then paging shows, on page 2, the rows ranked
  21st onward across the whole filtered set.
- **SC-013**: Paging and resizing pages issue zero network requests.
- **SC-014**: Every string added by this work is translatable: no literal English survives in a
  template or component.
- **SC-015**: Once #37304 has shipped, every row shows a creator name; against a backend without
  #37304, every row shows the placeholder and nothing renders as "undefined".

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The new Experiments portlet's listing screen only — the
  site-wide list introduced by this epic. It is new product surface, not legacy: the older
  per-page Experiments screens under the Universal Visual Editor are untouched, and the legacy
  listing code that still ships beside the new portlet is untouched. The portlet remains opt-in;
  nothing here changes how it is reached.
- **Backward-compatibility expectations**:
  - The address contract is public in practice — the Universal Visual Editor links into this
    listing with page-scope parameters, and users share filtered links. Existing parameters keep
    their names, meanings and defaults; the two new ones are additive and absent when unused, so
    every URL that works today still works and still produces the same rows.
  - No API is changed by this work. The creator name arrives from #37304, which is itself specified
    as additive.
  - The Created By column must tolerate a backend that predates #37304, because the portlet and the
    backend are not released as one unit.
  - The page-size parameter continues to honour values outside the offered set, so an address
    bookmarked under the old 10/25/50 options is not broken.
- **Known related decisions**:
  - **#37304** — adds the creator's display name to every experiment response. Hard dependency for
    User Story 3 and for FR-028 through FR-034a. Currently open.
  - **#37007** — swaps the listing to server-side paging and filtering; parked on **#36823**.
    FR-052 states what it must preserve. Its existence is the reason this work does not attempt
    server-side filtering.
  - **#37005** — introduced the page-scope narrowing and the back-link to the editor that share
    this listing's address contract. The two new parameters must not disturb them.
  - The **client-side narrowing chain**, the **counts-before-selection** rule, and the
    **defaults-absent-from-the-address** rule are all existing decisions carried in the source's
    own comments. This work extends them rather than revisiting them.
  - The plan phase will consult `dotCMS/platform-adrs` formally. This work is frontend-only and
    changes no persisted data, no index mapping and no API contract, so it is expected to be
    rollback-safe.

---

## Assumptions

1. **The pagination part is smaller than the issue implies.** Two of its five listed gaps are
   already closed and a third describes a defect that does not exist. Recorded in "State of the
   code as found"; the issue would benefit from a correction, but the requirements above are
   already written to the code.
2. **Reuse beats a new component for the Created By popover.** The issue expected a new sibling
   component; a shared one already does the job and is publicly exported. Reuse is assumed, with
   the two divergences it brings raised explicitly as FR-013 (debounce) and FR-014 (copy). If
   parameterising the shared component turns out to disturb its existing consumers, a portlet-local
   variant is the fallback — that is a plan-phase call, not a change of requirement.
3. **The creator filter matches ids, not names.** Taken from the issue, and the reason the filters
   are not blocked by #37304. It also means a creator selection is meaningful before any name is
   available.
4. **Windows are anchored to "now" at evaluation time**, not to when the option was chosen, so a
   long-open listing's boundary drifts with the clock. This is the simplest reading of "the last 3
   months" and matches how such filters read elsewhere; it is called out because it makes the
   filtered set time-dependent.
5. **"Any schedule" is modelled as no selection**, per FR-025, rather than as a selected option.
   This is a deliberate choice made against the shared chip's own documented reasoning; it is
   visible to the user only as a neutral chip while unfiltered, which is how Status and Goal read.
6. **A creator selection survives a site switch**, like the search term, status and goal selections
   do, and unlike the page scope. Assumed rather than specified in the issue.
7. **The user directory endpoint is used as-is.** No backend work is in scope: the endpoint already
   supports search, paging, ordering and a total count, and the total is what makes FR-011's stop
   condition possible.
8. **The column's placement follows the design prototype**, which could not be read while writing
   this spec, so the order in FR-032 was read from the design project directly and confirmed
   against it.
9. **No new tests are needed for the two already-correct pagination constants** beyond an assertion
   that pins them, so a later edit cannot silently reintroduce the divergence.
10. **This is a frontend-only change.** No Java, no SQL, no endpoint, no `openapi.yaml`. The one
    API-shaped dependency, the creator name, belongs to #37304.
11. **Selected creators are resolved to names by looking them up, not by reading the loaded
    experiments** (FR-009a). Chosen over the free alternative because the experiment-derived label
    only works for creators who have an experiment in view and only after #37304 — and the case it
    fails is exactly the one that produces the empty state, where the chip is the only thing on
    screen still naming what was filtered.
12. **The shared user search is created in the shared data-access library and the two existing
    portlet-local copies are left alone** (FR-015, FR-015a). Chosen over migrating them, to keep a
    frontend Experiments change from touching two unrelated portlets and their test suites. The
    cost is recorded openly: three implementations of the same call coexist until the follow-up
    lands. Note that nothing in the tooling enforces the boundary being respected here — the lint's
    dependency constraints are unrestricted, and what actually stops a portlet-to-portlet import is
    that neither portlet publishes its service. The cheap shortcut is therefore available, and is
    rejected on architecture rather than because it would fail a check.

### Where the design prototype and the issue disagree

The issue's refinement decisions win in every case below. They are recorded so a reviewer reading
the prototype alongside this spec is not surprised by the differences.

13. **The prototype shows a per-user experiment count on each option; the issue decided no counts.**
    FR-010 stands — counts cannot be computed for people whose page of the directory has not been
    fetched, so some options would carry a number and others would not.
14. **The prototype's chip reads "Created By" with a count badge once several people are selected.**
    Rejected in FR-016a in favour of the behaviour the Status and Goal chips already have, so the
    three chips in one toolbar read alike; a badge on one of them is a new affordance for no
    functional gain.
15. **The prototype's time filter offers a selectable "Any schedule" row.** FR-025 deliberately
    diverges, modelling the default as no selection, so the chip reads neutral while unfiltered and
    cannot reach the contradictory state of "Any schedule" ticked beside a specific window.
16. **The prototype's window labels read "Scheduled in last 3 months"** — no "the" — **and its
    active chip drops the "Scheduled in " prefix.** The issue writes "Scheduled in the last month".
    The exact catalogue strings are a plan-phase decision; the five options and their meanings are
    not.
17. **The prototype draws a round initials avatar beside the creator's name.** Treated as a design
    note rather than a requirement; the issue asks only for the name, truncated with the full value
    on hover.
18. **The prototype's first column is headed "Experiment" where the shipped table reads "Name",
    and the prototype has no Modified column.** Neither is changed here: renaming a column and
    dropping one are out of scope, and FR-032 keeps Modified.

---

## Dependencies

- **Blocking**: [#37304](https://github.com/dotCMS/core/issues/37304) — the creator's display name
  in the experiment payload. Blocks User Story 3 and FR-028 through FR-034a only. Open at the time
  of writing.
- **Non-blocking, must stay compatible**: [#37007](https://github.com/dotCMS/core/issues/37007)
  (server-side listing, parked on [#36823](https://github.com/dotCMS/core/issues/36823)) and
  [#37005](https://github.com/dotCMS/core/issues/37005) (page-scope narrowing and the editor
  back-link, which share this listing's address contract).
- **Spawned by this work**: a follow-up to consolidate the two existing portlet-local user searches
  onto the shared data-access capability introduced here (FR-015a), and to update the comment that
  currently documents the duplication as waiting for exactly that shared service.
- **Design**: the Claude Design prototype linked from issue #37307.
- **Product owners named in the issue**: filters and chips — Jalison. Users endpoint — Humberto.
