# Issue Resolution Specification: New Edit Contentlet UI polish — header divider, date picker width, relationship table icons, copy button

**Feature Branch**: `issue-37958-edit-content-ui-polish`

**Created**: 2026-10-13

**Updated**: 2026-10-15 — scope narrowed: no general icon migration; only the icons shown in the
issue's screenshots change (the relationship field table). See "Scope change" below.

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37958](https://github.com/dotCMS/core/issues/37958)

**Input**: User description: "Fix New Edit Contentlet UI polish: header divider, date picker width, drag handle, copy button, button icons. Scope is ONLY the 5 points in the issue (the error-banner request from the Slack thread is deferred, out of this spec)."

## Scope change (2026-10-15)

The requirement changed after implementation started: this PR does **not** migrate every
PrimeIcon in the New Edit Contentlet editor to Material Symbols. Only the icons shown in the
issue's screenshots change, which means the relationship field's table: its drag handle, add
(`+`) and remove (`×`) buttons, and its empty-state folder. Every other button, the date
picker trigger, the Add Relationships site chip and the shared `dot-chip-filter` keep their
PrimeIcons, exactly as on `main`.

## Problem Statement *(mandatory)*

A visual review of the New Edit Contentlet editor found these places where it drifts from the
design system:

1. **Header divider** — in the side-panel editor there is no divider line between the header
   (content title + expand/close buttons) and the row that holds the Content / SEO / Comments
   tabs and the command bar. The two rows run into each other.
2. **Date picker width** — the Date, Time and Date and Time picker popup opens as wide as its
   input. A full-width field produces a full-width calendar, with day cells spread far apart.
3. **Relationship table icons** — the relationship field's drag handle is visibly larger than
   the field's other icons (the header's `+` add icon and the row's `×` remove icon), and the
   table mixes PrimeIcons with the editor's Material Symbols.
4. **Copy button** — the Host / Folder field's copy button does not match the standard
   icon-only button: the button is too small and its icon is too big.

**Severity / Impact**: Low. Cosmetic only — no data, save, or workflow behavior is affected.
Every author using the New Edit Contentlet editor sees these on every visit, so the editor
looks unfinished next to the rest of the product.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` (local build), New Edit Contentlet editor enabled, any browser.
The PrimeNG version installed in `core-web` is 21.x.

**Steps to Reproduce**:

1. Open Content Drive and edit any contentlet so it opens in the side panel.
   → No divider line under the title row (issue screenshot 1).
2. Edit content of a type with a full-width Date and Time field and click the field's
   calendar button. → The popup is as wide as the input (issue screenshot 2).
3. Edit content with a relationship field that has at least one related item; hover a row.
   → The `⋮⋮` handle is larger than the `+` and `×` icons (issue screenshot 3).
4. Edit content with a Host / Folder field. → The copy button next to the field is small
   while its glyph is large (issue screenshot 4).

**Expected Behavior**: One divider under the header; a calendar popup of the date picker's
standard width; relationship table icons that are all Material Symbols of the same size; a
copy button that looks like every other icon-only button.

**Actual Behavior**: As described in the Problem Statement.

**Reproducibility**: Always.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content editing — the New Edit Contentlet Angular editor
  (`core-web/libs/edit-content`): its side panel, calendar field, relationship field and
  host/folder field.
- **Suspected surface**: Frontend only (Angular templates, one component method, their specs).
  No Java, no `com.dotmarketing.*`, no REST, no DB or index changes. The legacy (old) Edit
  Content screen is not touched.
- **Related known decisions**:
  - [Angular Standards → Icons](../../docs/frontend/ANGULAR_STANDARDS.md#icons-material-symbols)
    says new markup uses Material Symbols and that existing PrimeIcons must **not** be
    mass-migrated. This spec follows it: only the relationship field's table changes icon set.
  - The same doc puts icons drawn internally by PrimeNG out of scope, which this spec keeps.
  - The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

1. **Divider** — the side panel's drawer header (since #37965 the shared `dot-side-panel` in
   `libs/ui`, used by every side panel in the admin) has no bottom border, and neither does
   the Edit Content area under it; the tab list right below already draws
   `border-b border-surface-200`, but only under its own width. The Edit Content panel sets
   its content area's classes through `DotSidePanelRef.contentStyleClass` (as the legacy
   editor panel does), so the divider is that area's top border and other panels are
   unaffected.
2. **Date picker** — in the installed PrimeNG `DatePicker`, `showOverlay()` records the host
   element's `offsetWidth`, and `onOverlayBeforeEnter()` writes it as an **inline**
   `min-width` on the popup's wrapper (the `p-motion` element around `.p-datepicker-panel`)
   for every non-inline picker. Because the calendar field is `[fluid]="true"`, the host is as
   wide as the field, so the popup is too.
   *Correction to the issue text*: the issue ties this to `[appendTo]="'body'"`, but the code
   applies the `min-width` regardless of `appendTo`, so removing `appendTo` would not fix it.
   No PrimeNG input reaches the wrapper (`panelStyleClass` / `panelStyle` land on the panel
   inside it), so the fix clears the value when PrimeNG emits `onShow` and re-aligns the popup.
3. **Relationship table icons** — the drag handle is drawn as two overlapping
   `pi pi-ellipsis-v` glyphs at `text-[14px]`, which together read larger and heavier than the
   single-glyph `+` and `×`. The `+` and `×` are small `p-button`s with PrimeIcons, and the
   empty state uses `pi pi-folder-open`
   (`dot-relationship-field.component.html`).
4. **Copy button** — the `<p-button>` is declared with `size="small"`
   (`host-folder-field.component.html:80-97`), which shrinks the button around its glyph, so the
   glyph looks too big for it. Without `size="small"` the button takes the default icon-only
   size and the glyph is in proportion. The glyph also carries a `text-lg` class
   (`host-folder-field.component.html:93`) that the issue asks to remove; removing it does
   not change the glyph's rendered size.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- A full-width 1px `surface-200` divider under the side-panel header, in both the 80% and
  expanded (100%) panel.
- Date / Time / Date and Time popups rendered at the date picker's standard content width,
  independent of the input's width, still anchored to the field.
- The relationship field's table icons as Material Symbols at the default icon size: the drag
  handle as one `drag_indicator` (hover/focus/keyboard/drag/disabled behavior unchanged), the
  add and remove buttons as `add` / `close` at the default, rounded icon-only button size, and
  the empty-state folder as `folder_open`.
- The Host / Folder copy button at the default icon-only `p-button` size (removing
  `size="small"`), its glyph at the default button icon size (removing `text-lg`), and
  `check` the same size as `content_copy`.
- Unit specs for each point.

**Explicitly out of scope / non-goals**:

- Migrating any other PrimeIcon in the editor to Material Symbols: other buttons (command
  bar, sidebar, history, workflow, file field, category field, custom field, banners), the
  date picker trigger (`pi pi-calendar` / `pi pi-clock`), the Add Relationships site chip and
  the shared `dot-chip-filter` (scope change 2026-10-15).
- Replacing `<p-button>` with another button component.
- Icons PrimeNG draws internally (date picker month arrows and time spinners, drawer, etc.).
- The legacy (old) Edit Content screen.
- The Slack-thread request to clear the "form invalid" error banner once the author fixes the
  fields — a behavior change, tracked separately (deferred by the developer).

## Regression Risk *(mandatory)*

- **Blast radius**: four `libs/edit-content` components. The calendar field is used by every
  Date / Time / Date and Time field in the editor; the relationship field by every
  relationship. Risks: a popup misplaced after its width changes; the drag handle losing its
  keyboard focus or its `pReorderableRowHandle`; the add/remove buttons losing their
  `data-testid`.
- **Backward compatibility**: No API, content, serialized-state, DB or index change; nothing
  rollback-unsafe. External tests (e2e) that select the relationship add/remove buttons by
  `icon="pi pi-…"` or `.pi-*` would break — none exist in `core-web/apps/*-e2e`.
- **Data considerations**: None.

## Acceptance & Verification *(mandatory)*

**1. Header divider**

- **AC-001**: In the side-panel editor, a 1px horizontal divider runs the full width of the
  panel between the title row and the row with the tabs and the command bar.
- **AC-002**: The divider uses the same color as the tab list's bottom border
  (`surface-200`).
- **AC-003**: The divider shows in both the default (80%) and the expanded (100%) panel.

**2. Date picker popup**

- **AC-004**: On Date, Time and Date and Time fields, the popup is the same width whether the
  field is narrow or full-width.
- **AC-005**: The popup uses the date picker's standard content width and never stretches to
  the input's width.
- **AC-006**: The popup still opens attached to its field (below it, or above when there is
  no room below) and stays fully inside the viewport. *Accepted limit*: when the popup fits
  neither above nor below the field (e.g. a 1000px-tall window with the field mid-panel),
  PrimeNG pins it to the top of the viewport and its 2px offset from the field leaves it 2px
  past the top edge. This is PrimeNG's positioning, identical before and after this change,
  and is not addressed here.

**3. Relationship table icons**

- **AC-007**: The drag handle shows a single `drag_indicator` Material Symbol instead of two
  stacked `pi pi-ellipsis-v` glyphs.
- **AC-008**: The handle glyph is the same size as the row's remove (`×`) and the header's add
  (`+`) icons.
- **AC-009**: Hover and focus still reveal the handle, it is still keyboard-reachable,
  drag-to-reorder still works, and a disabled field still shows no handle.
- **AC-010**: The add (`+`) and remove (`×`) buttons show the `add` / `close` Material Symbols
  at the default icon size, at the default (not small), rounded icon-only button size, and
  keep their `data-testid`.
- **AC-011**: The empty-state folder is the `folder_open` Material Symbol.

**4. Host / Folder copy button**

- **AC-012**: The copy button uses the default icon-only `p-button` size (no `size="small"`).
- **AC-013**: Its glyph renders at the default button icon size, with no per-instance size
  override such as `text-lg`.
- **AC-014**: The copied state (`check`) is the same size as `content_copy`.
- **AC-015**: Tooltip, accessible name and disabled state behave as today.

**Regression**

- **AC-016**: The `edit-content` unit tests pass.

**Verification method**:

- Unit (Vitest + Spectator), written first and confirmed Red, per point: the Edit Content panel
  sets its content area's top border (`border-t border-surface-200`); on `onShow` the popup wrapper's inline `min-width` is cleared
  and the popup re-aligned; the drag handle renders one `drag_indicator` and still has
  `pReorderableRowHandle` / is absent when disabled; the add/remove buttons render `add` /
  `close`, not small, rounded; the empty state renders `folder_open`; the copy button has no
  `size="small"` and its glyph has no `text-lg`.
  Run: `pnpm nx test edit-content` (`-- <file>` while iterating).
- Manual, in the browser against the local instance (widths, viewport placement and pixel
  sizes can't be proven in jsdom): AC-001–AC-006, AC-008, AC-010, AC-013/AC-014, plus
  drag-to-reorder and keyboard focus for AC-009. Compare with the four issue screenshots.

## Assumptions

- Material Symbols Outlined (`material-symbols-outlined`), already loaded globally, is the
  icon family used; no new font or dependency.
- The relationship table's icons use the default icon size (24px), with no size class.
- "Standard content width" of the date picker means the width PrimeNG's theme gives the
  calendar content when nothing forces a `min-width`.
