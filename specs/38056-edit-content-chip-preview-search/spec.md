# Feature Specification: Edit Content state chip, Preview placement, and asset picker search scope

**Feature Branch**: `nicobytes/new-edit-content-ux-feedback-state-chip-next-to`

**Created**: 2026-10-16

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue dotCMS/core#38056 ("New Edit Content UX feedback: state chip next to the
title, Preview below the workflow actions, and Title / All Fields search in the asset picker";
Team Falcon, priority Medium; second round of UX feedback after #38036). Three changes to the new
Edit Content experience and the asset picker, all in the admin UI, with no backend change:
(1) the content state chip ("Published", "Draft", "New"…) moves from the form's tab bar to right
after the content title, in both the full-screen editor (global header) and the side panel header;
(2) the Preview link moves from the tab bar into the sidebar's Actions tab, below the workflow
action buttons, as a full-width outlined button with an eye icon; (3) the asset picker's search bar
gains the same Title / All Fields scope dropdown Content Drive already has, defaulting to Title.

**Current behavior (checked in a running dotCMS on 2026-10-16)**: in full screen, the tab bar
(Content / SEO / Comments) ends with the state chip, a vertical separator, the "Preview" link, the
⋮ menu and the sidebar toggle; the title sits in the global header under the breadcrumb, with
nothing beside it. A new Blog shows "New" in the same place. In the side panel (opened by
double-clicking an item in Content Drive) the title is in the panel header and the chip is again in
the tab bar. The sidebar's Actions tab lists Lock for Editing, the workflow action buttons, then
Details / Locales / Workflow. The asset picker ("Add Image" → "Select Existing File") has a plain
search box with no scope choice, and today it matches any field. Content Drive's search bar has a
Title / All Fields dropdown with a checkmark on the active option and a tooltip on each option.

## Clarifications

### Session 2026-10-16

- Q: What happens to the state chip in the centered Edit Content dialog (the third way to open the
  editor, used for example from the relationship field and the page editor), which the issue does
  not mention? → A: The dialog also shows the chip next to its title, like the side panel, so no
  surface loses the state when the chip leaves the tab bar.
- Q: What does the chip area show while existing content is still loading (opening the panel,
  switching language)? → A: No chip until the real state is known; existing content never shows a
  default "New" while loading. "New" is shown only for content that has not been saved.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the content state next to its title (Priority: P1)

An editor opens content in the new Edit Content, full screen or in the side panel, and sees the
state of the content (Published, Draft, Archived, Revision, or New) as a chip immediately after the
title, where they look first, instead of in the tab bar next to unrelated controls.

**Why this priority**: It is the main visual change of the issue and the only one that needs a new
surface (the global header and the side panel header have nothing next to the title today).

**Independent Test**: Open an existing published content item full screen and in the side panel;
the chip reads "Published" right after the title in both, and the tab bar has no chip or separator.

**Acceptance Scenarios**:

1. **Given** existing content in full-screen Edit Content, **When** the page loads, **Then** the
   chip shows right after the title in the global header with the same label and color it has today.
2. **Given** existing content in the Edit Content side panel or the centered Edit Content dialog,
   **When** it opens, **Then** the chip shows right after the title in that header.
3. **Given** either view, **When** the page is shown, **Then** neither the chip nor the separator
   beside it appears in the form's tab bar.
4. **Given** content that has not been saved yet, **When** the editor opens it, **Then** the chip
   next to the title reads "New".
5. **Given** published content, **When** the editor runs a workflow action that changes its state
   (for example Unpublish, then Publish), **Then** the chip next to the title updates without a
   page reload.
6. **Given** a title too long for the available space, **When** it is shown in either view,
   **Then** the title is cut with an ellipsis and the chip stays fully visible beside it.
7. **Given** a page that uses the global header but is not Edit Content (for example Content
   Drive), **When** it is shown, **Then** no chip appears next to its title; and after leaving
   Edit Content for such a page, the previous content's chip is gone.
8. **Given** the editor is viewing a historical version, **When** the page is shown, **Then** the
   chip is hidden and the "Previewing …" label still shows in the tab bar, as today.

---

### User Story 2 - Preview below the workflow actions (Priority: P2)

An editor of an HTML page or of content with a URL map finds Preview in the sidebar's Actions tab,
under the workflow action buttons, as a full-width outlined button with an eye icon, instead of a
text link in the tab bar.

**Why this priority**: A small relocation that completes the tab-bar clean-up of Story 1, but it
changes where an existing capability lives, so it must keep its visibility rules and result.

**Independent Test**: Open an existing HTML page; the tab bar has no Preview, the Actions tab shows
the button under the workflow actions, and clicking it opens the same page-editor URL in a new tab.

**Acceptance Scenarios**:

1. **Given** existing content that is an HTML page or has a URL map, **When** the Actions tab is
   shown, **Then** Preview appears below the workflow action buttons as a full-width outlined
   button with an eye icon, with a divider between it and the actions.
