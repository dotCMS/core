# Feature Specification: Content Drive Status Filter

**Feature Branch**: `issue-37066-content-drive-status-filter`

**Created**: 2026-08-24

**Status**: Draft

**Type**: New Feature (Task)

**GitHub Issue**: [dotCMS/core#37066](https://github.com/dotCMS/core/issues/37066) (absorbed [#37067](https://github.com/dotCMS/core/issues/37067); parent epic [#33999](https://github.com/dotCMS/core/issues/33999))

**Input**: User description: "Content Drive needs a Status filter (Archived, Unpublished, Locked): the search clauses on the drive search endpoint and the multiselect in the toolbar. None of the three predicates is expressible on the endpoint today. Selecting several narrows the results (AND), because they are independent states an item can hold at the same time. Nothing selected must mean exactly today's behavior: archived hidden, everything else returned."

---

## Scope Note *(read this first)*

This is **one vertical slice**: the search capability and the control that drives it ship together.
The ticket originally split them across two issues; #37067 was merged into #37066 because each half
restated the same contract and duplicated contract text is where drift starts.

The Status filter is deliberately **not** the same shape as the existing Content Drive filters.
Content type, base type and language are single-valued attributes, so selecting several is an OR
("either of these types"). Status is a set of independent flags a single item can hold at once, so
selecting several is an AND ("both unpublished *and* locked"). This difference is the reason the
control is a multiselect rather than a dropdown, and it is the single most important thing for a
reader to carry into planning.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - An editor finds content that was archived (Priority: P1)

An editor is looking for a page a colleague archived last week, to check what it said before
deciding whether to restore it. Content Drive hides archived content by default, so today the only
way to see it is to leave Content Drive for the legacy Content Search portlet. The editor selects
**Archived** in the Status filter and the drive lists archived items — and only archived items.

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
3. **Given** Unpublished is selected, **When** the editor also selects Archived, **Then** archived
   items are admitted to the results (see Edge Cases for why this pair reads as it does).

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

### User Story 4 - Combining statuses to ask a sharper question (Priority: P2)

A manager wants "drafts currently checked out by someone" — work in progress that is both blocked and
unpublished. They select **Unpublished** and **Locked** together and the drive lists only content
that is both.

**Why this priority**: This is the reason the control is a multiselect rather than a dropdown. A
single-select could not express it, and the combination is a question content teams actually ask.
It is P2 rather than P1 because it builds on stories 2 and 3 rather than standing alone.

**Independent Test**: Create four items covering every unpublished/locked combination. Select both
statuses. Only the item that is both unpublished and locked is listed.

**Acceptance Scenarios**:

1. **Given** items covering all four unpublished/locked combinations, **When** both statuses are
   selected, **Then** exactly the item holding both states is listed.
2. **Given** both statuses are selected, **When** one is cleared, **Then** the results widen to the
   remaining status alone.

---

### User Story 5 - A filtered view survives reload and can be shared (Priority: P3)

An editor sends a colleague a link to "everything unpublished in Marketing". The colleague opens the
link and sees the same filtered view, with the Status selection shown as an active chip. Reloading
the page keeps it. Clearing all filters removes it along with everything else.

**Why this priority**: Every other Content Drive filter behaves this way, so a Status filter that
did not would read as broken. It is P3 only because the filter is useful before it is shareable.

**Independent Test**: Select two statuses, copy the address, open it in a new session. The same two
statuses are selected and the same results are listed.

**Acceptance Scenarios**:

1. **Given** a Status selection, **When** the page is reloaded, **Then** the selection and results
   are unchanged.
2. **Given** a Status selection, **When** "Clear all" is used, **Then** the Status selection is
   removed along with the other filters.
3. **Given** a Status selection, **When** the view is shared as a link, **Then** the recipient sees
   the same selection.

---

### Edge Cases

- **Archived + Unpublished returns the same items as Archived alone.** Archiving an item removes its
  live version, so every archived item is already unpublished. The pair is redundant, not broken. It
  must not be presented as an error, an empty state, or a warning — it is simply a narrower question
  whose answer happens to coincide with a wider one.
- **Archived + Locked is reachable but uncommon**, since it needs an item that was locked by its own
  holder or archived by an administrator while a lock stood. An empty result there is legitimate and
  shows the ordinary empty state, never an error.
- **Folders have no status.** Whenever any status is selected the results are content only. This
  matches how the drive already behaves for the other narrowing filters.
- **No status selected** must produce exactly the behavior that exists today — archived content
  hidden, everything else returned — with no change to result counts, ordering or pagination.
- **An unrecognized status value** submitted directly to the search endpoint is rejected with a
  clear client error naming the accepted values, rather than being silently ignored (which would
  return a wider result set than the caller asked for).
- **Status combined with a workflow filter.** Content Drive can already filter by workflow, including
  steps that archive content. A status selection combined with such a workflow filter must return a
  coherent result, not an empty one caused by two rules contradicting each other about archived
  content.
- **Text search plus status.** The drive uses different search strategies depending on whether a
  keyword is present and how the environment is configured. Every strategy must apply the status
  filter identically, so the same selection never returns different results because of a
  configuration the user cannot see.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The drive search capability MUST accept a set of content statuses drawn from
  Archived, Unpublished and Locked, defaulting to an empty set.
- **FR-002**: An empty set MUST preserve today's behavior exactly: archived content excluded, all
  other content returned.
- **FR-003**: Archived MUST return only archived content, never archived content in addition to
  everything else.
- **FR-004**: Unpublished MUST return only content with no live version, and MUST exclude archived
  content unless Archived is also selected.
- **FR-005**: Locked MUST return only content on which a lock is held, regardless of who holds it.
- **FR-006**: Multiple selected statuses MUST combine with AND — the result is content holding every
  selected state at once.
- **FR-007**: Selecting Archived together with Unpublished MUST return the same set as Archived
  alone, and this MUST be documented rather than treated as a defect.
- **FR-008**: The existing inclusive "show archived" behavior relied on by the legacy Site Browser
  MUST be left unchanged; the exclusive Archived behavior is added alongside it.
- **FR-009**: A status selection MUST produce identical results whether or not a keyword search is
  active, and under every supported search strategy the environment can be configured to use.
- **FR-010**: An unrecognized status value MUST be rejected with a client error, consistent with how
  the drive already rejects unknown field-filter keys.
- **FR-011**: A status selection MUST NOT conflict with a workflow filter that also constrains
  archived content; the two MUST combine into one coherent result set.
- **FR-012**: Users MUST be able to select any combination of the three statuses from a single
  control in the Content Drive toolbar, alongside the existing filters.
- **FR-013**: The control's labels MUST be localizable, following the Content Drive naming
  convention already used by the other filters.
- **FR-014**: The active selection MUST be reflected as a chip, consistent with the other toolbar
  filters, and MUST be clearable from that chip.
- **FR-015**: Folders MUST be excluded from the results whenever any status is selected.
- **FR-016**: A status selection MUST round-trip through the address bar like every other filter, so
  a filtered view is shareable and survives a reload.
- **FR-017**: The existing "Clear all" action MUST clear the status selection.
- **FR-018**: An empty result set arising from a legitimate combination MUST show the standard empty
  state, never an error.
- **FR-019**: The Content Drive request MUST stop pinning archived content off unconditionally; that
  decision MUST come from the status selection instead.
- **FR-020**: The control and each of its options MUST carry stable test identifiers, and the
  control MUST carry an accessible label.

### Key Entities

- **Content Status**: An independent state an item can hold, from a closed set of three — *Archived*
  (removed from circulation but recoverable), *Unpublished* (no live version), and *Locked* (checked
  out by a user). An item may hold several at once, which is what makes the selection additive.
- **Status Selection**: The set of statuses the user has chosen. Empty by default; every member
  narrows the result set further.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An editor can locate archived content from within Content Drive in a single action,
  without leaving for another part of the product.
- **SC-002**: Each single status returns exactly the items in that state and no others, verified
  against a known fixture covering all three states plus unaffected content.
- **SC-003**: Every pair of statuses, and all three together, return exactly the items holding all
  the selected states.
- **SC-004**: With no status selected, result counts and ordering are identical to those produced
  before the filter existed, for the same folder and filters.
- **SC-005**: The same status selection returns the same result set with and without a keyword
  search, and under every supported search strategy.
- **SC-006**: A shared link to a status-filtered view reproduces the same selection and the same
  results for a second user.
- **SC-007**: No legitimate combination — including the redundant and the rarely-populated ones —
  presents as an error to the user.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The shared content-browsing capability behind both Content Drive
  (modern) and the Site Browser (legacy). The legacy Site Browser exposes a "Show Archived" checkbox
  whose meaning is *inclusive* — archived content **in addition to** everything else. The new
  Archived status is *exclusive* — archived content **only**. These are different questions and both
  must remain expressible; the new behavior is added alongside the old one rather than replacing it.
  The legacy Content Search portlet already offers all three of these predicates and already combines
  them additively, so this feature brings Content Drive to parity rather than inventing semantics.
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
- Users of this filter already have permission to see the content it surfaces; the filter narrows a
  result set that permission checks have already constrained, and grants no new visibility.
- The redundancy of Archived + Unpublished is acceptable to expose rather than something to prevent
  in the control. Disabling one option based on another would be a second, hidden rule the user has
  to learn, and the combination is harmless.
- The Shared Assets / System Host toggle is explicitly out of scope, tracked separately in
  [#34760](https://github.com/dotCMS/core/issues/34760).
