# Feature Specification: Shared Filter Toolbar for Content Drive and the Asset Picker

**Feature Branch**: `nicobytes/37174-improvements-to-the-shared-content-drive-components-folder-tree-filter-chips-row-selection-and-scoped-upload`

**Created**: 2026-09-03

**Type**: New Feature

**Status**: Draft

**Input**: User description: "Unify the asset picker toolbar with the Content Drive toolbar so both share the same UX and filter components. The new asset picker reuses Content Drive components in a modal, but its toolbar is missing the newer Content Drive options: 'Show Shared Assets' chip and the 'More' overflow for additional filters. The picker toolbar should be essentially the same bar as Content Drive's, minus the 'New' button (picker keeps its Upload action instead). Filters must be shared/homogeneous between both surfaces."

**Tracked issue**: [#37174](https://github.com/dotCMS/core/issues/37174) — Finding 6 ("Chips do not reach the picker"). This spec covers Finding 6 only; findings 1–5 and 7–9 of that issue are out of scope here.

## Clarifications

### Session 2026-09-03

- Q: What accessibility level does the spec require for the picker's chip row? → A: Keyboard parity with Content Drive's toolbar, verified in the dialog context — Tab reaches every chip in visual order, Escape closes an open popover without closing the dialog, and focus returns to the chip that opened it.
- Q: What does "Clear all" clear in the picker — does it include the search term, and does the folder scope move? → A: It clears the search term along with the chips, and leaves the folder scope where it is. Exact parity with Content Drive, where clearing empties every filter including the search term and never changes the browsed folder.
- Q: Should the picker offer the Status chip, given it pins archived content out? → A: Yes — the pin is the thing that is wrong, not the chip. `archived` and Status are two ways to say one thing, so the boolean is removed and Status becomes the only way to express content condition. Archived, Unpublished and Locked all become usable in the picker. Workflow stays excluded.
- Q: With the `archived` flag gone, what should `openBrowserModal`'s public `status: 'archived'` mean? → A: "Archived only" — it seeds the Status filter with Archived, rather than today's "working plus archived". No shipped caller passes it, and a value named `archived` returning archived content is the more honest reading.
- Q: Is any other filter contradicted by a pinned request property, now that `archived` is fixed? → A: One — version state. The platform forces the query onto the working version when Archived or Unpublished is selected, which was documented as unreachable from the UI; offering Status in a picker that pins live-only would make it reachable and could hand a field the working version of an unpublished page. Resolved by bounding the Status control's *options* to what the caller's version state admits, the same way the content-type control is already bounded by the allowed base types.
- Q: The field-filter chips cannot move to the shared library — they reach a content-selection dialog that lives in a library which already depends on the shared one, so moving them would make the dependency circular. How should the "More" filters still reach the picker? → A: Split it. The shared control covers every field type on its own; the one field type that needs that dialog (Relationship) gets it injected by whichever surface can supply it. Content Drive supplies it and keeps today's behaviour; the picker supplies none and the control degrades for that field type only — which costs it nothing, because the content types it filters have no relationship fields.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Control shared assets from inside the picker (Priority: P1)

An editor is filling an Image field on a site that inherits a large library of assets shared
across every site. The picker lists both the site's own assets and the shared ones with no way to
tell them apart or to narrow to just this site's, so finding the right local image means scrolling
past dozens of shared ones. In Content Drive the same editor can simply switch **Show Shared
Assets** off. They expect the same control where they are actually choosing the asset.

**Why this priority**: It is the only gap in the current picker that changes *which results come
back*. Every other item in this spec is about layout and future-proofing; this one is an editor
being unable to reach content they can reach one screen over. It is also the smallest independently
shippable slice.

**Independent Test**: Open the picker on a site with shared (SYSTEM_HOST) assets present, toggle
the chip off, and confirm the shared assets leave the list and the chip shows the applied state;
toggle back on and confirm they return. Delivers the missing filter on its own, with no other
toolbar change.

**Acceptance Scenarios**:

1. **Given** the picker is open on a folder that contains both site-local and shared assets,
   **When** the editor opens the toolbar, **Then** a **Show Shared Assets** chip is present and
   shows the filter as on.
2. **Given** the chip is on, **When** the editor switches it off, **Then** shared assets are
   removed from the list, the result count and paging reflect the narrower set, and the chip shows
   the filter as off.
3. **Given** the chip is off, **When** the editor switches it back on, **Then** the shared assets
   reappear and the chip shows the filter as on.
4. **Given** the editor has never touched the chip, **When** the picker opens, **Then** shared
   assets are included — the behavior the picker has today is unchanged as a default.
5. **Given** the editor switches the chip off and then selects an asset and confirms, **When** they
   reopen the picker later, **Then** the chip is back to its default (on) — the picker does not
   remember filter state between openings.

---

### User Story 2 - One filter row, wired in one place (Priority: P1)

Today each surface assembles its own filter row from scratch: a filter added to Content Drive is
invisible in the picker until someone re-implements it there. This is how the picker fell behind on
**Show Shared Assets** and **More** in the first place. A developer adding the next filter should
wire it once and have it offered to every surface that opts in.

**Why this priority**: Without this, User Story 1 is a one-off patch and the same drift happens with
the next filter. It is the requirement that makes the parity durable rather than momentary. It is
independently verifiable without any user-visible change: the same filter row, rendered from shared
pieces.

**Independent Test**: Move the concrete filter chips to a shared, surface-agnostic form and have
Content Drive render its existing row from them. Verifiable entirely through Content Drive: same
chips, same order, same behavior, same deep-link/URL round-trip, same "Clear all" — with no chip
reaching into Content Drive's own state directly.

**Acceptance Scenarios**:

1. **Given** the filter chips have been made shared, **When** an editor uses Content Drive,
   **Then** the filter row is unchanged — same chips, same order, same labels, same behavior.
2. **Given** an editor deep-links into Content Drive with filters in the URL, **When** the page
   loads, **Then** every chip reflects the URL state exactly as it does today, and changing a chip
   still writes back to the URL.
3. **Given** a developer adds a new filter chip, **When** a surface opts into it, **Then** no
   change is needed inside the chip itself for it to work on that surface.
4. **Given** a surface deliberately does not offer a chip, **When** the toolbar renders, **Then**
   the chip is absent by explicit configuration, and the exclusion is recorded rather than being an
   accident of where the code lives.

---

### User Story 3 - The picker's toolbar reads as the same toolbar (Priority: P2)

An editor moves between Content Drive and the picker constantly. Two bars that look and behave
differently for the same job cost them a moment of re-orientation every time. The picker's bar
should be the Content Drive bar: the same search box, the same chip row in the same order, the same
"More" overflow and the same "Clear all" — with the creation actions replaced by the picker's single
**Upload** action, since an editor picking an asset is not creating content types or folders.

**Why this priority**: Real UX value, but it is polish on top of a bar that already works once
Stories 1 and 2 land. Shipping it later still leaves the editor with the filter they were missing.

**Independent Test**: Open the picker and compare its toolbar against Content Drive's side by side:
the chip set matches the opted-in set, in the same order, with the same labels and interactions;
"New" is absent; **Upload** is present.

**Acceptance Scenarios**:

1. **Given** the picker is open, **When** the editor looks at the toolbar, **Then** the chips it
   offers appear in the same order and with the same labels as Content Drive's.
2. **Given** the picker is open, **When** the editor looks at the toolbar, **Then** there is no
   "New" / create-content affordance, and the **Upload** action is present in its place.
3. **Given** the editor has changed at least one filter away from its default, **When** they look
   at the end of the chip row, **Then** a **Clear all** action is offered; using it returns every
   filter — the search term included — to the state the picker opened with, while leaving the
   browsed folder untouched.
4. **Given** the picker was opened by a field that restricts what may be chosen (an Image field, a
   video node), **When** the editor uses the filters, **Then** they cannot widen the selection past
   that restriction — the restriction is not represented as a clearable chip.
5. **Given** the picker's dialog is at its default (non-full-screen) width, **When** the chip row
   does not fit on one line, **Then** it wraps rather than overflowing or clipping, and every chip
   stays reachable.

---

### Edge Cases

- **A filter that cannot be satisfied on a surface**: a chip is opted into but the surface's
  underlying request has no way to express it. This must be caught as a configuration error at build
  or test time, not silently ignored at runtime.
- **The "More" overflow with no eligible fields**: Content Drive enables "More" only when exactly
  one content type is selected, and lists that type's user-searchable fields. In the picker, a field
  restriction can leave a content-type selection with no eligible fields at all — the overflow must
  say so rather than open empty or appear enabled and do nothing.
- **A filter that conflicts with a restriction the picker was opened with**: the restriction wins,
  and the conflict must be visible rather than silently returning nothing. The one former example
  of this — the picker pinning archived content out while offering an Archived control — is
  removed at the source by FR-014b rather than papered over: the pin goes, so the conflict cannot
  arise.
- **A Status selection that matches nothing in the picker**: an empty list is the correct answer,
  not a prevented interaction. The editor asked a question the content did not satisfy; the
  toolbar must not decide on their behalf that the question was invalid.
- **A Status option that cannot coexist with the caller's version state**: it is not offered at all
  (FR-014d). This is deliberately *not* treated like the empty-result case above: an empty list is
  an honest answer to a valid question, whereas a selection that would silently switch which
  version of the content is described is not a question the toolbar should let the editor ask.
- **A request behind a chip fails** (content-type fields, languages): the picker runs in hosts that
  cannot reach the platform's usual error-reporting path, so a chip that reports errors must degrade
  to the picker's own in-dialog notification rather than breaking the dialog. Only the surface
  decides how a failure is announced; the chip only reports that one occurred.
- **Chip state and paging**: changing any filter must return the editor to the first page in both
  surfaces; a stale page cursor must never be applied to a narrower result set.
- **Clear all after a search**: searching moves the browsed scope to the site root on both
  surfaces. Clearing afterwards empties the search box but leaves the editor at the root — the
  location is not restored, because the location was never a filter (FR-009b).
- **Full-screen toggle mid-filter**: switching the picker to full screen must preserve the current
  filter state and re-flow the chip row, not reset it.
- **Escape with a filter panel open in the picker**: the panel closes and the dialog stays open
  (FR-019). Nesting a dismissible panel inside a dismissible dialog is the one interaction the
  picker has that Content Drive does not, so it cannot be assumed to work by inheritance.
- **Content Drive's selection-driven toolbar**: Content Drive swaps its creation actions for
  workflow/bulk actions when rows are selected. The picker is a single-select dialog with a
  confirm/cancel footer and has no such mode — its toolbar must not inherit that swap.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Asset Picker MUST offer a **Show Shared Assets** filter that controls whether
  assets shared across all sites appear in its list. It MUST default to *included*, matching the
  picker's current behavior and Content Drive's default.
- **FR-002**: Switching the Shared Assets filter MUST change the results the picker returns — it is
  a real filter on the search, not a display-only toggle — and the control MUST always reflect the
  state actually applied to the results.
- **FR-003**: The concrete filter controls MUST be usable by more than one surface: each control
  MUST take its current value and report changes through a surface-agnostic contract, and MUST NOT
  read or write any single surface's own state directly.
- **FR-004**: Content Drive's filter row MUST be unchanged for users after the controls are made
  shared — same set, same order, same labels, same interactions, same deep-link/URL round-trip, and
  same "Clear all" behavior.
- **FR-005**: Which controls a surface offers MUST be an explicit, recorded configuration of that
  surface. A control that a surface deliberately omits MUST be stated as omitted, not left implicit.
- **FR-006**: Adding a new filter control in the future MUST require wiring it in one place for it
  to become available to every surface that opts in.
- **FR-007**: The picker's toolbar MUST present the controls it offers in the same order and with
  the same labels as Content Drive presents them.
- **FR-008**: The picker's toolbar MUST NOT offer any create-content affordance ("New", content-type
  selection, new folder). It MUST keep its **Upload** action.
