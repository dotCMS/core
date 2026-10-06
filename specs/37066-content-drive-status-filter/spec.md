# Feature Specification: Content Drive Status Filter

**Feature Branch**: `issue-37066-content-drive-status-filter`

**Created**: 2026-08-24

**Status**: Draft

**Type**: New Feature (Task)

**GitHub Issue**: [dotCMS/core#37066](https://github.com/dotCMS/core/issues/37066) (absorbed [#37067](https://github.com/dotCMS/core/issues/37067); parent epic [#33999](https://github.com/dotCMS/core/issues/33999))

**Input**: User description: "Content Drive needs a Status filter (Archived, Unpublished, Locked): the search clauses on the drive search endpoint and the multiselect in the toolbar. None of the three predicates is expressible on the endpoint today. Nothing selected must mean exactly today's behavior: archived hidden, everything else returned."

---

## Scope Note *(read this first)*

This is **one vertical slice**: the search capability and the control that drives it ship together.
The ticket originally split them across two issues; #37067 was merged into #37066 because each half
restated the same contract and duplicated contract text is where drift starts.

**Selected statuses combine additively (OR), exactly like the Content Type and Language filters
beside it.** Checking more boxes returns more content, never less. `Archived + Unpublished` means
"archived **or** unpublished" — everything with no live version — not the intersection of the two.

This is a deliberate reversal of the ticket's original wording, which specified AND. The argument
for AND was that these are independent flags one item can hold at once, so intersecting them is
meaningful. It is — but only for one of the four possible combinations. Under AND,
`Archived + Unpublished` is redundant (archiving removes the live version, so every archived item is
already unpublished), `Archived + Locked` is almost always empty, and all three together is empty in
practice. Only `Unpublished + Locked` says something useful. Under OR every combination is
meaningful, and the control stops behaving as the sole exception in a row of filters that all widen
when you check more boxes — a difference no UI affordance can convey and that a user would read as
a bug.

The cost is accepted and recorded in Assumptions: "unpublished **and** locked" is no longer
expressible in Content Drive.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - An editor finds content that was archived (Priority: P1)

An editor is looking for a page a colleague archived last week, to check what it said before
deciding whether to restore it. Content Drive hides archived content by default, so today the only
way to see it is to leave Content Drive for the legacy Content Search portlet. The editor selects
**Archived** and the drive lists archived items — and only archived items.

**Why this priority**: This is the capability people currently leave Content Drive to get. It is
also the only one of the three that is *partially* present today in a form that does the wrong
thing (a flag that returns archived content **plus** everything else), so shipping it correctly is
what makes the filter trustworthy.

**Independent Test**: Archive one item in a folder that also holds live and draft items. Select
Archived. The result set contains the archived item and nothing else.

**Acceptance Scenarios**:

1. **Given** a folder holding live, draft and archived items, **When** the editor selects Archived,
   **Then** only the archived items are listed.
2. **Given** Archived is selected, **When** the editor clears it, **Then** the drive returns to
   hiding archived content, exactly as before the filter existed.
3. **Given** Archived is selected, **When** the editor also types a keyword in the search box,
   **Then** the results are archived items matching that keyword — the two filters narrow together.

---

### User Story 2 - An editor reviews what is not live yet (Priority: P1)

Before a release an editor wants to see everything in a section that has never been published or
whose published version has been taken down. They select **Unpublished** and the drive lists content
with no live version. Archived items do not appear: archived content is a separate question, and an
editor auditing drafts is not asking about the recycle bin.

**Why this priority**: "What is not live?" is the most common pre-release question content teams
ask, and Content Drive cannot express it at all today. The existing live/working switch answers a
different question (show me the live version) and is not the inverse of this one.

**Independent Test**: Create a never-published item, publish a second, archive a third. Select
Unpublished. Only the never-published item is listed.

**Acceptance Scenarios**:

1. **Given** a folder with published, unpublished and archived items, **When** the editor selects
   Unpublished, **Then** only the unpublished, non-archived items are listed.
2. **Given** an item that was published and then unpublished, **When** the editor selects
   Unpublished, **Then** that item is listed.

---

### User Story 3 - A manager finds content someone has checked out (Priority: P2)

A content manager notices work is stalled and wants to see everything currently locked. They select
**Locked** and the drive lists items with a lock held, whoever holds it, so the manager can chase
the owner or unlock the item.

**Why this priority**: Real and frequently asked, but it is a supervisory question rather than part
of the daily editing loop, and there is a workaround today (open items one at a time and look at the
lock indicator). It also has no partial implementation to correct, so it is pure addition.

**Independent Test**: Lock one item in a folder of otherwise unlocked items. Select Locked. Only the
locked item is listed.

**Acceptance Scenarios**:

1. **Given** a folder with one locked item, **When** the manager selects Locked, **Then** only that
   item is listed.
2. **Given** the lock is released, **When** the manager reloads the filtered view, **Then** the item
   no longer appears.

---

### User Story 4 - Seeing everything that is not cleanly published (Priority: P2)

Before handing a section over, a lead wants one view of everything needing attention: drafts,
archived items and anything checked out. They select all three statuses and the drive lists content
in *any* of those states, rather than making them run three separate passes and reconcile the
results by hand.

**Why this priority**: This is the reason the control is a multiselect rather than a dropdown. Each
status alone is already useful (stories 1–3), so this builds on them rather than standing alone —
but the union is the question a lead actually asks at review time, and no single status answers it.

**Independent Test**: Seed one archived, one unpublished, one locked and one plain live item. Select
all three statuses. The first three are listed and the live one is not.

**Acceptance Scenarios**:

1. **Given** items in each of the three states plus a clean live item, **When** all three statuses
   are selected, **Then** every item except the clean live one is listed.
2. **Given** Unpublished is selected, **When** the editor also selects Locked, **Then** the results
   *widen* to include locked content that is not unpublished.
3. **Given** two statuses are selected, **When** one is cleared, **Then** the results narrow to the
   remaining status alone.

---

### User Story 5 - A filtered view survives navigation and can be shared (Priority: P3)

An editor sends a colleague a link to "everything unpublished in Marketing". The colleague opens the
link and sees the same filtered view, with the Status selection shown as an active chip. Browsing
into a subfolder keeps it. Browser Back returns to the previous view with the right filters. Opening
an item in the editor and coming back keeps it. Reloading keeps it. Clearing all filters removes it
along with everything else.

**Why this priority**: Every other Content Drive filter behaves this way, so a Status filter that
did not would read as broken. It is P3 only because the filter is useful before it is shareable.

**Independent Test**: Select two statuses, navigate into a subfolder, press Back, then reload. The
selection and the results are the same at every step.

**Acceptance Scenarios**:

1. **Given** a Status selection, **When** the page is reloaded, **Then** the selection and results
   are unchanged.
2. **Given** a Status selection, **When** the editor browses into another folder, **Then** the
   selection still applies in that folder.
3. **Given** a Status selection, **When** the editor uses browser Back or Forward, **Then** the
   restored view carries the selection that URL had.
4. **Given** a Status selection, **When** the editor opens an item in the editor and returns,
   **Then** the selection is still applied.
5. **Given** a Status selection, **When** "Clear all" is used, **Then** the Status selection is
   removed along with the other filters.
6. **Given** a Status selection, **When** the view is shared as a link, **Then** the recipient sees
   the same selection.

---

### Edge Cases

- **Folders have no status.** Whenever any status is selected the results are content only. This
  matches how the drive already behaves for the other narrowing filters.
- **No status selected** must produce exactly the behavior that exists today — archived content
  hidden, everything else returned — with no change to result counts, ordering or pagination.
- **Archived is the only status that reveals archived content.** Selecting Unpublished or Locked
  alone must not surface archived items, even though every archived item is technically also
  unpublished. Hiding archived content is the drive's standing default, and only Archived lifts it.
- **An unrecognized status value** submitted directly to the search endpoint is rejected with a
  clear client error naming the accepted values, rather than being silently ignored (which would
  return a wider result set than the caller asked for).
- **Status combined with a workflow filter.** The two combine with AND: *governed by that workflow*
  **and** *in any selected state*. Content Drive can already filter by workflow, including steps that
  archive content, so the pairing must return a coherent result rather than an empty one caused by
  two rules contradicting each other about archived content.
- **Text search plus status.** The drive uses different search strategies depending on whether a
  keyword is present and how the environment is configured. Every strategy must apply the status
  filter identically, so the same selection never returns different results because of a
  configuration the user cannot see.
- **An empty result is still possible** — a folder with no content in any selected state. That shows
  the standard empty state, never an error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The drive search capability MUST accept a set of content statuses drawn from
  Archived, Unpublished and Locked, defaulting to an empty set.
- **FR-002**: An empty set MUST preserve today's behavior exactly: archived content excluded, all
  other content returned. The status filter MUST be **skipped entirely** in that case, not
  translated into a vacuous "matches anything" condition — every search that exists today sends no
  status, so this is the default path, not an edge case.
- **FR-003**: Archived alone MUST return only archived content, never archived content in addition
  to everything else.
- **FR-004**: Unpublished alone MUST return only content with no live version, and MUST exclude
  archived content.
- **FR-005**: Locked alone MUST return only content on which a lock is held, regardless of who holds
  it, and MUST exclude archived content.
- **FR-006**: Multiple selected statuses MUST combine with **OR** — the result is content holding
  *any* of the selected states. Adding a status MUST never reduce the result set.
- **FR-006a**: OR applies **within** the status filter only. Status MUST still combine with every
  other filter by **AND**, as the existing filters already do with each other. Selecting a workflow
  and two statuses means *governed by that workflow* **and** *in either of those states* — adding a
  status MUST never loosen another filter.
- **FR-007**: Archived MUST be the only status that admits archived content into the results.
  Selecting it alongside others widens the results to include archived content as well as content in
  the other selected states.
- **FR-008**: The existing inclusive "show archived" behavior relied on by the legacy Site Browser
  MUST be left unchanged; the exclusive Archived behavior is added alongside it.
- **FR-009**: A status selection MUST produce identical results whether or not a keyword search is
  active, under the default search strategy.
- **FR-009a**: Under the index-only strategy (`PURE_ES`, opt-in and not the default), *Unpublished*
  MAY additionally return content that has a live version alongside newer unpublished edits. This is
  an accepted divergence, not a defect: *Unpublished* means "no live version exists", which is a
  question about the content as a whole, while the index records that flag per version — so a
  published item's draft version reads as not-live. The identifier-scoped meaning is the definition;
  the index cannot express it in a single-document query.
  - This follows [ADR-0018](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0018-database-first-content-drive-search-with-index-deferred-text-filtering.md),
    which routes structural predicates to the database precisely because the index cannot answer
    them reliably, and states that the index-only strategy forfeits that guarantee for *every*
    criterion and must not become the default. This is one instance of a limitation that decision
    already accepted, not a new one introduced here.
  - Every other status is unaffected, and the default strategy is unaffected.
- **FR-010**: An unrecognized status value MUST be rejected with a client error, consistent with how
  the drive already rejects unknown field-filter keys.
- **FR-011**: A status selection MUST NOT conflict with a workflow filter that also constrains
  archived content; the two MUST combine into one coherent result set.
- **FR-012**: Users MUST be able to select any combination of the three statuses from a single
  control in the Content Drive toolbar, positioned **after the workflow filter and before the locale
  filter**. Content type and workflow MUST stay adjacent: the workflow filter's options are derived
  from the content-type selection, and that is the only such dependency in the row. Status depends on
  nothing, so it sits beside workflow — the two ask the same kind of question, where content sits in
  its lifecycle and whether anyone is holding it — without coming between workflow and the selection
  it reads from.
- **FR-013**: The control's labels MUST be localizable, following the Content Drive naming
  convention already used by the other filters.
- **FR-014**: The active selection MUST be reflected as a chip, consistent with the other toolbar
  filters, and MUST be clearable from that chip.
- **FR-015**: Folders MUST be excluded from the Content Drive results whenever any status is
  selected. This is a **client-side** rule: folders carry no status, so the Content Drive UI stops
  requesting them. The search capability itself MUST NOT override an explicitly requested
  folder setting — a caller that asks for folders alongside a status MUST receive them. Silently
  overriding it would make the response stop matching the request, and would leave the folder
  pagination describing a query the caller never received.
- **FR-015a**: Because the rule is client-side, changing it MUST remain a client-side change. If
  folder visibility is later exposed as its own control, honouring it MUST NOT require altering the
  search capability or its contract.
- **FR-016**: A status selection MUST persist across navigation exactly as every other Content Drive
  filter does: deep link, page reload, browsing between folders, browser Back/Forward, and opening
  an item in the editor and returning.
- **FR-017**: The existing "Clear all" action MUST clear the status selection.
- **FR-018**: An empty result set MUST show the standard empty state, never an error.
- **FR-019**: The Content Drive request MUST stop pinning archived content off unconditionally; that
  decision MUST come from the status selection instead.
- **FR-020**: The control and each of its options MUST carry stable test identifiers, and the
  control MUST carry an accessible label.

### Key Entities

- **Content Status**: An independent state an item can hold, from a closed set of three — *Archived*
  (removed from circulation but recoverable), *Unpublished* (no live version), and *Locked* (checked
  out by a user). An item may hold several at once, but the filter asks whether an item is in *any*
  selected state, not all of them.
- **Status Selection**: The set of statuses the user has chosen. Empty by default; every member
  widens the result set.

## Success Criteria *(mandatory)*

> **How these are verified.** This specification stays technology-agnostic by convention, so the
> concrete test types live in the plan: **unit** for status parsing and the 400, **integration** for
> the semantics matrix (each status, each pair, all three, the empty default, the never-shrinks
> property, `PURE_ES` parity, and the archive-step regression), and **Jest/Spectator** for the chip,
> the request payload and the URL round-trip. Postman is deliberately not used here — the behavior
> needs seeded archived/locked fixtures that a collection cannot construct against a shared
> environment, and the integration tests cover the same endpoint more precisely.
>
> Per Constitution Principle V these are non-negotiable and land **before** implementation:
> `/speckit-tasks` orders every user story as tests → developer-approval gate → confirmed-failing
> (Red) gate → implementation.

### Measurable Outcomes

- **SC-001**: An editor can locate archived content from within Content Drive in a single action,
  without leaving for another part of the product.
- **SC-002**: Each single status returns exactly the items in that state and no others, verified
  against a known fixture covering all three states plus unaffected content.
- **SC-003**: Every pair of statuses, and all three together, return exactly the union of the items
  in the selected states — never fewer items than any one of them alone returns.
- **SC-004**: With no status selected, result counts and ordering are identical to those produced
  before the filter existed, for the same folder and filters.
- **SC-005**: The same status selection returns the same result set with and without a keyword
  search under the default strategy. Under the opt-in index-only strategy, parity holds for every
  status except the *Unpublished* case described in FR-009a.
- **SC-006**: A status-filtered view reproduces the same selection and results after a reload, a
  folder change, a Back/Forward, an editor round-trip, and when opened by a second user from a
  shared link.
- **SC-007**: Selecting an additional status never returns fewer results than the selection did
  before it was added.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The shared content-browsing capability behind both Content Drive
  (modern) and the Site Browser (legacy). The legacy Site Browser exposes a "Show Archived" checkbox
  whose meaning is *inclusive* — archived content **in addition to** everything else. The new
  Archived status is *exclusive* — archived content **only**, when selected alone. These are
  different questions and both must remain expressible; the new behavior is added alongside the old
  one rather than replacing it.
  - The legacy Content Search portlet offers the same three predicates but combines them
    **additively in the AND sense** on its backend, while exposing them as a mutually exclusive
    dropdown in its UI. This spec deliberately follows neither: it keeps the multiselect but makes
    it a union, because that is what matches the rest of the Content Drive toolbar.
- **Backward-compatibility expectations**: The legacy Site Browser's "Show Archived" checkbox must
  behave exactly as before. Existing callers of the drive search capability that send no status must
  see byte-identical results. No content, stored configuration, or admin workflow changes.
- **Known related decisions**: The workflow filter delivered earlier in this epic established how a
  new drive-search filter is plumbed end to end, and the later archive-step work established the
  precedent for a filter that manipulates the archived condition — including the care needed when two
  filters both have an opinion about it. Both are binding shape precedents. The plan phase will
  formally consult `dotCMS/platform-adrs`.

## Assumptions

- The three statuses are the complete set for this feature. Other states an item can be in (for
  example "has a scheduled publish date") are out of scope.
- "Locked" means a lock is held by anyone, not "locked by me". A per-user variant is not requested
  and would be a separate filter.
- **Intersective queries are out of scope.** "Unpublished **and** locked" — drafts currently checked
  out — is not expressible through this control, and this is an accepted trade for a filter row with
  one consistent mental model. If it proves to be a real need, it belongs in a later refinement that
  makes the combining rule explicit in the UI rather than implicit and inverted.
- Users of this filter already have permission to see the content it surfaces; the filter narrows a
  result set that permission checks have already constrained, and grants no new visibility.
- The Shared Assets / System Host toggle is explicitly out of scope, tracked separately in
  [#34760](https://github.com/dotCMS/core/issues/34760).
