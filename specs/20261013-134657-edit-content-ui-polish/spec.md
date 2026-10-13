# Issue Resolution Specification: New Edit Contentlet UI polish — header divider, date picker width, drag handle, copy button, button icons

**Feature Branch**: `issue-37958-edit-content-ui-polish`

**Created**: 2026-10-13

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37958](https://github.com/dotCMS/core/issues/37958)

**Input**: User description: "Fix New Edit Contentlet UI polish: header divider, date picker width, drag handle, copy button, button icons. Scope is ONLY the 5 points in the issue (the error-banner request from the Slack thread is deferred, out of this spec)."

## Problem Statement *(mandatory)*

A visual review of the New Edit Contentlet editor found five places where it drifts from the
design system:

1. **Header divider** — in the side-panel editor there is no divider line between the header
   (content title + expand/close buttons) and the row that holds the Content / SEO / Comments
   tabs and the command bar. The two rows run into each other.
2. **Date picker width** — the Date, Time and Date and Time picker popup opens as wide as its
   input. A full-width field produces a full-width calendar, with day cells spread far apart.
3. **Drag handle size** — the relationship field's drag handle is visibly larger than the
   field's other icons (the header's `+` add icon and the row's `×` remove icon).
4. **Copy button** — the Host / Folder field's copy button does not match the standard
   icon-only button: the button is too small and its icon is too big.
5. **Button icons** — several buttons in the editor still draw their icon with PrimeIcons
   (`pi pi-*`) instead of Material Symbols, so icons in the same screen come from two
   different icon sets with different weights and sizes.

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
5. Look at the command bar's `⋮` menu, the sidebar's history/close buttons, the workflow
   pencil/replay buttons, the file field's actions, the category chip `×`, etc.
   → These show PrimeIcons glyphs, while the side panel's expand/close buttons show
   Material Symbols.

**Expected Behavior**: One divider under the header; a calendar popup of the date picker's
standard width; a drag handle the same size as the field's other icons; a copy button that
looks like every other icon-only button; Material Symbols on every button icon in the editor.

**Actual Behavior**: As described in the Problem Statement.

**Reproducibility**: Always.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content editing — the New Edit Contentlet Angular editor
  (`core-web/libs/edit-content`), specifically its side panel, calendar field, relationship
  field, host/folder field, sidebar, command bar, file field, category field, custom (iframe)
  field and the editor layout's top-bar banners.
- **Suspected surface**: Frontend only (Angular templates, one util, their specs). No Java,
  no `com.dotmarketing.*`, no REST, no DB or index changes. The legacy (old) Edit Content
  screen is not touched.
- **Related known decisions**:
  - [Angular Standards → Icons](../../docs/frontend/ANGULAR_STANDARDS.md#icons-material-symbols)
    says new markup uses Material Symbols and that existing PrimeIcons must **not** be
    mass-migrated. Point 5 is a deliberate, scoped exception approved in the issue: it covers
    only the icons on `<p-button>`s (plus the date picker trigger) inside `libs/edit-content`.
  - The same doc puts icons drawn internally by PrimeNG out of scope, which this spec keeps.
  - The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

1. **Divider** — the drawer's `#header` template in
   `dot-edit-content-side-panel.component.html` has no bottom border; the tab list right below
   already draws `border-b border-surface-200`, but only under its own width.
2. **Date picker** — in the installed PrimeNG `DatePicker`, `showOverlay()` records the host
   element's `offsetWidth`, and `onOverlayBeforeEnter()` writes it as an **inline**
   `min-width` on the popup for every non-inline picker. Because the calendar field is
   `[fluid]="true"`, the host is as wide as the field, so the popup is too.
   *Correction to the issue text*: the issue ties this to `[appendTo]="'body'"`, but the code
   applies the `min-width` regardless of `appendTo`, so removing `appendTo` would not fix it.
   Because the value is written inline after the template's `panelStyle` is applied, the fix
   has to win over an inline style; the plan confirms the exact mechanism.
3. **Drag handle** — drawn as two overlapping `pi pi-ellipsis-v` glyphs at `text-[14px]`
   (`dot-relationship-field.component.html:123-124`), which together read larger and heavier
   than the single-glyph `+` and `×`.
4. **Copy button** — the `<p-button>` is declared with `size="small"`
   (`host-folder-field.component.html:80-97`), which shrinks the button around its glyph, so the
   glyph looks too big for it. Without `size="small"` the button takes the default icon-only
   size and the glyph is in proportion. The glyph also carries a `text-lg` class
   (`host-folder-field.component.html:93`) that the issue asks to remove; removing it does
   not change the glyph's rendered size.
5. **Button icons** — ~30 `icon="pi pi-*"` usages on `<p-button>` across 12 templates (list in
   the issue's Additional Context), plus `pi pi-calendar` / `pi pi-clock` set as the date
   picker trigger in `calendar-field.util.ts`.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- A full-width 1px `surface-200` divider under the side-panel header, in both the 80% and
  expanded (100%) panel.
- Date / Time / Date and Time popups rendered at the date picker's standard content width,
  independent of the input's width, still anchored to the field and inside the viewport.
- The relationship drag handle drawn as one `drag_indicator` Material Symbol, the same size
  as the field's add and remove icons, with its current hover/focus/keyboard/drag/disabled
  behavior unchanged.
- The Host / Folder copy button at the default icon-only `p-button` size (removing
  `size="small"`), its glyph at the default button icon size (removing `text-lg`), and
  `check` the same size as `content_copy`.
- Every `<p-button icon="pi pi-*">` in `core-web/libs/edit-content/src` switched to a Material
  Symbol in an `<ng-template #icon>`, keeping `<p-button>`, `ariaLabel` and `data-testid`.
  Buttons that are small today (`size="small"` or the `p-button-sm` class) move to the default
  button size, so the default-size Material Symbol fits them the way it now fits the copy
  button (developer decision 2026-10-13).
- The category field's remove button (`<button pButton icon="pi pi-times">`, not a
  `<p-button>`) gets the same treatment, keeping the `pButton` directive.
- The relationship site chip's `pi pi-folder` switched to a Material Symbol. The icon is drawn
  by the shared `dot-chip-filter` (`libs/ui`), which today only accepts an icon class; it is
  adapted to draw a Material Symbol. The site chip is the only consumer of that input, so no
  other screen changes (developer decision 2026-10-13).
- The date picker trigger icon switched from `pi pi-calendar` / `pi pi-clock` to Material
  Symbols.
- Unit specs updated where they assert `pi pi-*` classes, and new spec assertions for each
  point.

**Explicitly out of scope / non-goals**:

- Replacing `<p-button>` with another button component.
- PrimeIcons that are not button icons: loading spinners (`pi-spin`), status and info glyphs
  (e.g. the banners' `pi-info-circle` / `pi-times-circle`), and field prefixes such as
  `pi pi-globe`.
- Icons PrimeNG draws internally (date picker month arrows and time spinners, drawer, etc.).
- PrimeIcons outside `libs/edit-content`, and any global/theme change that would alter
  buttons in other portlets. The `dot-chip-filter` change above is the one exception outside
  `libs/edit-content`, and it changes nothing for its other consumers.
- The legacy (old) Edit Content screen.
- The Slack-thread request to clear the "form invalid" error banner once the author fixes the
  fields — a behavior change, tracked separately (deferred by the developer).

## Regression Risk *(mandatory)*

- **Blast radius**: `libs/edit-content` templates, plus the `icon` input of the shared
  `dot-chip-filter` (`libs/ui`), whose only user is the relationship site chip. The calendar field is used by every
  Date / Time / Date and Time field in the editor; the icon change touches the command bar,
  sidebar (history, workflow, compare/historical banners), file field, category field,
  relationship field (and its site chip), custom iframe field and the layout's beta banner.
  Risks: an icon-only button losing its accessible name or `data-testid`; a popup clipped by
  the viewport; the drag handle losing its keyboard focus or its `pReorderableRowHandle`.
- **Backward compatibility**: No API, content, serialized-state, DB or index change; nothing
  rollback-unsafe. External tests (e2e) that select buttons by `icon="pi pi-…"` attributes or
  `.pi-*` classes would break — the plan checks `core-web/apps/*-e2e` for such selectors.
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
  no room below) and stays fully inside the viewport.

**3. Relationship drag handle**

- **AC-007**: The drag handle shows a single `drag_indicator` Material Symbol instead of two
  stacked `pi pi-ellipsis-v` glyphs.
- **AC-008**: The handle glyph is the same size as the row's remove (`×`) and the header's add
  (`+`) icons.
- **AC-009**: Hover and focus still reveal the handle, it is still keyboard-reachable,
  drag-to-reorder still works, and a disabled field still shows no handle.

**4. Host / Folder copy button**

- **AC-010**: The copy button uses the default icon-only `p-button` size (no `size="small"`).
- **AC-011**: Its glyph renders at the default button icon size, with no per-instance size
  override such as `text-lg`.
- **AC-012**: The copied state (`check`) is the same size as `content_copy`.
- **AC-013**: Tooltip, accessible name and disabled state behave as today.

**5. Button icons use Material Symbols**

- **AC-014**: Every `<p-button>` in `libs/edit-content` that used `icon="pi pi-*"` shows an
  equivalent Material Symbol through `<ng-template #icon>`; `<p-button>` stays.
- **AC-015**: The date picker trigger (`pi pi-calendar` / `pi pi-clock` from
  `calendar-field.util.ts`) is replaced by an equivalent Material Symbol.
- **AC-016**: Every icon-only button that gets a new icon keeps its `ariaLabel` and
  `data-testid`.
- **AC-017**: Every icon-only button that gets a new icon renders it at the same size as the
  other icon-only buttons in the editor.
- **AC-018**: Searching `core-web/libs/edit-content/src` for `icon="pi pi-` returns no
  matches.
- **AC-021**: Every button that gets a new icon uses the default button size — no
  `size="small"` and no `p-button-sm` class.
- **AC-019**: PrimeIcons that are not button icons (spinners, status/info glyphs, field
  prefixes) are unchanged.

**Regression**

- **AC-020**: The `edit-content` unit tests pass, with assertions on `pi pi-*` classes updated
  to the new Material Symbols (known: `iframe-field.component.spec.ts`,
  `dot-edit-content-calendar-field.component.spec.ts`).

**Verification method**:

- Unit (Vitest + Spectator), written first and confirmed Red, per point: header carries the
  divider classes; the date picker gets the width override; the drag handle renders one
  `drag_indicator` and still has `pReorderableRowHandle` / is absent when disabled; the copy
  button has no `size="small"` and its glyph has no `text-lg`; each migrated button renders
  the expected Material Symbol and keeps `ariaLabel` / `data-testid`; the calendar util
  returns the new trigger icons.
  Run: `pnpm nx test edit-content` (targeted with `--testPathPatterns=` while iterating).
- Static check: `grep -rn 'icon="pi pi-' core-web/libs/edit-content/src` returns nothing
  (AC-018).
- Manual, in the browser against the local instance (widths, viewport placement and pixel
  sizes can't be proven in jsdom): AC-001–AC-006, AC-008, AC-011/AC-012, AC-017, plus
  drag-to-reorder and keyboard focus for AC-009. Compare with the four issue screenshots.

## Assumptions

- Icon mapping (PrimeIcons → Material Symbols), refined in the plan if a closer glyph exists:
  `pi-times` → `close`, `pi-plus` → `add`, `pi-ellipsis-v` → `more_vert`, `pi-history` →
  `history`, `pi-pencil` → `edit`, `pi-replay` → `replay`, `pi-trash` → `delete`,
  `pi-download` → `download`, `pi-info` → `info`, `pi-link` → `link`, `pi-file` →
  `description`, `pi-code` → `code`, `pi-folder` → `folder`, `pi-calendar` →
  `calendar_today`, `pi-clock` → `schedule`.
- Material Symbols Outlined (`material-symbols-outlined`), already loaded globally, is the
  icon family used — it is the only family `libs/edit-content` uses today; no new font or
  dependency.
- Migrated button icons and the drag handle use the default icon size. Making the small
  buttons default-size changes their footprint (relationship table add/remove column,
  sidebar banners, history menus, file field actions and preview, category chip, beta banner
  close); that is accepted as part of this fix.
- "Standard content width" of the date picker means the width PrimeNG's theme gives the
  calendar content when nothing forces a `min-width`.
- The layout's beta-banner close button (`pi pi-times`) counts as a button icon and is in
  scope, even though it sits inside a banner.