- **FR-009**: The picker's toolbar MUST offer **Clear all** whenever at least one filter differs
  from the state the picker opened with, and MUST NOT offer it otherwise. Clearing MUST return
  filters to the picker's opening state, including any values seeded by the caller.
- **FR-009a**: **Clear all** MUST clear the search term along with the chips, on both surfaces. The
  search term is a filter like any other for this purpose, even though the search box also carries
  its own clear affordance.
- **FR-009b**: **Clear all** MUST NOT move the editor to a different folder. The browsed location
  is not a filter: it survives clearing on both surfaces, so an editor who reached the site root by
  searching stays there rather than being returned to wherever the picker opened. (Searching itself
  does move the scope to the root — that behavior is unchanged and is not what Clear all undoes.)
- **FR-010**: Restrictions the picker's caller imposes (allowed content/base types, media type,
  version state) MUST remain unreachable through the filter controls: they MUST NOT be presented as
  clearable chips, and no filter interaction may widen the result set past them. Content condition
  is explicitly **not** among them — see FR-014b; it was previously enforced as a restriction and
  becomes a filter.
- **FR-011**: The picker MUST NOT persist filter state across openings — each opening starts from
  the caller's configuration plus defaults. Content Drive MUST keep persisting its filter state as
  it does today.
- **FR-012**: The picker and Content Drive MUST keep independent filter state: a filter set in one
  MUST NOT affect the other, including when the picker is opened from Content Drive.
