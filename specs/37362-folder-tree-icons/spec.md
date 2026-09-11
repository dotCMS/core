# Feature Specification: Shared folder tree — state-aware folder icons

**Feature Branch**: `nicobytes/37362-shared-folder-tree-restore-folder-icons-in-content-drive-and-fix-icon-state-on-collapse`

**Created**: 2026-09-03

**Status**: Draft

**Type**: Defect

**Issue**: [#37362](https://github.com/dotCMS/core/issues/37362)

**Input**: User description: "https://github.com/dotCMS/core/issues/37362 — validate first; the collapse icons are probably already restored"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A content author recognises folders in Content Drive at a glance (Priority: P1)

A content author opens Content Drive and scans the sidebar tree to find where their content lives.
Every folder row is identifiable as a folder without reading its name, and the row tells the author
whether that folder is currently opened or closed. Today the rows carry only an expand/collapse
chevron, so the tree reads as an anonymous outline and the author has lost the cue that was there
before the folder tree was unified.

**Why this priority**: This is the visible regression on Content Drive's main navigation surface —
the sidebar is on screen for the whole session, and it is the part of the report that a user can
see without any special setup.

**Independent Test**: Open Content Drive with a site that has folders and confirm each folder row
shows a folder marker beside its name, and that the marker changes between the closed and open
state as the row is expanded and collapsed. Delivers the restored navigation cue on its own, with
no other part of this work in place.

**Acceptance Scenarios**:

1. **Given** Content Drive is open on a site that has folders, **When** the author looks at the
   sidebar tree, **Then** every folder row shows a folder marker next to the folder name.
2. **Given** a collapsed folder row with children, **When** the author expands it, **Then** the
   row's marker changes to the open-folder state.
3. **Given** that same row now expanded, **When** the author collapses it, **Then** the marker
   returns to the closed-folder state.
4. **Given** the site row at the top of the tree, **When** the author looks at it, **Then** it is
   still presented as a site and not as a folder, so the two remain distinguishable.
5. **Given** a "load more" row inside a level, **When** the author looks at it, **Then** it carries
   no folder marker, because it is an action and not a folder.

---

### User Story 2 - A folder's marker never lies about its state in the Site/Folder field (Priority: P2)

A content editor picking a destination folder in a Site or Folder field opens the picker, expands a
folder to look inside it, then collapses it again. The row's marker follows the row: open while the
folder is open, closed once it is closed. It never stays stuck in the open state on a folder the
editor has already closed.

**Why this priority**: A marker stuck in the open state contradicts what the row is showing and
makes the editor doubt whether the click registered. Lower than P1 because it affects one state
transition in an overlay rather than a permanently visible surface — and because current code
review suggests the picker may already behave correctly (see Assumptions), which would reduce this
story to locking the behaviour in with regression coverage.

**Independent Test**: Open a Site/Folder field picker, expand a folder with children, collapse it,
and confirm the marker matches the row's state at each step. Testable without touching Content
Drive.

**Acceptance Scenarios**:

1. **Given** the Site/Folder field picker is open on a site with folders, **When** the editor
   expands a folder with children, **Then** its marker shows the open state.
2. **Given** that folder is expanded, **When** the editor collapses it, **Then** its marker reverts
   to the closed state.
3. **Given** a folder whose children are still loading, **When** the editor looks at the row,
   **Then** the row shows that it is loading, and once loading finishes the marker matches the
   row's resulting state — with no duplicated or missing marker at any point.
4. **Given** a preselected folder deep in a site, **When** the picker opens with that branch already
   expanded and the editor collapses one of its ancestors, **Then** that ancestor stays collapsed
   with a closed marker and does not re-open on its own.

---

### User Story 3 - The tree reads the same wherever it appears (Priority: P3)

A user who moves between Content Drive, the Site/Folder field, the Browser Selector sidebar and the
Asset Picker sidebar sees the same folder tree conventions in all of them, because one shared tree
decides how a folder row presents its state rather than each screen deciding for itself.

**Why this priority**: This is the durability of the fix rather than the fix itself. Without it, the
two symptoms in this report can drift apart again the next time one consumer is touched.

**Independent Test**: Compare a folder row in each of the four consumers and confirm the folder
marker and its state behaviour are the same; confirm the behaviour is configured in one place, not
re-implemented per screen.

**Acceptance Scenarios**:

1. **Given** the four consumers of the shared folder tree, **When** a folder row is expanded and
   collapsed in each, **Then** the marker behaves identically in all of them.
2. **Given** a new screen adopting the shared folder tree, **When** it turns on folder markers,
   **Then** it gets the correct expand/collapse behaviour without writing any icon logic of its own.
3. **Given** the Browser Selector and Asset Picker sidebars, **When** this work ships, **Then** their
   trees look and behave as they did before it.

---

### Edge Cases

- **A folder with no children**: the row must not offer an expand/collapse affordance that implies
  it can be opened; it still shows a closed-folder marker.
- **A folder reported as expandable whose children come back empty**: the row must not be left
  showing an open marker over nothing.
- **The site row in Content Drive**: it is a site, not a folder, and keeps its own identity while
  still being collapsible.
- **The "load more" row**: an action row, not a folder — no folder marker.
- **A level that is loading**: the loading indicator replaces the affordance for the duration; the
  folder marker must not be dropped or drawn twice.
- **Flat search results in the Site/Folder field**: rendered by the search-results list, not by the
  tree; unchanged by this work.
- **Long folder names**: adding a marker must not push the name into overflowing or clipping the
  row, at any indentation depth.
- **A deeply nested branch opened from a preselected value**: every already-open ancestor shows the
  open marker on first paint, not only after a manual click.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Content Drive's sidebar tree MUST present a folder marker next to the name of every
  folder row.
- **FR-002**: A folder row's marker MUST reflect that row's current state: closed when the folder is
  collapsed, open when it is expanded.
- **FR-003**: Collapsing an expanded folder MUST return its marker to the closed state, in every
  screen that renders the shared folder tree.
- **FR-004**: The decision of what marker a folder row shows for a given state MUST live in the one
  shared folder tree, with each screen opting in through configuration — no screen may re-implement
  it.
- **FR-005**: A row known to have no children MUST NOT present an expand/collapse affordance.
- **FR-006**: A site row MUST remain visually distinguishable from a folder row.
- **FR-007**: Rows that are not folders — the "load more" action row in particular — MUST NOT show a
  folder marker.
- **FR-008**: While a level's children are loading, the row MUST show that it is loading and MUST NOT
  show a missing or duplicated marker before, during, or after the load.
- **FR-009**: The Browser Selector sidebar, the Asset Picker sidebar and the Site/Folder field picker
  MUST keep their existing tree appearance and behaviour apart from the corrections named in FR-002
  and FR-003.
- **FR-010**: Expand/collapse MUST stay operable by keyboard and MUST keep announcing the row's state
  to assistive technology; the marker is a visual cue and MUST NOT be the only carrier of that state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of folder rows in the Content Drive sidebar show a folder marker, verified on a
  site with at least three nesting levels.
- **SC-002**: In each of the four screens that render the shared folder tree, expanding a folder and
  then collapsing it returns the row to exactly the appearance it had before the expansion — 4 of 4
  screens pass.
- **SC-003**: The rule that maps folder state to a marker exists in exactly one place; the number of
  per-screen re-implementations is zero.
- **SC-004**: Automated regression coverage exists for the expand-then-collapse marker transition and
  fails if the marker is left in the wrong state.
- **SC-005**: Every existing automated test covering the folder tree and its four consumers passes
  without being weakened or rewritten to accommodate the change.
- **SC-006**: A user shown the tree in any two of the four screens cannot tell, from folder-row
  presentation alone, which screen they are looking at.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The folder tree shared by Content Drive's sidebar, the Site/Folder
  field picker, the Browser Selector sidebar and the Asset Picker sidebar. This is modern product
  surface (the tree was unified in #36733 / PR #36848), but the Browser Selector reaches deep into
  older admin flows, which is why "no visible change there" is an explicit requirement rather than
  an assumption.
- **Backward-compatibility expectations**: Purely presentational. No content, API, or stored data is
  affected, and no admin workflow may change beyond the corrected marker. The Content Drive change
  is a restoration of behaviour users had before #36848; the Site/Folder field change is a
  correction, not a redesign. No deprecations intended.
- **Known related decisions**: The unification of four separate folder trees into one shared
  component (#36733) is the decision this work must respect — the fix belongs in the shared tree,
  not in the screens that consume it, and this report exists precisely because that unification
  dropped a presentation detail. Related: the split-from report #37174, the parent epic #36702, and
  the QA feedback on #36733. The plan phase will consult `dotCMS/platform-adrs` for any binding
  decision on shared presentational components.

## Assumptions

- **Scope is presentational only.** No change to which folders load, how they are fetched, how they
  paginate, or what a click selects.
- **"Folder marker" means the closed/open folder pair already used by the other consumers** — a
  closed folder when collapsed, an open folder when expanded. This spec deliberately names the
  behaviour rather than the glyph; the plan chooses the glyph.
- **The marker is additive to the expand/collapse affordance, not a replacement for it.** Rows keep
  a way to expand and collapse; the marker is a second, state-reflecting cue. Removing the
  affordance in favour of a clickable marker is out of scope.
- **Nothing here changes the site row's identity.** Content Drive's top row stays a site.
- **The flat search-results view in the Site/Folder field is out of scope** — it is a separate list
  with nothing to expand.

### Validation of the reported symptoms

Both symptoms were re-checked against the current branch (`main` @ `4a82203f77`) before writing this
spec, because the request was to confirm whether the second one had already been fixed:

- **Content Drive folder markers — confirmed still missing.** Before #36848, Content Drive supplied
  its own expand/collapse affordance that could render a folder marker, driven by a per-tree display
  mode; both that markup and the styles selecting between its modes were removed when the tree was
  unified. Content Drive's folder rows are now built without any marker at all (only its site row
  carries one), and the shared tree's affordance renders a chevron only. So there is nowhere for a
  folder marker to come from, and US1 is real work.
- **Site/Folder field collapse state — likely already correct, still needs a runtime check and
  regression coverage.** The rows in that picker are built with both a collapsed and an expanded
  marker, and the shared tree library resolves which one to draw from the row's current state on
  every render, so a collapse should revert it. Nothing in the picker's state pins a row to
  "expanded" after the initial preselected-branch expansion. This matches the reporter's hunch that
  the collapse behaviour has since been restored — but it is a read of the code, not an observation
  of the running product, so US2 stays in scope as **verify, then cover**: if the runtime check
  passes, US2's deliverable is the regression coverage in SC-004 and no behavioural change. Note
  also that this symptom predates #36848 (the picker's marker handling came in with #36449), so it
  is not part of the same regression as US1 even though it was reported alongside it.
- **Content Drive's pre-#36848 look was narrower than the report implies.** In that version Content
  Drive turned the folder marker on for the *first root row only*, showing chevrons on every other
  row. The report's acceptance criteria ask for a marker on folder rows generally, with state
  reflected — which is the behaviour the other three consumers already have. This was put to the
  requester and **decided: every folder row gets a state-reflecting marker**, matching the other
  three screens, rather than a literal restoration of the first-row-only mode. That mode is not
  being reinstated and the shared tree is not gaining a switch for it; reintroducing either is a
  scope change, not a detail.