2. **Given** that content, **When** the form's tab bar is shown, **Then** it has no Preview.
3. **Given** the editor clicks Preview, **When** the content is an HTML page, **Then** the page
   editor opens in a new tab; **otherwise** the URL-map preview opens in a new tab (the same URLs as
   today).
4. **Given** the Actions tab lists no workflow actions (no workflow selected, or none the user can
   run), **When** it is shown, **Then** Preview still shows, right below Lock for Editing and below
   the "Select a Workflow" warning when that shows.
5. **Given** content that has not been saved yet, or a historical version being viewed, **When**
   the Actions tab is shown, **Then** Preview does not appear.
6. **Given** content that is neither an HTML page nor has a URL map, **When** the Actions tab is
   shown, **Then** Preview does not appear (the rule for who sees it does not change).

---

### User Story 3 - Search assets by Title or All Fields in the asset picker (Priority: P2)

An editor choosing an existing image or file in the asset picker uses the same search bar as
Content Drive: a search box with a scope dropdown (Title or All Fields). A search matches titles
only until the editor picks All Fields.

**Why this priority**: Gives picker users the same control over search precision as Content Drive
and removes a divergence between two surfaces that look alike. It also changes the picker's default
results, so it deserves its own testable slice.

**Independent Test**: Open the picker, type a word that appears only inside an asset's metadata,
confirm no match under Title and a match after switching to All Fields.

**Acceptance Scenarios**:

1. **Given** the picker is open, **When** the editor looks at the search bar, **Then** it has a
   scope dropdown with two options, Title and All Fields, laid out as in Content Drive: a checkmark
   on the active option and a tooltip on each option.
2. **Given** the picker was used before and left on All Fields, **When** it is opened again,
   **Then** the scope is Title.
3. **Given** scope Title, **When** the editor searches a term, **Then** only assets whose title
   matches are listed; **given** All Fields, **then** assets matching in any field are listed (what
   the picker returns today).
4. **Given** a term typed, **When** the editor changes the scope, **Then** the search re-runs with
   the new scope.
5. **Given** a search with a term, **When** the picker asks for results, **Then** the request names
   the scope (Title or All Fields); **given** an empty search box, **then** the request names no
   scope.
6. **Given** Content Drive, **When** the editor uses its search bar, **Then** it behaves as today:
   default Title, scope kept in the URL, same debounce, clear button and keyboard shortcuts.
7. **Given** the picker's left sidebar, **When** the editor uses "Search folders…", **Then** it
   works as today and has no scope dropdown.

---

### Edge Cases

- Existing content still loading (panel just opened, language switch in progress): no chip is
  shown, not even "New"; it appears once the state is known.
- A content title that is empty or still being typed in a new item: the chip still shows ("New")
  and the header does not collapse or overflow.
- The editor opens another content from the related-content trail or switches language/locale:
  the chip follows the content now shown, never the previous one.
- Navigating from Edit Content to another portlet and back quickly: no stale chip remains on the
  other portlet; the chip is correct on return.
- Sidebar closed: Preview is out of sight, like the workflow actions (accepted; not a defect).
- The picker is opened from a host that has no router (the legacy host): the new search bar must
  still work there.
- Re-selecting the already active scope does not re-run the search or reset paging.
- Archived or deleted content shows the Archived chip next to the title, as the tab bar did.
- Several content windows open in sequence (panel closed, then another opened): each panel shows its
  own chip.

## Requirements *(mandatory)*

### Functional Requirements

**State chip**

- **FR-001**: In full-screen Edit Content, the system MUST show the content state chip immediately
  after the title in the global header, with the same label and color as today.
- **FR-002**: In the Edit Content side panel and in the centered Edit Content dialog, the system MUST
  show the chip immediately after the title in that surface's header.
- **FR-003**: The chip and the separator beside it MUST NOT appear in the form's tab bar.
- **FR-004**: For content that has not been saved, the chip MUST read "New"; while existing content
  is still loading, the chip MUST NOT show at all (never a default "New") until its real state is
  known.
- **FR-005**: The chip MUST reflect the current state of the content and update without a reload
  after an action that changes it (Publish, Unpublish, Archive, Save, restore, locale switch).
- **FR-006**: A long title MUST be cut with an ellipsis while the chip stays fully visible, in both
  views.
- **FR-007**: Pages other than Edit Content that use the global header MUST show no chip, including
  right after leaving Edit Content.
- **FR-008**: Other users of the side panel and of the dialog MUST be unaffected: the chip is
  offered only to Edit Content.
- **FR-009**: While a historical version is viewed, the chip MUST be hidden and the "Previewing …"
  label MUST remain in the tab bar.

**Preview**

- **FR-010**: Preview MUST NOT appear in the form's tab bar.
- **FR-011**: The sidebar Actions tab MUST show Preview below the workflow action buttons as a
  full-width outlined button with an eye icon, with a divider between it and the actions.
- **FR-012**: Preview MUST be shown only for existing content that is an HTML page or has a URL
  map, and never for unsaved content or while a historical version is viewed.