- **FR-013**: Changing any filter on either surface MUST reset the result list to its first page.
- **FR-014**: The picker MUST offer exactly these filter controls, in this order: **Show Shared
  Assets**, **Content Types**, **Status**, **Locale**, and the **More** overflow for additional
  per-field filters — Content Drive's row minus **Workflow**.
- **FR-014a**: **Workflow** MUST be recorded as a deliberate exclusion from the picker, not an
  omission. Rationale, per FR-005: an editor choosing an asset for a field is choosing something to
  reference, not managing where it sits in a review process, and the assets a File or Image field
  can take carry no meaningful scheme distinction. This is a scope judgement, not an impossibility
  — adding it later is one element in the picker's toolbar, once the control no longer depends on
  the platform's shared error-reporting path (see FR-015).
- **FR-014b**: Content condition — **Archived**, **Unpublished**, **Locked** — MUST have exactly one
  representation, the Status control, on both surfaces. The picker MUST NOT separately pin archived
  content out of its request: the pin and the control are two ways to say the same thing, and the
  pin is the one that must go. Selecting Archived in the picker MUST therefore return archived
  content, and a selection that matches nothing MUST show an empty list rather than being
  prevented.
- **FR-014c**: Version state — whether the live or the working version is browsed — is a **separate
  axis** from content condition and remains a caller-controlled request property, not a filter
  control. It MUST NOT be presented as a chip and MUST stay unreachable through the toolbar
  (FR-010).
