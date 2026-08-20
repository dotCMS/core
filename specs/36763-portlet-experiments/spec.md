# Feature Specification: Experiments Portlet

**Feature Branch**: `issue-36987-experiments-portlet-spec`

**Created**: 2026-08-20

**Status**: Descriptive baseline (Screens 1–3 built; Screens 4–6 pending)

**Type**: New Feature

**Epic**: [#36763](https://github.com/dotCMS/core/issues/36763) — Experiments: A/B Testing v2

**Work item**: [#36987](https://github.com/dotCMS/core/issues/36987) — Experiments v2, Part 2: Build the new Experiments UI (the epic's child that owns this portlet)

**Input**: User description: "Experiments Portlet — the standalone portlet that replaces the per-page UVE experiments screens. Write the spec for the FEATURE AS A WHOLE, across all its screens, not for one screen."

---

## Read This First: What Kind Of Spec This Is

**This is a descriptive spec of software that already exists.** Most of this feature is built and
sits in this worktree's history. It is written to do two things:

1. **Capture the decisions and invariants that are currently only visible in code and commit
   messages** — the reasoning lives in docblocks and commit bodies today, where it is invisible to
   anyone who has not read the diff.
2. **Serve as the baseline for the portlet's remaining screens** (UVE integration, the server-side
   list swap, and the legacy deletion), which must not re-litigate the decisions recorded here.

**On the TDD gate (Constitution Principle V).** The constitution's Test-First rule is
non-negotiable *for work this spec drives forward*. It is **not** applied retroactively to the
code already committed. Screens 1–3 were built and tested in the same change, not test-first, and
this document says so plainly rather than reconstructing a Red phase that never happened. The
shipped screens carry unit coverage (914 tests across 52 suites in the portlet at the time of
writing) and that coverage is the evidence for the requirements marked **[BUILT]**. Every
requirement marked **[PENDING]** is future work and **is** subject to the full three-gate TDD rule:
tests written → developer approval → confirmed failing → implementation.

**Status legend used throughout:**

| Marker | Meaning |
|---|---|
| **[BUILT]** | Shipped and covered by unit tests. Described here so the behavior is documented, not to request it. |
| **[PENDING]** | Not yet built. Subject to the TDD gate. |

---

## Overview

Today an experiment exists only inside the page editor. The entry point is a UVE nav item scoped
to one page, the list shows only that page's experiments, and creation is a drawer that asks for a
name and a description because the page is implied by the route. There has never been a screen
that lists every experiment on a site, and there has never been a way to create an experiment by
choosing a page.

The Experiments Portlet is a standalone, site-wide portlet mounted at `/experiments` that replaces
those per-page screens. A user can find, create, configure, start, monitor and conclude an
experiment end-to-end without opening the page editor.

The legacy per-page screens remain in the tree, frozen, and keep serving until a dedicated
migration retires them. Nothing in this feature breaks them.

### Screens

| # | Screen | Route(s) | Issue | State |
|---|---|---|---|---|
| 1 | **List** | `/experiments` | [#36989](https://github.com/dotCMS/core/issues/36989) | **[BUILT]** — PR [#37034](https://github.com/dotCMS/core/pull/37034) merged to `main` 2026-08-18 |
| 2 | **Create/Update** | `/experiments/new`, `/experiments/:id/configuration` | [#37003](https://github.com/dotCMS/core/issues/37003) | **[BUILT]** — PR [#37064](https://github.com/dotCMS/core/pull/37064) open |
| 3 | **View Results** | `/experiments/:id/results` | [#37004](https://github.com/dotCMS/core/issues/37004) | **[BUILT]** — PR [#37135](https://github.com/dotCMS/core/pull/37135) open, stacked on #37064 |
| 4 | UVE integration behind the experiments feature flag, plus the variant Edit Content round-trip | — | [#37005](https://github.com/dotCMS/core/issues/37005) | **[PENDING]** |
| 5 | Swap the list to the server-side contract | — | [#37007](https://github.com/dotCMS/core/issues/37007) | **[PENDING]**, blocked on #36823 |
| 6 | Migration: delete the legacy screens, remove the flag | — | [#37008](https://github.com/dotCMS/core/issues/37008) | **[PENDING]**, gated on #37006 |
| — | End-to-end suite (gates screen 6; see *Out of Scope*) | — | [#37006](https://github.com/dotCMS/core/issues/37006) | **[PENDING]** |

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find and triage every experiment on a site (Priority: P1)

**[BUILT]**

A marketer opens the Experiments portlet and sees every experiment running on the current site in
one table, not one page at a time. They narrow it by name, by status or by goal, sort it, page
through it, and act on individual rows — archive one, cancel a schedule, end a run — without
leaving the list. When they hand the URL to a colleague, the colleague sees the identical view.

**Why this priority**: This is the screen that makes the portlet exist. It is the entry point for
every other journey, and it delivers value on its own: a site-wide inventory of experiments has
never been available in any form.

**Independent Test**: Navigate to `/experiments` on a site with experiments spread across several
pages. The list shows all of them; filtering, sorting and paging change what is shown; the URL
changes with them; pasting the URL into a new tab reproduces the view exactly.

**Acceptance Scenarios**:

1. **Given** experiments exist on several pages of the current site, **When** the user opens
   `/experiments`, **Then** all of them are listed with name, page path, goal, variant count,
   schedule, status and last-modified date, sorted by last-modified descending.
2. **Given** a list of experiments, **When** the user types a term into the search box, **Then**
   after a short idle pause the list narrows to experiments whose name, description or page path
   contains that term, case-insensitively, and paging returns to the first page.
3. **Given** an unfiltered list, **When** the user has selected no status, **Then** archived
   experiments are hidden; **When** they explicitly select the archived status, **Then** archived
   experiments appear.
4. **Given** the status and goal filters, **When** the user selects a value in one, **Then** the
   counts shown beside the values they have *not* selected do not move.
5. **Given** a filtered, sorted, paged view, **When** the user copies the URL into a new tab,
   **Then** the identical view is restored; **When** they press the browser Back button, **Then**
   they walk back through the previous views.
6. **Given** the user is on `/experiments` with no filters applied, **Then** the URL carries no
   query parameters at all.
7. **Given** an experiment row, **When** the user opens its actions, **Then** only the actions the
   experiment's status permits are offered; **When** they choose one, **Then** they are asked to
   confirm, the action is applied, a confirmation message appears and the list reloads.
8. **Given** the user switches the current site in the site selector, **Then** the list reloads
   scoped to the new site and paging restarts at the first page, while search, sort and status
   selection are kept.
9. **Given** a row action fails, **Then** the error is reported once through the shared error
   handler and the list stays usable rather than being replaced by an error screen.

---

### User Story 2 - Create and configure an experiment without opening the page editor (Priority: P1)

**[BUILT]**

A marketer presses *New Experiment*, names it, picks the page it should run on, defines a goal,
adds variants and splits traffic between them, optionally schedules it, and starts it — all on one
screen, with no Save button anywhere. Their work is persisted as they go.

**Why this priority**: This is the capability the whole epic exists to add. Before it, an
experiment could only be created from inside the page editor, on the page you happened to be
editing.

**Independent Test**: Open `/experiments/new`, type a name, pick a page, and observe the URL become
`/experiments/:id/configuration` without a page reload. Reload the browser: everything entered is
still there.

**Acceptance Scenarios**:

1. **Given** the empty Configure screen at `/experiments/new`, **When** the user has supplied both
   a name and a page, **Then** the experiment is created and the URL is replaced with
   `/experiments/:id/configuration` — with no dialog, no navigation flicker, and no loss of edits
   made while creation was in flight.
2. **Given** an existing experiment, **When** the user changes any field, **Then** the change is
   persisted automatically after a short idle pause, and several changes made inside that pause —
   from different cards — are persisted together as one update.
3. **Given** a save is in flight, **When** the user keeps editing, **Then** the newer edit is never
   lost: it is either included in the in-flight save or re-sent afterwards.
4. **Given** a save fails, **Then** the user is told, the unsaved changes are retained, and the
   screen does not go on claiming it is saving for the rest of the session.
5. **Given** the URL carries a page pre-selection parameter, **When** the page exists, **Then** the
   Page card is prefilled and the picker is skipped; **When** it names a page that does not exist,
   **Then** an inline message says so; **When** the lookup itself fails, **Then** a different
   message says so and the failure is reported through the shared error handler.
6. **Given** any page-identifying value taken from the URL, **When** it is not a well-formed
   identifier, **Then** it is rejected before any lookup is attempted.
7. **Given** the user presses Start with an incomplete form, **Then** all unmet rules are revealed
   at once, the view scrolls to the first offending field, and the footer counts them. **When**
   they then fix a field, **Then** that rule clears immediately without pressing Start again.
8. **Given** a form where nothing has been filled in, **When** the user has typed a goal but no
   page yet, **Then** the goal counts as entered — a value the user supplied is never reported as
   missing merely because the draft does not exist yet.
9. **Given** variant weights, **When** the user commits one row's weight, **Then** the remainder is
   distributed over the rows they have *not* set, leaving their earlier explicit choices intact;
   **When** they press Split Evenly, **Then** every weight is overridden with an even share.
10. **Given** an experiment whose status is not draft, **Then** every field is read-only behind a
    banner, and the banner copy for a running experiment differs from the generic read-only copy.
11. **Given** the page an experiment runs on, **Then** it can be chosen once and never changed
    afterwards.
12. **Given** a schedule with only an end date, **Then** the control that clears the schedule is
    still offered, because an end date alone is a schedule the server keeps.
13. **Given** the user presses Start with a schedule, **Then** the experiment becomes scheduled;
    **without** one, **Then** it starts running. Each transition is confirmed by a message.

---

### User Story 3 - Read an experiment's outcome and act on it (Priority: P2)

**[BUILT]**

An analyst opens a running experiment's report, reads the daily conversion curve and the Bayesian
posterior, checks whether the backend has called a winner, and — if it has — promotes that variant,
knowing the promotion also ends the experiment.

**Why this priority**: Valuable independently of Story 2 (an experiment created in the legacy
screens can be read here), but it has nothing to report until experiments are running, so it
follows Story 1 rather than leading.

**Independent Test**: Open `/experiments/:id/results` for a running experiment with recorded
sessions. The header, stat strip, both chart tabs and the summary table render. Refresh re-fetches
without navigating.

**Acceptance Scenarios**:

1. **Given** an experiment of any status, **When** the user opens its results URL, **Then** the
   screen loads — including for drafts and scheduled experiments, which render a waiting state
   built from the experiment alone.
2. **Given** a draft or scheduled experiment, **Then** no report is ever requested for it.
3. **Given** a report has loaded, **When** the user presses Refresh, **Then** the report is
   re-fetched in place, the existing figures are never replaced by a skeleton, and a second press
   while the first is in flight replaces that request rather than queueing behind it.
4. **Given** a report, **Then** the leading variant named on screen is the one the backend
   suggests; **When** the backend suggests none, **Then** the screen says there is no winner yet
   rather than naming the variant with the highest conversion rate.
5. **Given** fewer than the minimum number of sessions across the whole experiment, **Then** the
   summary table is replaced by a single empty state rather than showing rates computed from too
   little data; **above** that threshold every row shows its full data.
6. **Given** a summary table, **Then** each variant's lift is shown as signed percentage points
   against the control, in a tone that reads as gain or loss, and as a dash on the control row
   itself and wherever there is nothing to compare against.
7. **Given** a running experiment, **When** the user promotes a variant from either the stat strip
   shortcut or a table row, **Then** they are asked to confirm and the confirmation states that
   the experiment will be ended; **When** they confirm, **Then** the header re-renders as ended in
   place, every Promote control disappears, and the promoted row is marked.
8. **Given** a running experiment, **When** the user stops it from the header, **Then** they are
   asked to confirm and the header re-renders as ended in place, without navigating away.
9. **Given** two charts, **Then** exactly one is mounted at any moment, so the interactive legend
   always belongs to the chart it is drawn under.

---

### User Story 4 - Keep working when analytics is broken or a report is missing (Priority: P2)

**[BUILT]**

Analytics goes down. The marketer can still find, create and configure experiments; only the
reports are unavailable, and they are told why rather than being bounced to another screen.

**Why this priority**: The failure mode is common (a misconfigured analytics app is the single most
frequent cause of a rejected report call), and the previous behavior — a guard that redirected —
made a broken report take out screens that had nothing to do with reporting.

**Independent Test**: Point the install at a misconfigured analytics app. `/experiments` still
renders, with an inline explanation in place of the table. `/experiments/:id/results` renders the
misconfiguration state. Configure is unaffected.

**Acceptance Scenarios**:

1. **Given** a misconfigured analytics app, **When** the user opens the results URL, **Then** that
   screen — and only that screen — is replaced by an explanation of the misconfiguration, and the
   URL does not change.
2. **Given** the same misconfiguration, **When** the user opens the list, **Then** the list URL is
   unchanged and an inline state explains the problem in place of the table; the user is not
   redirected anywhere.
3. **Given** an experiment that loads but whose report does not, **Then** the screen keeps its
   shape — header, goal and period still read off the experiment — and the missing report is
   reported inline. Only a missing *experiment* blanks the screen.
4. **Given** a report already on screen, **When** a manual refresh fails, **Then** the last good
   report stays exactly as it is and a message says the refresh failed.
5. **Given** any failed call anywhere in the portlet, **Then** the error is reported once through
   the shared error handler, with no bespoke error dialog.

---

### User Story 5 - Retire the legacy screens without breaking them first (Priority: P3)

**[BUILT]** for the coexistence guarantees; **[PENDING]** for the deletion itself.

A developer needs to delete the legacy per-page experiments tree in one commit, with confidence
that nothing in the new portlet falls over.

**Why this priority**: It is a developer-facing guarantee rather than a user journey, but it is the
constraint that makes the whole incremental migration safe, and it is the one thing that is
expensive to retrofit if it is allowed to lapse.

**Independent Test**: Delete the legacy subtree locally. Nothing in the new tree fails to compile.

**Acceptance Scenarios**:

1. **Given** the new portlet code, **Then** it imports nothing from the legacy subtree.
2. **Given** a piece of logic both trees need, **Then** it lives in the shared area and the legacy
   tree points at it there — never duplicated, and never the other way round.
3. **Given** the legacy screens, **Then** their routes, their nav entry and their tests are
   unchanged by any work in this feature, and their tests pass unmodified.

---

### Edge Cases

- **An experiment whose page cannot be resolved.** The site filter fails closed: the experiment is
  dropped rather than leaked into another site's list. This is why the page lookup over-asks — one
  document exists per identifier *and* language, so a multilingual site returns several documents
  per page, and a lookup limited to the page count silently truncates and takes those experiments
  with it.
- **An empty list.** The table settles into a loaded empty state rather than spinning on its
  skeleton, including the case where the lookup has nothing to resolve.
- **An unrecognised sort field in the URL.** The list keeps the order the API returned rather than
  failing.
- **A cleared weight input.** An empty weight is kept as empty rather than being forced to zero
  mid-edit; a total that no longer adds up is exactly what the card exists to report.
- **A page locked by another user.** Variant rows that cannot be edited are disabled with a tooltip
  explaining why.
- **A crafted page identifier in the URL.** Rejected before it reaches any query.
- **A very fast save.** The saving indicator is held long enough to be legible rather than
  flickering for a frame; back-to-back saves read as one continuous indicator.
- **A results payload with no control variant.** The daily chart has no axis to draw against, so
  the empty chart state covers it rather than rendering a partial chart.
- **A Bayesian dataset that came back empty.** Not drawn, because a flat line would read as a real
  posterior.
- **The user navigates from one experiment's results to another's while the screen is up.** The
  previous experiment's report is dropped immediately, so stale figures are never read as the new
  experiment's.

---

## Requirements *(mandatory)*

### Functional Requirements — Cross-cutting

- **FR-001** **[BUILT]** The portlet MUST be reachable at `/experiments`, with the list at its root
  and each screen on its own URL, so any screen can be linked to directly.
- **FR-002** **[BUILT]** Every user-facing string MUST come from the translation catalogue; no
  literal copy in templates.
- **FR-003** **[BUILT]** Every failed request MUST be reported exactly once through the shared
  error-handling service. No screen may present its own error dialog.
- **FR-004** **[BUILT]** A failed *load* MUST put its screen into an error state; a failed
  *action* MUST leave the screen usable so the action can be retried.
- **FR-005** **[BUILT]** Every destructive or irreversible action MUST be confirmed before it is
  sent, and MUST report its outcome.
- **FR-006** **[BUILT]** The set of actions offered for an experiment MUST be derived from its
  status by the single shared status→actions map, never hard-coded per screen.
- **FR-007** **[BUILT]** A URL that names the same destination from two places MUST be built by one
  shared helper, so the two cannot drift apart.
- **FR-008** **[BUILT]** The portlet MUST NOT be registered for customers while its screens are
  still landing. It is reachable only where a layout row has been inserted by hand on a
  development or QA instance.
- **FR-009** **[PENDING]** Registration for customers MUST NOT happen before the list endpoint
  applies permission filtering (#36823) — the endpoint in use today ignores the user it is given.

### Functional Requirements — Screen 1: List

- **FR-010** **[BUILT]** The list MUST show every experiment on the **current site**, across all
  its pages, with columns for experiment name and description, page path, goal, variant count,
  schedule, status, last-modified date and actions.
- **FR-011** **[BUILT]** Search MUST match, case-insensitively and as a substring, against exactly
  what the row renders as text: name, description and the page path — including the identifier the
  Page column falls back to when a page has no path.
- **FR-012** **[BUILT]** Search MUST be applied after a brief idle pause rather than on every
  keystroke, and MUST return paging to the first page.
- **FR-013** **[BUILT]** Status and goal filters MUST be multi-select, MUST show a count per value,
  and MUST narrow together — an experiment has to satisfy both.
- **FR-014** **[BUILT]** Filter counts MUST be computed over the site-and-search-filtered set and
  MUST be independent of what is currently selected, so selecting a value never moves the numbers
  next to the values not yet selected.
- **FR-015** **[BUILT]** An empty filter selection MUST mean "no filter", not "match nothing".
- **FR-016** **[BUILT]** Archived experiments MUST be excluded from the default, unfiltered view
  and MUST appear when the archived status is explicitly selected.
- **FR-017** **[BUILT]** The list MUST page and sort, defaulting to last-modified descending, with
  a selectable page size.
- **FR-018** **[BUILT]** Filter, status selection, goal selection, page, page size, sort field and
  sort direction MUST all round-trip through the URL. A view with only defaults MUST produce a bare
  URL with no query parameters, and history entries MUST NOT be created for a URL that has not
  changed.
- **FR-019** **[BUILT]** Switching the current site MUST reload the list and restart paging, while
  preserving search, sort and status selection.
- **FR-020** **[BUILT]** An experiment whose page cannot be attributed to a site MUST be excluded
  rather than shown, so no experiment can leak across sites.
- **FR-021** **[BUILT]** The list MUST offer, per row and gated by status: Configure, View Results,
  archive/restore, cancel schedule, end, abort, delete, push-publish and add-to-bundle.
- **FR-022** **[BUILT]** *View Results* MUST be offered on **every** status, unlike the
  status-gated actions, because the report renders its own waiting state for an experiment that has
  measured nothing.
- **FR-023** **[BUILT]** Placeholder rows MUST be drawn while the first page loads, and the loaded
  empty state MUST be reachable — a list that resolves to nothing must never leave the placeholder
  in place.
- **FR-024** **[PENDING]** Paging, sorting, filtering and counts MUST move server-side once #36823
  lands. The URL parameter contract is final today, so the swap changes where parameters are
  applied, not what they are called.

### Functional Requirements — Screen 2: Create/Update

- **FR-025** **[BUILT]** Creation MUST be a routed screen, not a dialog: `/experiments/new` is the
  Configure screen with an empty draft.
- **FR-026** **[BUILT]** There MUST be no Save button. Persistence is automatic.
- **FR-027** **[BUILT]** The experiment MUST be created as soon as a name and a page both exist,
  exactly once, and the URL MUST then be replaced with the created experiment's configuration URL
  — without tearing down the screen or losing edits made while creation was in flight.
- **FR-028** **[BUILT]** Field changes MUST accumulate into a single pending change set behind a
  single idle timer, and MUST be sent as one combined update regardless of which cards they came
  from.
- **FR-029** **[BUILT]** A change made while a save is in flight MUST NOT be lost. A pending change
  is only considered saved while the value that was sent is still the current one.
- **FR-030** **[BUILT]** Unsaved changes MUST survive a failed save and be re-sent with the next
  change. The screen MUST distinguish "changes pending" from "a save is on the wire" and MUST NOT
  remain stuck reporting that it is saving.
- **FR-031** **[BUILT]** The saving indicator MUST stay visible long enough to be read once it has
  appeared, and consecutive saves MUST read as one continuous indicator.
- **FR-032** **[BUILT]** The page MUST be chosen once and MUST NOT be changeable afterwards; it
  MUST never be part of an update payload.
- **FR-033** **[BUILT]** Targeting conditions MUST never be part of any payload this screen sends.
- **FR-034** **[BUILT]** The screen MUST accept a page pre-selection from the URL by identifier or
  by path, prefilling the Page card. A value naming a page that does not exist and a lookup that
  failed MUST produce **different** messages; only the latter is reported as an error.
- **FR-035** **[BUILT]** Any page-identifying value taken from the URL MUST be validated as a
  well-formed identifier before it reaches a query. A malformed value MUST be answered as a page
  that is not there, without spending a request.
- **FR-036** **[BUILT]** The screen MUST enforce eight rules before an experiment can start: a
  name, a page, a goal type, a goal name, a goal condition value, a goal parameter name, at least
  two variants, and weights totalling 100.
- **FR-037** **[BUILT]** No rule may show an error before Start is pressed. Pressing Start MUST
  reveal all unmet rules at once, scroll to the first, and count them in the footer. The *reveal*
  latches; the errors themselves MUST be derived live, so fixing a field clears its error
  immediately. Start MUST never be disabled.
- **FR-038** **[BUILT]** A value the user has entered MUST count as entered even before the draft
  exists — a rule may not report a filled field as missing merely because nothing is persisted yet.
- **FR-039** **[BUILT]** Variant weights MUST be edited as a set that is only ever valid summing to
  100. Committing one row's weight MUST distribute the remainder over rows the user has **not**
  set, preserving their explicit choices; only when every other row has been set may one move, and
  then the one set longest ago, only as far as the arithmetic demands. Split Evenly MUST override
  every weight.
- **FR-040** **[BUILT]** A weights total other than 100 MUST be reported live, and MUST also be one
  of the eight Start rules.
- **FR-041** **[BUILT]** A cleared weight input MUST be preserved as empty rather than coerced to a
  number.
- **FR-042** **[BUILT]** Variants MUST support inline rename, copy-variant-URL, and a used/maximum
  counter; the control variant MUST never be renamed or deleted; the maximum MUST come from the
  shared model, not be redeclared.
- **FR-043** **[BUILT]** When the experiment's page is locked by another user, the affected variant
  controls MUST be disabled with a tooltip explaining why.
- **FR-044** **[BUILT]** Scheduling MUST offer start and end with time-of-day and MUST respect the
  configured minimum and maximum experiment duration.
- **FR-045** **[BUILT]** The control that clears a schedule MUST be offered whenever **either**
  date is set — an end date on its own is a schedule the server keeps.
- **FR-046** **[BUILT]** Any status other than draft MUST make every field read-only behind a
  banner, with distinct copy for a running experiment. Status-specific actions remain available;
  it is the fields that are frozen.
- **FR-047** **[BUILT]** Goal types and their condition operators MUST come from the shared model,
  so the screen can never offer an operator the backend does not validate. Goal types without
  server-side conditions MUST render the same placeholder the legacy screen renders.
- **FR-048** **[BUILT]** Starting with a schedule MUST produce a scheduled experiment; starting
  without one MUST produce a running experiment; each transition MUST be confirmed by a message.
- **FR-049** **[BUILT]** The page picker MUST list pages only — no folders, files, assets or links
  — and MUST include unpublished pages, because an experiment can be configured against a draft.
- **FR-050** **[BUILT]** The picker MUST NOT restrict which page can be chosen. Any number of
  experiments may exist on one page; what cannot happen is two *running* on the same page at once,
  and that is enforced at Start, not at the pick.

### Functional Requirements — Screen 3: View Results

- **FR-051** **[BUILT]** The results screen MUST be reachable for an experiment of **any** status.
- **FR-052** **[BUILT]** An experiment that has measured nothing — draft or scheduled — MUST
  render a waiting state built from the experiment alone, and MUST NOT cause a report to be
  requested.
- **FR-053** **[BUILT]** The load MUST be sequential, not parallel: whether the report is worth
  requesting is decided by the experiment's **status**, read from the experiment once it has
  arrived — not by whether a report happens to be absent.
- **FR-054** **[BUILT]** Reports MUST be fetched lazily, MUST NOT be polled, and MUST be
  re-fetchable only by an explicit user action.
- **FR-055** **[BUILT]** A manual refresh MUST replace the results in place. Figures already on
  screen MUST NOT be swapped for a skeleton, and a second refresh while one is in flight MUST
  replace it rather than queue.
- **FR-056** **[BUILT]** A refresh MUST NOT be startable for a status that will never produce a
  request.
- **FR-057** **[BUILT]** A failed refresh MUST leave the last good report untouched and say only
  that the refresh failed.
- **FR-058** **[BUILT]** When the experiment loads but its report does not, the screen MUST keep
  its shape — header, goal and period read off the experiment — and report the missing report
  inline. Only a missing **experiment** may blank the screen.
- **FR-059** **[BUILT]** The screen's states MUST be mutually exclusive and resolved in this order:
  analytics misconfigured, first load failed, first load in progress, report.
- **FR-060** **[BUILT]** The leading variant MUST be whichever variant the backend suggests. A
  client-side comparison of conversion rates MUST NOT be used, because only the backend applies a
  significance threshold and a rate-based pick would always name someone.
- **FR-061** **[BUILT]** When the backend suggests no winner, the screen MUST render an explicit
  "no winner yet" state.
- **FR-062** **[BUILT]** Below a minimum of ten sessions **across the whole experiment**, the
  summary table MUST be replaced by a single empty state. Above it, every row shows its full data
  regardless of its own session count. No per-row filtering is applied above the threshold.
- **FR-063** **[BUILT]** Each row MUST show lift against the control as signed percentage points,
  toned as gain or loss, and as a dash on the control row and wherever there is nothing to compare.
  The baseline MUST be resolved by key and by name — never by row position — so the order rows
  arrive in cannot change the arithmetic.
- **FR-064** **[BUILT]** Promoting a variant MUST be confirmed from **every** entry point, and the
  confirmation MUST state that a running experiment will be ended, because the server ends it in
  the same call.
- **FR-065** **[BUILT]** After a promotion, every Promote control MUST disappear and the promoted
  row MUST be marked; a variant MUST NOT be promotable twice.
- **FR-066** **[BUILT]** Stopping from the header MUST be confirmed and MUST re-render the header
  as ended in place, without navigating.
- **FR-067** **[BUILT]** Exactly one chart MUST be mounted at any moment, so the interactive legend
  can only ever belong to the chart it is drawn under.
- **FR-068** **[BUILT]** The Bayesian posterior MUST come entirely from the backend. No client-side
  distribution maths.
- **FR-069** **[BUILT]** A posterior MUST NOT be drawn unless every variant has one, and a daily
  chart MUST NOT be drawn without a control to form its axis.
- **FR-070** **[BUILT]** A rejected report call MUST be reported with a meaningful heading even
  when the response carries none, since the most common cause is a misconfigured analytics app.

### Functional Requirements — Analytics health

- **FR-071** **[BUILT]** Analytics health MUST gate the **results route only**. A broken analytics
  app takes out reporting and nothing else.
- **FR-072** **[BUILT]** The gate MUST report rather than redirect: the URL stays where it is and
  the screen renders the misconfiguration state in place of the report. The edit-page-coupled guard
  used by the legacy screens is deliberately not reused.
- **FR-073** **[BUILT]** The list MUST NOT be route-gated by analytics health. It resolves health
  inline and renders an inline explanation in place of the table, keeping `/experiments` as the
  URL. Only an explicit not-OK answer blocks it — a pending answer never does.

### Functional Requirements — Coexistence with the legacy screens

- **FR-074** **[BUILT]** No code in the new tree may import from the legacy subtree. The dependency
  direction is legacy → shared, one way, so the legacy subtree stays deletable in one commit.
- **FR-075** **[BUILT]** Logic both trees need MUST be relocated to the shared area and the legacy
  tree repointed at it — never duplicated, never inverted. Only import lines may change under the
  legacy subtree, and its tests MUST pass unmodified.
- **FR-076** **[BUILT]** The legacy routes, the legacy nav entry and the legacy tests MUST be
  unchanged by this feature.
- **FR-077** **[PENDING]** The legacy subtree MUST NOT be deleted before every ported behavior is
  verified on a development or QA instance and the end-to-end suite is green.

---

## Architectural Invariants (as built)

> These are load-bearing and are currently documented only in code and commit messages. They are
> stated here in implementation terms **on purpose** — they constrain how the remaining screens are
> built, and a technology-neutral paraphrase would lose the constraint. They sit in their own
> section so the technology-agnostic requirements above stay technology-agnostic.

- **AI-1 — One state pattern, and only one.** Every screen's state lives in an NgRx Signal Store
  using the events plugin. Page events carry user intent and are named as commands; API events come
  in `Requested → Succeeded → Failed` triples. `withReducer` is the **only** place state changes;
  `withEventHandlers` is the **only** place async work runs. Components dispatch via
  `injectDispatch`; stores expose no mutating methods and never open UI — confirmations, dialogs
  and toasts belong to the components.
- **AI-2 — The async hook is `withEventHandlers`.** The installed `@ngrx/signals` is **21.1.1**.
  **`withEffects` does not exist** and will not compile, however many online examples name it.
- **AI-3 — Cancellation semantics are chosen per flow.** `switchMap` for loads and refreshes, so a
  re-trigger cancels the in-flight request; `mergeMap` for per-row and per-variant actions, so
  acting on one row cannot cancel a call already made for another.
- **AI-4 — Results are expensive and uncached.** A results call still goes through CubeJS: two
  analytics round-trips plus a Monte Carlo run, with no caching. That cost is the reason results
  are fetched lazily, never polled, and exposed behind a manual refresh, and the reason statuses
  that have measured nothing never reach the endpoint. **#36763 migrates this to CAEM +
  ClickHouse; the result model is unchanged by that migration**, so nothing in this spec depends on
  which backend answers.
- **AI-5 — Significance is the backend's call.** The leading variant is read from
  `bayesianResult.suggestedWinner`. A client-side comparison of conversion rates is forbidden: it
  would always name a leader, and the "no winner yet" state could not exist.
- **AI-6 — The results component follows the route.** The store subscribes to the route parameters
  for as long as the screen lives rather than reading them once, because the component is reused
  across experiments; an arriving identifier drops the previous experiment's state immediately.
- **AI-7 — One route config serves both Configure URLs.** `/experiments/new` and
  `/experiments/:id/configuration` are matched by a single route configuration. Two configurations
  would tear down the screen — and its store — during the post-creation URL swap, dropping the
  debounced saves and the just-created experiment. The absence of the identifier parameter is what
  tells the store it is on the creation screen, and the mount point must not force route
  re-creation on this subtree.
- **AI-8 — Resolvers run in the route injector.** Services a resolver injects must be provided on
  the route, not on the component, or activation fails before the component exists.
- **AI-9 — Two mounted dialogs must never share a key.** The Configure screen, the Results shell
  and the Results summary table each own their own confirmation dialog key; a shared key opens both
  at once.
- **AI-10 — The list's server-side operations are a documented interim.** Paging, sorting,
  filtering and counts are computed client-side against an endpoint that returns everything. This
  is acceptable only while the portlet is unregistered for customers, and is replaced by #36823's
  contract. The data-access layer is shaped so the swap changes the service and one load handler.

---

## Key Entities

- **Experiment** — The unit of work. Belongs to exactly one page, chosen once at creation and
  immutable thereafter. Carries a name, an optional description, a status, a goal, a set of
  variants with their traffic weights, a traffic allocation, and an optional schedule.
- **Status** — Draft, Scheduled, Running, Ended or Archived. Determines which actions are offered,
  whether the configuration is editable, and whether a report is worth requesting. Draft and
  Scheduled have measured nothing.
- **Variant** — One arm of an experiment, with a name, a share of the traffic, and a flag marking
  the control. The control is never renamed or deleted. Exactly one variant may be promoted, and
  only the experiment — never the report — knows which.
- **Traffic proportion** — The variants' weights as a single set, valid only when they total 100.
  Weights are only ever edited and persisted together.
- **Goal** — What the experiment measures: a type, a name, and at most one condition. Two of the
  four offered types carry conditions; their persisted shapes differ only in where the
  query-parameter name sits.
- **Schedule** — An optional start and an optional end, each with a time of day, bounded by the
  configured minimum and maximum durations. Either date alone constitutes a schedule.
- **Results** — The report for an experiment: total sessions, per-variant sessions, conversions and
  conversion rates, a daily series per variant, and a Bayesian result carrying the posterior and
  the suggested winner. Lift against the control is derived, not carried.
- **Page information** — A page's path and its site, resolved per page identifier. The experiment
  itself carries no site, so this is the only thing that can scope the list to a site.
- **Analytics health** — Whether the analytics app is configured and reachable. Anything other than
  OK is a misconfiguration.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001** A user can go from opening the portlet to a running experiment on a page of their
  choosing without ever opening the page editor.
- **SC-002** A user can see every experiment on a site in one view — a capability that did not
  previously exist in any form.
- **SC-003** No configuration work is ever lost to a missing Save button: after entering data and
  reloading the browser, everything entered is still there.
- **SC-004** A view of the list can be shared by copying its URL; the recipient sees an identical
  view, including filters, sort and page.
- **SC-005** A broken analytics installation costs the user reporting only. Finding, creating and
  configuring experiments continue to work.
- **SC-006** An experiment whose report fails to load still shows its identity, goal and period —
  the screen never goes blank when there is something to show.
- **SC-007** The portlet never claims a winner the backend has not called. Where there is no
  statistically suggested winner, the screen says so.
- **SC-008** No user can end a running experiment by promoting a variant without having been told,
  in the confirmation, that promoting will end it.
- **SC-009** Every one of the twelve behaviors identified as existing in the legacy screens is
  present in the portlet. None is dropped silently.
- **SC-010** The legacy per-page screens behave exactly as they did before this feature, at every
  point of its development.
- **SC-011** The legacy subtree can be deleted in a single commit without any change to the new
  tree.
- **SC-012** No experiment from another site ever appears in the current site's list.

---

## Out of Scope

Listed explicitly so they are not invited back in:

- **Any change to the results backend contract.** The result model is taken as given, including
  across the #36763 migration.
- **Charts or per-experiment metrics on the list screen.** Each report costs two analytics
  round-trips plus a Monte Carlo run and there is no batch form, so the design deliberately shows
  none.
- **The narrative Bayesian headline and note copy** present in the prototype's logic but never
  rendered. Confirmed dropped.
- **End-to-end tests**, which live in their own issue ([#37006](https://github.com/dotCMS/core/issues/37006))
  and gate the legacy deletion rather than this work.
- **The variant Edit Content / Preview round-trip into the page editor**, which belongs to the UVE
  integration issue.
- **The Targeting card**, which is commented out in the legacy screen and is therefore not a parity
  loss.
- **Autosave conflict resolution beyond last-write-wins.**
- **Server-side paging, sorting and filtering**, which belongs to the list-swap issue.
- **A registration upgrade task or starter bump.** The portlet stays unregistered while its screens
  land.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The per-page experiments experience inside the page editor —
  its nav entry, its per-page list, its creation drawer, its configuration screen and its reports
  screen. That surface is the older product surface and is **frozen**: it keeps serving unchanged
  until a dedicated migration retires it. The new portlet is additive; the one place the two
  overlap, the page-editor entry point, is switched by a feature flag that is off by default.

- **Backward-compatibility expectations**: The legacy screens must behave identically to today at
  every point of this feature's development — routes, nav entry and specs unchanged, specs passing
  unmodified. Existing experiments, whichever screen created them, must be readable and actionable
  from both. No deprecation is announced to customers during this work; the portlet is not
  registered for them. Two backend concerns are deliberately inherited rather than fixed here: the
  update endpoint's handling of targeting conditions (#36988), which the portlet works around by
  never sending them, and the list endpoint's missing permission filtering (#36823), which is a
  hard gate on registration rather than something the frontend can compensate for.

- **Known related decisions**: The reuse-over-duplication rule for anything both trees need
  (relocate to shared, repoint the legacy tree) is what keeps the legacy subtree deletable, and it
  is the constraint most easily lost. The events-store pattern recorded in the portlet architecture
  guide during Screen 1 is now the standard for portlet stores, not a local choice. The analytics
  gate deliberately departs from the legacy redirecting guard in favour of reporting in place. The
  plan phase will formally consult `dotCMS/platform-adrs`.

---

## Assumptions

- **This spec documents built software.** Screens 1–3 are implemented; their requirements are
  recorded, not requested. The TDD gate applies to the **[PENDING]** requirements and to any future
  change to the **[BUILT]** ones — not retroactively to code already committed. Where a future
  change touches built behavior, the three gates apply to that change.
- **The result model survives the analytics migration.** #36763 moves reporting from CubeJS to
  CAEM + ClickHouse without changing the result model, so nothing here is contingent on which
  backend answers. If that turns out to be wrong, the affected requirements are FR-052 to FR-070.
- **Volumes stay modest while the list operates client-side.** The interim is safe specifically
  because the portlet is not registered for customers; the assumption expires at registration.
- **The approved design prototype drives visual and behavioural detail only.** Existing code is
  reused wherever it exists. The prototype's own logic is a mock in two places and is **not** the
  spec: it picks the leading variant by highest conversion rate with no significance gate (see
  FR-060), and it has no negative, empty, loading, error or confirmation states at all — every such
  state in this spec comes from the legacy screens' behavior or from review, not from the
  prototype.
- **The status→actions map is authoritative.** Where a screen departs from it, that departure is
  itself a requirement (FR-022) rather than an oversight.
- **The minimum session threshold is ten**, applied experiment-wide, matching the threshold the
  daily chart has always used.

---

## Traceability

| Source | What it grounds |
|---|---|
| [#36987](https://github.com/dotCMS/core/issues/36987), under epic [#36763](https://github.com/dotCMS/core/issues/36763) | Strategy, coexistence, the two switches, functional-parity commitment |
| Issue [#36989](https://github.com/dotCMS/core/issues/36989) · PR [#37034](https://github.com/dotCMS/core/pull/37034) (merged) | Screen 1 — FR-010 to FR-024 |
| Issue [#37003](https://github.com/dotCMS/core/issues/37003) · PR [#37064](https://github.com/dotCMS/core/pull/37064) (open) | Screen 2 — FR-025 to FR-050 |
| Issue [#37004](https://github.com/dotCMS/core/issues/37004) · PR [#37135](https://github.com/dotCMS/core/pull/37135) (open, stacked) | Screen 3 — FR-051 to FR-070 |
| `core-web/libs/portlets/CLAUDE.md` | AI-1 to AI-3 — the events-plugin pattern and the version note |
| `.../dot-experiments/portlet/src/lib/shared/constants.ts`, `shared/models.ts` | Named thresholds, defaults, and the state shapes behind the entities |
| `.../portlet/src/lib/store/*.store.ts` | AI-1 to AI-6; the failure-handling requirements |
| `.../portlet/src/lib/lib.routes.ts` | AI-7, AI-8, FR-071 to FR-073 |
| Branch commits `d50b89324d`…`e2f3e2af6f` | The reuse-over-duplication rule (FR-074 to FR-076) and the Screen 3 decisions |
| Blocking backend issues #36823, #36988; migration #36763 | FR-009, FR-024, FR-033, AI-4 |