- **FR-013**: Clicking Preview MUST open, in a new tab, the same URL as today: the page editor for
  an HTML page, the URL-map preview otherwise.
- **FR-014**: When the Actions tab lists no workflow actions, Preview MUST still show, right below
  Lock for Editing and below the "Select a Workflow" warning when that shows.
- **FR-015**: Preview MUST keep its existing label text and be reachable by keyboard with an
  accessible name.

**Asset picker search**

- **FR-016**: The asset picker's search bar MUST offer a scope dropdown with Title and All Fields
  that looks and works like Content Drive's: same layout, a checkmark on the active option, and a
  tooltip on each option.
- **FR-017**: Every time the picker opens, the scope MUST be Title; the choice MUST NOT be
  remembered between openings.
- **FR-018**: With Title, a search MUST match assets by title only; with All Fields, by any field.
- **FR-019**: Changing the scope while a term is typed MUST re-run the search with the new scope;
  choosing the already active scope MUST NOT.
- **FR-020**: A search with a term MUST send the chosen scope to the server; with an empty box it
  MUST send none.
- **FR-021**: Content Drive's search bar MUST work as today: default Title, scope kept in the URL,
  same debounce, clear and shortcut behavior, and a new search still resets the folder selection.
- **FR-022**: The picker's folder search ("Search folders…") MUST work as today.
- **FR-023**: The search bar MUST be one shared control used by both Content Drive and the picker,
  each keeping its own search state, so the two cannot drift apart; the picker's own keyboard
  shortcut behavior (it is the topmost overlay) MUST stay as it is.
- **FR-024**: The scope dropdown's labels and tooltips MUST be translated like the rest of the UI.
- **FR-025**: The picker's "Clear filters"/non-default-filters indicator MUST treat Title as the
  default scope, so a fresh picker shows nothing to clear and picking All Fields counts as a change.

### Key Entities

- **Content state**: Published, Draft, Archived, Revision, or New (unsaved); derived from the
  content; shown as a chip.
- **Title area**: where the content title is drawn: the global header (full screen), the side panel
  header, or the centered dialog header; the only places that gain the chip.
- **Preview action**: the existing way to open an HTML page in the page editor or a URL-mapped item
  at its public URL; now an Actions-tab button.
- **Search scope**: Title or All Fields; says which fields a search term is matched against; held
  per search surface.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In all three states (New, Published, Archived) the chip is visible next to the title
  in both views with no chip left in the tab bar (checked on both views, 100% of states).
- **SC-002**: After a workflow action that changes state, the chip shows the new state within 1
  second of the action finishing, with no reload.
- **SC-003**: With a 200-character title the chip remains fully visible at the narrowest supported
  panel and window widths.
- **SC-004**: Zero pages other than Edit Content show a chip; verified across the pages using the
  global header.
- **SC-005**: Editors who need Preview find it in the Actions tab; the number of clicks to open the
  preview does not increase when the sidebar is open.
- **SC-006**: In the picker, a term present only in non-title fields returns no results under
  Title and the same results as today under All Fields.
- **SC-007**: Every picker opening starts on Title (100% of openings, including after All Fields
  was used).
- **SC-008**: Existing Content Drive search behavior is unchanged: its existing automated tests pass
  without modification of their expectations.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: the new Edit Content editor (full screen and side panel), the
  platform's global header/breadcrumb, the shared side panel, Content Drive's search bar, and the
  asset picker (the new Angular picker; the older Dojo-based picker is untouched). The new Edit
  Content is the newer product surface; the global header is shared with older portlets.
- **Backward-compatibility expectations**: the server API already accepts a search scope and needs
  no change; the picker's default results change on purpose (All Fields → Title), which must be
  called out in the release notes. Content Drive's URLs (scope kept in the URL) keep working.
  Pages that do not set a chip render exactly as before. Automated end-to-end tests that locate the
  chip, Preview or the picker search must be updated to the new places.
- **Known related decisions**: follow-up to the first round of feedback (#38036); Content Drive's
  Title / All Fields scope and its default of Title; the picker was designed to share Content
  Drive's pieces. The plan will consult `dotCMS/platform-adrs`.

## Assumptions

- The visual design is the pair of mock images attached to issue #38056; where this spec and the
  design differ on spacing or color, the design wins.
- The chip reuses the existing status badge, so its labels, colors and translations are unchanged.
- Preview's button label reuses the existing "Preview" text.
- Only Edit Content sets a chip; the mechanism for showing it must clear itself when Edit Content
  goes away.
- Scope labels and tooltips reuse Content Drive's existing messages; no new languages are needed.
- Out of scope: the stale breadcrumb trail seen in the global header (it showed "Home > Getting
  Started > Welcome" above the correct title on content pages, observed 2026-10-16; it predates
  this issue), the ⋮ menu and sidebar toggle in the tab bar, folder search in the picker, and any
  backend or API change.
- Out of scope: the older Dojo asset picker and legacy (non-new) Edit Content.