- **FR-014d**: When the caller has pinned the picker to published content only, the Status control
  MUST offer only the conditions that can coexist with that pin. **Locked** can — published content
  may be locked — while **Archived** and **Unpublished** cannot, because neither has a published
  version. Those two MUST NOT be offered in that context, so that no selection can return content
  described by a version the caller did not ask for. Bounding the *options* of a control, rather
  than omitting the control, is the same treatment the content-type control already gets from the
  caller's allowed base types.
- **FR-014e**: A control whose options are bounded by a caller restriction MUST make the bound
  legible rather than silently showing a shorter list — the editor should be able to tell that
  something is unavailable because of how the picker was opened, not because it does not exist.
- **FR-020**: A shared filter control MUST NOT require capabilities that only some surfaces can
  provide. Where one field type needs a capability a surface cannot supply, the control MUST accept
  it as an optional, surface-provided extension and MUST remain fully usable without it — degrading
  only that field type, and saying so, rather than failing to render.
- **FR-021**: Content Drive's field filters MUST keep their current behavior in full, the
  relationship picker included. Nothing an editor can do there today may become unavailable as a
  result of the control being shared.
- **FR-015**: A filter control whose options are loaded on demand MUST report a load failure through
  the reporting path of the surface it is rendered on, and MUST leave that surface usable. In the
  picker this is its own in-dialog notification — the same channel it already uses when assets or
  folders fail to load — and the dialog MUST stay open and operable, with the affected control
  degrading to an empty option list rather than blocking the rest of the toolbar.
