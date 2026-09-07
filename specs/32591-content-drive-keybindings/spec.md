# Feature Specification: Content Drive Keybindings

**Feature Branch**: `32591-content-drive-keybindings`

**Created**: 2026-09-04

**Status**: Draft

**Issue**: [#32591](https://github.com/dotCMS/core/issues/32591) — [FEAT] CD: Add Keybindings

**Input**: Content authors should be able to navigate, select and filter Content Drive content
using the keyboard conventions they already know from desktop file managers. Delivered on a
reusable shortcut registry so the asset selection dialog inherits the same behaviour.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reach and move through the listing with the keyboard (Priority: P1)

A content author lands on Content Drive and, without touching the pointer, moves down through the
rows of the current page and back up again. Moving through rows changes which row is *focused*; it
does not change what is *selected*.

This story also closes an existing defect. Today the listing is not reliably reachable by keyboard
at all: on a freshly loaded page the arrow keys scroll the listing container instead of moving
between rows, and after sorting a column no row can be reached by keyboard even with Tab.

**Why this priority**: Every other keyboard story depends on a row being focusable. Without this,
range selection has nothing to anchor from and the feature cannot be demonstrated. It is also the
half of the issue that fixes broken behaviour rather than adding new behaviour.

**Independent Test**: Load the portlet, press Tab until focus enters the listing, and move through
every row of the page with the arrow keys, using no pointer input. Then sort a column and repeat.
Delivers a keyboard-operable listing on its own, with no selection changes involved.

**Acceptance Scenarios**:

1. **Given** a freshly loaded multiple-selection listing with rows and no prior pointer interaction,
   **When** the author presses Tab until focus reaches the listing, **Then** exactly one row receives
   focus and it is visibly indicated.
2. **Given** a focused row that is not the last on the page, **When** the author presses Arrow
   Down, **Then** focus moves to the next row, the listing does not scroll independently of the
   focus, and the selection is unchanged.
3. **Given** a focused row that is not the first on the page, **When** the author presses Arrow Up,
   **Then** focus moves to the previous row and the selection is unchanged.
4. **Given** focus on the last row of the page, **When** the author presses Arrow Down, **Then**
   focus stays on that row and the listing does not advance to the next page.
5. **Given** focus on the first row of the page, **When** the author presses Arrow Up, **Then**
   focus stays on that row.
6. **Given** a listing the author has been moving through, **When** they sort a column, change
   page, change page size, apply a filter, or run a search, **Then** the listing remains reachable
   by keyboard and one row is focusable again without any pointer input.
7. **Given** a listing containing rows it marks as unselectable, **When** the author moves through
   the listing with the arrow keys, **Then** focus skips those rows rather than stopping on them.
   Whatever the listing already treats as disabled or unselectable stays that way; the keyboard
   never reaches something the pointer cannot.
8. **Given** the listing is presented in a read-only context (the action preview), **When** the
   author presses the arrow keys, **Then** no row takes focus and no selection changes.

---

### User Story 2 - Extend a selection from the keyboard (Priority: P1)

A content author selects a row, then holds Shift and presses Arrow Down several times to grow the
selection to a contiguous block, exactly as they would in a desktop file manager. Releasing Shift
and pressing an arrow again collapses back to moving focus only.

**Why this priority**: This is the acceptance criterion the issue names ("SHIFT + ARROWS, to select
content") and the reason the feature was requested. It is also the piece with no existing support
to build on.

**Independent Test**: With one row selected, hold Shift and press Arrow Down three times; four
contiguous rows are selected and the toolbar reflects a four-item selection. Delivers keyboard
multi-select on its own.

**Acceptance Scenarios**:

1. **Given** a selected, focused row, **When** the author presses Shift+Arrow Down, **Then** focus
   moves to the next row and both rows are selected.
2. **Given** a selection extended downward over several rows, **When** the author presses
   Shift+Arrow Up, **Then** the selection shrinks from the far end back toward the anchor rather
   than growing upward past it.
3. **Given** a selection anchored on a row, **When** the author reverses direction past the anchor
   with Shift held, **Then** the selection follows through the anchor and extends the other way,
   with the anchor unchanged.
4. **Given** an extended selection, **When** the author presses an arrow key *without* Shift,
   **Then** the selection collapses to the newly focused row and that row becomes the new anchor.
5. **Given** focus on the last row of the page, **When** the author presses Shift+Arrow Down,
   **Then** the selection is unchanged and does not extend onto the next page.
6. **Given** an extended selection, **When** the author changes page, **Then** the selection is
   cleared, because a selection never spans pages.
7. **Given** a keyboard-extended selection, **When** the author then runs an action from the
   toolbar, **Then** the action receives exactly the rows the listing shows as selected.
8. **Given** a range shrunk all the way back, **When** it reaches the anchor, **Then** the anchor row
   stays selected and the range never empties. The anchor is the pivot the range turns through, so it
   belongs to every range by construction.
9. **Given** an existing selection made before the gesture began, **When** the author extends and then
   shrinks a range, **Then** those earlier rows are untouched throughout. A range extends the
   selection, it does not replace it.

---

### User Story 3 - Jump to search with a shortcut (Priority: P1)

A content author anywhere in the portlet presses the search shortcut and starts typing immediately,
without reaching for the pointer to click the search field.

**Why this priority**: Named in the issue ("CMD + K, to search"), independent of the listing work,
and the entry point for the "filter content" half of the user story.

**Independent Test**: From a focused row, press the search shortcut and type; the characters land in
the search field and the listing filters. Delivers on its own with no selection behaviour involved.

**Acceptance Scenarios**:

1. **Given** focus anywhere in the portlet that is not a text field, **When** the author presses the
   search shortcut, **Then** the search field receives focus and any existing term is preserved.
2. **Given** the search shortcut is pressed, **When** the browser also binds that combination to its
   own search affordance, **Then** the browser's affordance does not activate.
3. **Given** the search field already has focus, **When** the author presses the search shortcut
   again, **Then** focus stays in the search field and nothing is disturbed.
4. **Given** the asset selection dialog is open over the portlet, **When** the author presses the
   search shortcut, **Then** the dialog's own asset search field receives focus, not the search
   field on the page behind it.
5. **Given** the asset selection dialog is closed again, **When** the author presses the search
   shortcut, **Then** focus returns to the portlet's search field.

---

### User Story 4 - Extend a selection by Shift-clicking a checkbox (Priority: P2)

A content author ticks one row's checkbox, then Shift-clicks another row's checkbox further down,
and every row between the two becomes selected.

**Why this priority**: Requested on the issue as a follow-on to the arrow-key work, and it shares
the anchor-and-extend behaviour built for Story 2, so it is inexpensive once that exists. Lower than
P1 because the pointer path already has a working per-row path today.

**Independent Test**: Tick row 2's checkbox, Shift-click row 5's checkbox, and confirm rows 2
through 5 are selected. Fully testable on its own.

**Acceptance Scenarios**:

1. **Given** one row selected via its checkbox, **When** the author Shift-clicks a checkbox further
   down the page, **Then** every row from the first to the clicked row is selected.
2. **Given** one row selected via its checkbox, **When** the author Shift-clicks a checkbox further
   up the page, **Then** every row from the clicked row to the first is selected.
3. **Given** nothing is selected, **When** the author Shift-clicks a checkbox, **Then** only that
   row is selected and it becomes the anchor.
4. **Given** a Shift-click range that spans a row the listing marks as unselectable, **When** the
   range is applied, **Then** that row is excluded from the selection and the rest of the range is
   selected.

---

### User Story 5 - Back out of a selection, then out of every filter (Priority: P3)

A content author who has narrowed the listing down and selected some rows presses Escape to back
out. The first press drops the selection. With nothing selected, the next press clears every active
filter at once, returning the listing to its default view.

Escape is deliberately the keyboard equivalent of the toolbar's existing **Clear all** control, not
a separate behaviour. Same effect, same conditions, so there is one rule to learn and a visible
button that advertises it.

**Why this priority**: A cheap and conventional escape hatch, but no author is blocked without it,
and it must not interfere with the surfaces that already claim Escape.

**Independent Test**: Apply two filters and select rows; press Escape and confirm the selection
clears with the filters intact; press Escape again and confirm every filter clears and the listing
returns to its default view. Testable on its own.

**Acceptance Scenarios**:

1. **Given** one or more rows selected, **When** the author presses Escape, **Then** the selection
   clears and every active filter is left untouched.
2. **Given** no selection and at least one active filter, **When** the author presses Escape,
   **Then** every active filter clears at once, exactly as activating **Clear all** would, and the
   listing returns to the first page of its default view.
3. **Given** filters are cleared this way, **When** the listing reloads, **Then** the filters that
   have defaults are re-seeded to those defaults rather than emptied, so the listing is never left
   in a state the author could not reach through the interface.
4. **Given** focus is inside the search field with no rows selected, **When** the author presses
   Escape, **Then** every active filter clears, not only the search term. The rule does not change
   with where focus sits.
5. **Given** no selection and no active filter, **When** the author presses Escape, **Then** nothing
   happens and no error is surfaced. This is the same condition under which **Clear all** is not
   offered.
6. **Given** a dialog or the content editing panel is open over the portlet, **When** the author
   presses Escape, **Then** that surface closes and neither the selection nor the filters are
   affected.
7. **Given** the author has navigated into a folder, **When** they clear the filters with Escape,
   **Then** the browsed folder is unchanged. Escape clears filters, it does not navigate.

---

### User Story 6 - Toggle the folder tree (Priority: P3)

A content author collapses the folder tree to give the listing more room, and expands it again,
without leaving the keyboard.

**Why this priority**: Convenience only. The toggle is already available as a toolbar control.

**Independent Test**: Press the toggle shortcut twice and confirm the tree collapses then expands.

**Acceptance Scenarios**:

1. **Given** the folder tree is expanded, **When** the author presses the toggle shortcut, **Then**
   the tree collapses and the listing widens.
2. **Given** the folder tree is collapsed, **When** the author presses the toggle shortcut, **Then**
   the tree expands.
3. **Given** focus is inside a rich text editing surface, **When** the author presses the toggle
   shortcut, **Then** the editor's own handling of that combination applies and the tree does not
   toggle.

---

### Edge Cases

- **Empty listing**: with no rows, the shortcuts that act on rows do nothing and surface no error;
  the search shortcut still works.
- **Listing still loading**: keys pressed while rows are being fetched do not queue up and fire
  against the rows that arrive.
- **A single row**: arrow keys keep focus on it; Shift+Arrow leaves the selection at one row.
- **Focus inside a text field**: shortcuts that would otherwise act on the listing do not fire while
  the author is typing in a text field, except the search shortcut, which is a no-op there.
- **Two search fields on screen**: when the asset selection dialog is open it has both an asset
  search and a sites-and-folders search; the search shortcut targets the asset search.
- **Overlapping claims on the same key**: Escape is already claimed by open dialogs and the content
  editing panel, and the tree-toggle combination is claimed by rich text editing surfaces. The
  topmost surface wins in both cases.
- **Modifier held across a page change**: a page change while Shift is held does not carry the
  anchor into the new page.
- **Read-only listing**: the action preview presents the same listing read-only; no shortcut may
  change a selection there.

## Requirements *(mandatory)*

### Functional Requirements

**Keyboard reachability and movement**

- **FR-001**: A **multiple-selection** listing MUST expose exactly one row at a time as a keyboard tab
  stop, so a single Tab press moves focus into the listing rather than through every visible row.
  Single-selection listings are out of scope: see Out of Scope for why, and note their behaviour is
  unchanged by this feature rather than degraded.
- **FR-002**: Arrow Up and Arrow Down MUST move focus between rows on the current page without
  changing the selection, and MUST suppress the listing container's own scrolling for those keys.
- **FR-003**: Focus MUST stop at the first and last row of the current page; no shortcut in this
  feature may trigger pagination.
- **FR-004**: Keyboard reachability MUST be restored automatically after every listing state change,
  including sort, page change, page-size change, filter change and search.
- **FR-005**: Rows the listing marks as unselectable MUST be skipped when moving focus. This is a
  single rule about the listing's existing unselectable state, whatever sets it, rather than a list
  of specific conditions. No keyboard path may reach a row the pointer cannot.
- **FR-006**: A read-only listing MUST NOT take row focus or accept any selection shortcut.

**Selection**

- **FR-007**: The listing MUST maintain a selection *anchor*: the row a range extends from.
- **FR-008**: A plain arrow key press MUST move both focus and the anchor to the newly focused row.
- **FR-009**: Shift+Arrow MUST move focus and extend the selection from the anchor to the newly
  focused row, leaving the anchor where it is.
- **FR-010**: Reversing direction with Shift held MUST shrink the range back toward the anchor and,
  past it, extend the other way. The anchor itself MUST remain selected throughout: a range is
  anchor-to-focus inclusive, so it can shrink to one row but never to none.
- **FR-010a**: A range MUST extend whatever was already selected, not replace it. Rows selected before
  the gesture began survive both the extension and the shrink.
- **FR-011**: Shift-clicking a row checkbox MUST select every row between the anchor and the clicked
  row inclusive.
- **FR-012**: A selection MUST NOT span pages. Any range is bounded by the current page, and changing
  page clears the selection.
- **FR-013**: Rows the listing marks as unselectable MUST be omitted from a range without
  interrupting the rest of the range.
- **FR-014**: Whatever the selection mechanism, the rows reported to the surrounding portlet MUST
  match the rows the listing displays as selected, so that toolbar actions act on exactly that set.

**Search shortcut and the registry**

- **FR-015**: A shortcut MUST move focus to the active search field from anywhere in the portlet,
  preserving any term already entered.
- **FR-016**: The system MUST prevent the browser's own handling of any combination it claims, so no
  browser affordance activates instead.
- **FR-017**: Shortcuts MUST be registered against a scope, and when scopes overlap the most recently
  opened scope MUST receive the key. Closing a scope MUST return the key to the scope beneath it.
- **FR-018**: The shortcut mechanism MUST be reusable by any surface that presents a search field or
  the shared listing, without that surface depending on Content Drive.
- **FR-019**: A surface MUST be able to decline a shortcut so the key falls through to the surface
  beneath or to the browser.

**Escape and the tree toggle**

- **FR-020**: Escape MUST clear the selection when one exists, leaving filters untouched.
- **FR-020a**: With no selection, Escape MUST clear every active filter at once and produce exactly
  the same result as the toolbar's existing **Clear all** control, including re-seeding the filters
  that have defaults rather than emptying them, and returning to the first page.
- **FR-020b**: With neither a selection nor an active filter, Escape MUST do nothing. This is the
  same condition under which **Clear all** is not offered.
- **FR-020c**: Escape MUST NOT change the browsed folder.
- **FR-021**: Escape MUST be handled by an open dialog or the content editing panel before it reaches
  the listing.
- **FR-022**: A shortcut MUST toggle the folder tree, and MUST NOT fire while focus is inside a text
  editing surface that claims the same combination.

**Documentation**

- **FR-023**: Every shortcut shipped MUST be documented for authors. No in-application shortcut help
  surface is built.

  > **Partially satisfied in this repository, by design.** `docs/frontend/KEYBOARD_SHORTCUTS.md`
  > documents the shortcuts for *developers*. The author-facing list belongs on the public
  > documentation site, which is outside this repository, so it is an external deliverable this PR
  > cannot close. The registry exposes `activeShortcuts()` with each label so that page can be
  > generated from the live set rather than hand-maintained and left to drift.

### Key Entities

- **Shortcut registration**: a claim on a key combination by a surface, carrying the combination, a
  human-readable label for documentation, the scope it belongs to, and what to run. Registrations are
  added when a surface opens and withdrawn when it closes.
- **Scope**: a nesting level of the interface (the portlet, a dialog over it, a panel over that).
  Scopes stack; the topmost scope holding a claim on a combination receives the key.
- **Selection anchor**: the row a range extends from. Distinct from the focused row, which is where
  the keyboard cursor currently sits.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From a freshly loaded portlet, an author can reach the listing and visit every row on
  the current page using only the keyboard, with zero pointer input.
- **SC-002**: Keyboard reachability survives all five listing state changes (sort, page change,
  page-size change, filter change, search) with no pointer input needed to recover it. Today it
  survives none of them.
- **SC-003**: After anchoring on a row, an author can select a contiguous block of N rows on the
  current page in N-1 further keystrokes.
- **SC-004**: An author can move focus into the search field in a single keystroke from anywhere in
  the portlet, and the browser's own search affordance never activates in its place.
- **SC-005**: The same shortcut set behaves identically when the shared listing and search are
  presented inside the asset selection dialog, with the dialog's field receiving the search shortcut
  rather than the one behind it.
- **SC-006**: Every shortcut shipped is covered by automated tests. The baseline is zero: the
  repository currently contains no keyboard tests for the shared listing.
- **SC-007**: Existing automated coverage for the shared listing and the Content Drive portlet
  continues to pass unchanged.
- **SC-008**: No shortcut can destroy, publish, archive or otherwise mutate content. This feature
  moves focus, changes selection, toggles a panel and clears filters, and nothing else. Clearing
  filters is reversible by re-applying them; nothing it does is destructive.
- **SC-009**: Pressing Escape with nothing selected leaves the listing in exactly the state that
  activating the **Clear all** control leaves it in, verified against the same set of active filters.

## Out of Scope

Each of the following was considered and deliberately excluded. Recorded so the decision is not
relitigated, and so a reviewer can see it was a choice.

- **Cross-page ranges and cross-page selection**: a selection is bounded by the current page. Holding
  a selection across fetches is a substantially larger change to how the listing owns its state.
  Note that the existing behaviour of clearing the selection when the rows change is what *enforces*
  this rule; it must not be removed as dead code.
- **Enter and Space bindings**: both are already claimed by the listing's existing row-selection
  behaviour. Rebinding them to "open" would diverge from what the component does everywhere else it
  is used.
- **A Delete key binding**: Content Drive has no plain delete. Removal happens through
  permission-dependent workflow actions that differ per item, so a Delete key would do nothing on
  some rows and destroy content on others. That inconsistency is worse than the absence.
- **Go up one folder level**: the author has the folder tree and the breadcrumb for this.
- **Type-ahead row jumping**: the listing is server-paged, so typing letters could only ever match
  the rows already on screen. The search shortcut covers the real need.
- **Cut, copy and paste**: there is no clipboard model to bind to. Worth noting separately that
  moving items is currently pointer-only (drag and drop), so keyboard-only authors cannot move items
  at all. That is an accessibility gap and belongs in its own issue.
- **A refresh binding**: the browser already provides one.
- **A rename binding**: folders have an edit dialog and items have no rename at all, so the binding
  would behave differently depending on what is focused.
- **An in-application shortcut help overlay**: the shortcuts are documented instead.
- **Back and forward navigation, and focus-only movement with a modifier held**: both already work
  today. Nothing to build.
- **A single tab stop in single-selection listings** (the asset selection dialog). The underlying
  table short-circuits its tab-stop calculation to "every row is tabbable" whenever the selection is
  empty, and an empty selection is the permanent resting state of a single-selection list, so the
  anchor this feature drives is never consulted. Confirmed structural rather than a timing artifact.
  Delivering it would mean a second, different tab-stop mechanism living alongside the first in one
  shared component, for a surface outside this issue. That consumer's behaviour is **unchanged** by
  this work, not degraded, so this is a deferred improvement rather than a regression. Proposed as a
  follow-up alongside the keyboard-move gap below.
- **Full grid semantics for assistive technology** (announcing the listing as a grid, announcing each
  row's selected state): adjacent to this work and worth doing, but a separate scope. To be proposed
  as a follow-up issue.

## Assumptions

- The shortcut combinations follow the conventions authors already know from desktop file managers
  and web applications: a modifier plus K for search, Shift plus arrows to extend a selection, Escape
  to back out. Platform-appropriate modifiers are used, so the primary modifier differs between
  macOS and other platforms.
- Authors operate Content Drive in a current desktop browser. Touch-only and screen-reader-primary
  operation are not the target of this feature, though nothing here may make them worse.
- The listing's existing pointer behaviour is correct and stays as it is. This feature adds keyboard
  paths and one pointer path (Shift-click on a checkbox); it does not redesign selection.
- **Unselectable rows are defined by the listing, not by this feature.** FR-005 and FR-013 are written
  against whatever the listing already marks unselectable, so they hold as that definition grows. At
  the time of writing the trunk listing disables rows only in bulk (a disabled or read-only listing);
  richer per-row states are arriving on other branches. The behaviour itself needs no new code — the
  underlying table already steps over unselectable rows — so this is a matter of when the per-row case
  becomes *observable* in a test, not when it starts working.
- The shared listing component keeps its current three consumers. New consumers inherit the
  behaviour rather than opting in.
- No backend, API or persistence change is required. This is entirely a browser-side feature.
- The work is scoped to a single page of rows at a time, consistent with how the listing already
  fetches and discards rows.

## Investigation Findings (verified)

Recorded because they were measured against this codebase, not assumed, and because two of them mean
this feature is partly a defect fix. This section intentionally carries more implementation detail
than the rest of the spec; it is evidence for the plan phase, not a design.

**Two of the issue's acceptance criteria are currently broken, not merely absent.**

- The shared listing gives each row a selection binding but never tells it *which row it is*. Every
  range operation in the underlying table component keys off that index, so Shift-click ranges and
  the component's own range shortcuts are silently inert today. This is a pre-existing defect.
- Arrow keys currently do nothing but scroll. The table component's key handling lives on the row
  itself, so it only runs when a row genuinely has focus, and no row is ever focused on load. The
  arrow therefore falls through to the scroll container.
- Row focusability was measured directly. On load, every row is reachable (a 20-stop tab trap rather
  than a single tab stop). **After sorting a column, no row is reachable at all** — the table nulls
  the value its tab-stop calculation compares against. Reproduction: load the portlet, sort any
  column, then press the arrow keys without clicking a row first.

**The fix is cheaper than it first appeared.**

- Supplying the row index makes every row unreachable, which is the trap that has to be handled. But
  the table's own tab-stop calculation can be *driven* rather than overridden: pointing the anchor at
  the intended row produces exactly one tab stop, measured. So a roving tab stop comes from the
  library's existing behaviour, with no override of it. It must be re-seeded after sort and page
  change, where the library clears it, and the component already has handlers at both points.
- Supplying the row index was verified safe against existing coverage: the shared listing's 216 tests
  and the portlet's 1,472 tests all pass with it in place.

**Known remaining risk, to be addressed in the test strategy.**

- Range selection has to survive a round trip through the portlet, which owns the selection and
  re-asserts it back onto the listing while the table component is simultaneously suppressing its own
  propagation. This loop is already the subject of several defensive comments in the component and is
  the one part of this feature that was not proved out during investigation. It should be the first
  failing test written.

**Regression surface.**

- The shared listing has three consumers: Content Drive, the read-only action preview, and the asset
  selection dialog (which uses single-row selection). Supplying the row index changes tab-stop
  behaviour in all three, so all three need coverage.
- The repository contains **zero** keyboard tests for this component today, which is precisely why
  the existing green suites cannot be cited as evidence of no regression. The tests are the
  deliverable, not a follow-up.

**Environment.**

- Content Drive is a native route in the admin application, not a legacy embedded portlet, so
  document-level key handling reaches it.

**Two conflicts the registry must resolve.**

- The content editing panel already claims Escape to close itself, so Escape must reach it before the
  listing.
- That same panel hosts a rich text editing surface that claims the tree-toggle combination for text
  formatting, so the toggle must not fire while such a surface has focus.