- **FR-016**: The picker's chip row MUST wrap within the dialog's width at every size the dialog
  supports (default and full screen), keeping every control reachable without horizontal scrolling.
- **FR-017**: The picker's toolbar MUST NOT adopt Content Drive's selection-driven action modes
  (workflow actions, bulk action entry points); the picker confirms a single selection through its
  own dialog footer.
- **FR-018**: Every filter control MUST be operable by keyboard on both surfaces, at parity with
  Content Drive's toolbar today: Tab reaches each control in the order it is displayed, and a
  control that opens a panel can be opened, navigated and dismissed without a pointer.
- **FR-019**: In the picker, dismissing an open filter panel MUST NOT dismiss the dialog. Pressing
  Escape while a panel is open closes only that panel and returns focus to the control that opened
  it; a second Escape is what closes the dialog. This is a picker-only concern — Content Drive's
  toolbar has no dialog above it — and it is the interaction most likely to lose an editor's
  in-progress selection.

### Key Entities

- **Filter control (chip)**: One named, user-facing filter with a current value, a set of options
  (fixed or loaded), and a way to report a change. Independent of where it is rendered.
- **Filter set**: The collection of filter values a surface currently has applied, plus which of
  them count as defaults — the latter is what decides whether "Clear all" is offered.
- **Toolbar configuration**: A surface's declaration of which filter controls it offers, in what
  order, and which primary action sits alongside them (create vs. upload).
- **Caller restriction**: A boundary imposed on the picker by whatever opened it (allowed types,
  media type). Never a filter; never clearable.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An editor can narrow the picker to their own site's assets, excluding shared ones, in
  one interaction — the same interaction count as in Content Drive.
- **SC-002**: The filter controls the picker offers and the ones Content Drive offers are the same
  set, in the same order, with the same labels, for every control both surfaces opt into —
  verifiable by direct comparison of the two toolbars.
- **SC-003**: Adding one new filter control and offering it on both surfaces requires touching the
  control in exactly one place, plus one line of configuration per surface that opts in.
- **SC-004**: Every user-facing behavior of Content Drive's filter row is unchanged after the move:
  zero regressions across chip set, order, labels, deep-link restore, URL write-back, and
  "Clear all".
- **SC-005**: The picker remains fully operable in every host it supports today, including hosts
  without the platform's usual navigation and error-reporting services — no host loses the ability
  to open the picker.
- **SC-006**: A picker opened with a caller restriction returns only assets satisfying it, under
  every reachable combination of filter controls.
- **SC-007**: An editor can reach and operate every filter control on either surface using only the
  keyboard, and can dismiss a filter panel in the picker without losing the dialog or their
  in-progress selection.
- **SC-008**: Content condition is expressed in exactly one place across the product: selecting
  Archived returns archived content on both surfaces, and no request property elsewhere contradicts
  that selection.
- **SC-009**: No combination of filter selections reachable in the picker can return content
  described by a version state the caller did not ask for.
- **SC-010**: A shared filter control renders and works on a surface that supplies none of its
  optional extensions.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**:
  - **Content Drive** (the newer portlet surface) — its filter row is the source of the controls
    being shared. Behavior must not change; only where the controls live.
  - **The Asset Picker dialog** — reached from Edit Contentlet's File/Image fields, from Story Block
    media nodes, and from custom-field templates through the browse entry point. Gains the filters.
  - **The legacy custom-field host**: the picker is bundled into the older Dojo-based custom element
    host, which boots without the platform's router and event socket. This is a hard boundary — the
    picker's own state layer already avoids the platform's shared error-reporting service for
    exactly this reason, and two of Content Drive's controls currently depend on it. Any control
    moved into shared code must not reintroduce that dependency.
  - **Shipped VTL templates** that open the picker through the browse entry point continue to work
    unchanged; this spec adds filters and changes none of their inputs.

- **Backward-compatibility expectations**:
  - Content Drive's URL contract for filters (deep links, Back/Forward, shared links) must keep
    working byte-for-byte; existing links must resolve to the same filter state.
  - The picker's public configuration contract (what a caller may pass when opening it) is
    unchanged by this work — no caller has to be updated.
  - The picker's default result set is unchanged: shared assets included, first page, same sort.
  - Content Drive's existing behavior around filters that hide folder rows (type, workflow and
    status selections) is preserved as-is.
  - No deprecations intended.

- **Known related decisions**:
  - #36733 / PR #36848 unified the folder tree across Content Drive, the picker, and the Site/Folder
    field. This is the same consolidation applied to the toolbar, and #37174's own acceptance
    criteria already commit to "concrete chips move to the shared UI lib with a store-agnostic API".
  - #37207 established the picker's browse entry point and its opt-in browse capabilities; those
    capabilities are the caller restrictions FR-010 protects.
  - #37066 added Content Drive's Status filter. The clarification session reversed the initial
    "Content Drive-only" reading: content condition must have one representation, so the chip is
    shared and the picker's `archived` pin is removed instead (FR-014b). Only **Workflow** stays a
    Content Drive-only control (FR-014a).
  - The picker's state layer was built as a headless, URL-free counterpart to Content Drive's
    (#36834). That split — one surface owns the URL, the other does not — is the constraint any
    shared filter contract has to respect, and is why the contract cannot assume URL persistence.
  - The plan phase will formally consult `dotCMS/platform-adrs` for decisions on shared UI library
    boundaries and on what may be pulled into the legacy custom-element bundle.

## Assumptions

- **The picker's Search box stays where it is** — first row, alongside **Upload** — matching Content
  Drive's search-above-chips layout. Only the chip row and the primary action differ.
- **Content Drive's folder-tree dock toggle is not part of the picker's toolbar.** The picker already
  shows its sidebar permanently, in a resizable split; it needs no toggle. (Content Drive's toggle
  itself is Finding 5 of #37174, handled separately.)
- **Shared Assets defaults to on in the picker**, and is written explicitly rather than implied by
  an absent value — the same choice Content Drive made, so "Clear all" and a fresh open land on the
  same visible state.
- **"Clear all" in the picker returns filters to the caller's seeded values**, not to empty. An
  Image field that opened the picker pre-filtered to its own locale and types should still be
  pre-filtered after clearing; clearing to empty would strand the editor in an unfiltered library.
- **Filter state is not shared between the two surfaces, and neither is it remembered by the
  picker.** The picker is transient; carrying filters across openings would surprise an editor
  filling a different field.
- **No new backend work is expected.** Every filter in Content Drive's row is already expressible
  in the search request the picker builds — the picker simply pins one of the flags (shared assets)
  and never offers the rest. This is a frontend consolidation; the plan phase confirms it.
- **The set of controls the picker offers is a product decision.** Settled during specification and
  then revised by the clarification session: Shared Assets, Content Types, **Status**, Locale and
  More — see FR-014, which is normative. Only **Workflow** is excluded (FR-014a); adding it later is
  a configuration change on the picker, not a redesign.
- **Making the controls shared does not require every Content Drive control to move.** Only the
  ones both surfaces offer must become surface-agnostic. Workflow is the one that stays in the
  portlet and is projected into the shared row — a plan-phase call, invisible to users either way.
