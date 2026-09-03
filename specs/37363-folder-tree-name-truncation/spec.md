# Feature Specification: Shared folder tree — single-line folder names with ellipsis truncation and an overflow tooltip

**Feature Branch**: `nicobytes/37363-shared-folder-tree-truncate-long-folder-names-with-an-ellipsis-and-a-tooltip`

**Created**: 2026-09-03

**Status**: Draft

**Type**: Defect (shared UI behavior)

**Related GitHub Issue**: [dotCMS/core#37363](https://github.com/dotCMS/core/issues/37363) (split from [#37174](https://github.com/dotCMS/core/issues/37174) finding 3; parent epic [#36702](https://github.com/dotCMS/core/issues/36702); originating [#36733](https://github.com/dotCMS/core/issues/36733))

**Input**: User description: "Long folder names render differently in each consumer of the shared folder tree: the Site/Folder Field cuts the name off and lost the tooltip it used to show, while Content Drive wraps the name onto multiple lines. Decision taken during refinement: truncate with an ellipsis and expose the full name via a tooltip, in both consumers, owned by the shared component."

---

## Scope Note *(read this first)*

The folder tree is now **one shared component** used by several places in the product: the
Content Drive sidebar, the Site/Folder Field overlay, the Browser Selector sidebar, the Asset
Picker sidebar, and the Roles panel. What a long folder name looks like today depends entirely
on which of those you are looking at, because each one styles the row itself:

| Where | Today's behavior |
|-------|------------------|
| Content Drive sidebar | Name **wraps** onto multiple lines, growing the row |
| Site/Folder Field overlay | Name is **cut off**, no tooltip — the tooltip it used to show was lost |
| Browser Selector sidebar | Truncates **and** shows a tooltip, driven by its own local rules |
| Asset Picker sidebar | Truncates, **no** tooltip |
| Roles panel | No truncation handling at all |

Five consumers, four different answers. This specification makes one answer and moves ownership
of it into the shared tree, so the behavior is inherited rather than re-implemented — and so the
next consumer gets it without writing any styling of its own.

The deliverable is therefore **one behavior plus a relocation of responsibility**, not five local
fixes.

---

## Clarifications

### Session 2026-09-03

- Q: How does the shared tree apply truncation and the tooltip to a label a consumer supplies? → A: The shared tree wraps the projected label content in its own single-line container and derives the tooltip text from what is actually rendered; consumers contribute content only.
- Q: How is the complete name reached without a pointing device? → A: Keyboard focus opens the same tooltip that hover does. No separate accessible-name mechanism is added — the shortening is visual only, so assistive technology already receives the full name.
- Q: When is it decided whether a name is shortened? → A: Lazily, at the moment the pointer or keyboard focus reaches the row. The state has no observable consequence at any other time, so no per-row work happens when the tree renders and width changes need no tracking.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A long folder name never breaks the row (Priority: P1)

An editor browsing a site has folders with long, descriptive names — campaign folders, dated
asset folders, imported directory trees. Wherever the folder tree appears, each folder occupies
exactly one row of consistent height, and a name too long for the row ends in an ellipsis
instead of wrapping or being sliced mid-character with no indication that anything is missing.

**Why this priority**: This is the visible defect. Wrapping rows make the tree hard to scan and
push sibling folders off-screen; a hard cut gives no signal that the name continues. Both make
the tree unreliable to read, which is its only job.

**Independent Test**: Create a folder with a 60+ character name, open the Content Drive sidebar
and the Site/Folder Field overlay, and confirm the row height matches the rows around it and the
name ends in an ellipsis.

**Acceptance Scenarios**:

1. **Given** a folder whose name is longer than the width of the tree row, **When** the editor
   views it in the Content Drive sidebar, **Then** the name renders on a single line, ending in
   an ellipsis, and the row is the same height as rows with short names.
2. **Given** the same folder, **When** the editor opens the Site/Folder Field overlay, **Then**
   the name renders the same way — one line, ellipsis — with no visual difference from Content
   Drive beyond each panel's own width.
3. **Given** a folder whose name fits the available width, **When** it is displayed in any
   consumer, **Then** it renders in full with no ellipsis.

---

### User Story 2 - The full name is one hover away (Priority: P1)

When a name is shortened, the editor can point at it (or move keyboard focus to its row) and see
the complete folder name, so a truncated tree never hides the information needed to pick the
right folder.

**Why this priority**: Truncation without a reveal trades one defect for another — the Site/Folder
Field already lost its tooltip and that loss is part of this report. Truncation is only acceptable
because the full value stays reachable.

**Independent Test**: Hover a truncated folder name in each consumer and confirm the full name
appears; confirm the same is reachable without a mouse.

**Acceptance Scenarios**:

1. **Given** a folder name displayed with an ellipsis, **When** the editor hovers it, **Then**
   the complete folder name appears in a tooltip after a brief delay.
2. **Given** the tooltip is showing, **When** the pointer leaves the row, **Then** the tooltip
   is dismissed and does not obstruct the rows underneath.
3. **Given** the editor is navigating with the keyboard, **When** a shortened folder row receives
   focus, **Then** the same tooltip opens with the complete name, and it closes when focus moves
   to another row.

---

### User Story 3 - No tooltip where there is nothing to reveal (Priority: P2)

Folders whose names fit show no tooltip at all. Pointing at a short folder name does not produce
a floating box that repeats what is already on screen.

**Why this priority**: A tooltip on every row turns browsing into a stream of pop-ups and trains
users to ignore it — which defeats User Story 2 in the cases that matter. It is called out
explicitly in the issue's acceptance criteria.

**Independent Test**: Hover a short folder name in each consumer and confirm nothing appears;
narrow the panel until the same name truncates and confirm the tooltip then appears.

**Acceptance Scenarios**:

1. **Given** a folder name that fits its row, **When** the editor hovers it, **Then** no tooltip
   is shown.
2. **Given** a folder name that fits at the current panel width, **When** the panel becomes
   narrower so the name no longer fits, **Then** the name truncates and the tooltip becomes
   available without the editor reloading or reopening anything.
3. **Given** a short name displayed deep in the tree where indentation leaves little room,
   **When** the indentation is enough to make the name overflow, **Then** it truncates and the
   tooltip is available — the decision follows the room actually available, not the length of
   the name.

---

### User Story 4 - Consumers stay aligned by default (Priority: P2)

A place in the product that shows the folder tree gets single-line truncation and the overflow
tooltip without providing any styling or tooltip configuration of its own. Consumers that
customize what a row *says* (bolding a site row, labelling the root differently, adding a row
action) keep those customizations and still inherit the truncation behavior.

**Why this priority**: Divergence is the actual root cause here, not the truncation rule. If each
consumer keeps owning this, the five-way split reappears the next time a panel is added. Lower
than P1 only because it delivers no new user-visible behavior on its own.

**Independent Test**: Remove every consumer-local truncation and tooltip rule and confirm the
behavior is unchanged in all of them; confirm a consumer that provides no label customization at
all still truncates and shows the tooltip.

**Acceptance Scenarios**:

1. **Given** a consumer that provides no label customization, **When** it renders a long folder
   name, **Then** the name truncates and the tooltip is available.
2. **Given** a consumer that customizes the label's wording or emphasis, **When** it renders a
   long folder name, **Then** its customization is preserved **and** the name truncates with the
   tooltip available.
3. **Given** a consumer that renders a row containing the name plus its own controls, **When**
   the name is long, **Then** the name truncates and the controls stay visible and usable rather
   than being pushed out of the row.

---

### Edge Cases

- **Deep nesting**: indentation grows with depth, so the room left for the name shrinks. A name
  that fits at the root may not fit five levels down; truncation and the tooltip follow the room
  available at that depth.
- **Panel resized / overlay reopened at a different width**: because the decision is made when the
  row is pointed at or focused (FR-005a), it always reflects the current width — a name that stops
  overflowing has no tooltip on the next hover, and one that starts overflowing has one.
- **Site rows**: the root row of a tree can carry a hostname rather than a folder name, and is
  emphasized differently. It follows the same rule — one line, ellipsis, tooltip when shortened.
- **Consumer displays different wording**: a consumer may show a localized label for a row instead
  of the stored folder name (the Asset Picker labels the tree's root that way). The tooltip
  follows the wording on screen, so it stays consistent with the row (FR-013).
- **Rows carrying their own controls**: where a consumer adds elements beside the name (the Roles
  panel adds an icon and a user-count badge), the tooltip's hover area is the whole contained row,
  so pointing at the badge also reveals the shortened name. This is accepted: the tooltip only
  ever appears when something is actually hidden, and it reveals the row's own name.
- **Rows that are not folder names**: the "load more" row and loading/empty states are not folder
  labels and are unaffected.
- **Empty or missing name**: renders as today (nothing), with no tooltip.
- **Name that fits exactly**: no ellipsis and no tooltip — the tooltip appears only when something
  is actually hidden.
- **Names of the same visible length but different content**: two names may be equal in character
  count and differ in whether they overflow. Both are handled correctly because the rule is about
  the space the name occupies, not how many characters it has.
- **Touch input**: hover does not exist. The full name remains available by selecting/expanding the
  folder as today; no new touch-only affordance is introduced by this change.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A folder name in the shared folder tree MUST render on a single line — it MUST NOT
  wrap onto additional lines or change the row's height.
- **FR-002**: When a folder name does not fit the space available in its row, the system MUST
  shorten the displayed name and indicate the omission with a trailing ellipsis.
- **FR-003**: When a folder name is shortened, the system MUST make the complete name available
  on hover, in a tooltip.
- **FR-004**: When a folder name is displayed in full, the system MUST NOT show a tooltip for it.
- **FR-005**: The decision of whether a name is shortened MUST be based on the space actually
  available to it — accounting for nesting depth, panel width, and later width changes — and MUST
  NOT be based on the name's character count.
- **FR-005a**: That decision MUST be made at the moment the pointer or keyboard focus reaches the
  row, so it always reflects the row's current width. The system MUST NOT do per-row work for it
  when the tree renders, scrolls, expands, or pages.
- **FR-006**: The single-line truncation and the overflow tooltip MUST be provided by the shared
  folder tree, so that a consumer inherits both without supplying styling or tooltip
  configuration of its own. The shared tree MUST apply them by containing whatever the consumer
  contributes for that row — a consumer contributes content, never the truncation or the tooltip.
- **FR-007**: Consumers that today implement their own truncation or tooltip rules for folder
  names MUST stop doing so and inherit the shared behavior, leaving exactly one definition of it
  in the product.
- **FR-008**: A consumer that customizes what a folder row displays (wording, emphasis, or
  additional row controls) MUST keep that customization and still receive the truncation and
  tooltip behavior.
- **FR-009**: The tooltip MUST NOT appear instantly on pointer transit — it MUST require a brief
  dwell — and it MUST be dismissed when the pointer leaves the row.
- **FR-010**: When a shortened folder row receives keyboard focus, the system MUST show the same
  tooltip that hover shows, and MUST dismiss it when focus leaves the row. No separate mechanism
  is added for assistive technology: the shortening is visual only, so the complete name is
  already conveyed to it.
- **FR-011**: The tooltip content MUST be the folder name in full — the same value the row
  displays, un-shortened — and MUST NOT be the folder's full path. The tooltip therefore reveals
  exactly what the row was trying to show and nothing else.
- **FR-012**: When a consumer displays wording other than the stored folder name for a row (for
  example a localized label for the tree's root), the tooltip MUST reveal the wording the row
  displays, not the underlying stored value. The tooltip can never disagree with its row.
- **FR-013**: This change MUST NOT alter folder selection, expansion/collapse, paging ("load
  more"), context menus, drag-and-drop targeting, or which folders are listed.

### Key Entities

- **Folder tree row**: one folder (or site) presented in the tree. Carries a display name, a
  nesting depth that determines its indentation, and an optional consumer-supplied presentation.
  The display name is the unit this specification governs.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A folder with a 60-character name occupies exactly one row, of the same height as
  its siblings, in every place the folder tree appears — 5 of 5 consumers, versus 1 of 5 today.
- **SC-002**: 100% of shortened folder names reveal the complete name on hover within one second.
- **SC-003**: 0% of fully-displayed folder names produce a tooltip.
- **SC-004**: Long-folder-name presentation is identical across consumers: an editor comparing the
  same folder in two panels sees no difference in how the name is shortened.
- **SC-005**: A new consumer of the folder tree needs zero styling or tooltip configuration of its
  own to get this behavior, and the number of places in the product that define this behavior drops
  from 3 to 1.
- **SC-006**: No regression in the existing folder-tree behavior of any consumer: selection,
  expansion, paging, context menus and drag-and-drop targeting are unchanged, and the Browser
  Selector, Asset Picker and Roles panels show no visual regression.
- **SC-007**: Automated regression coverage exists for both the shortened case (tooltip present)
  and the fitting case (tooltip absent).
- **SC-008**: Rendering, scrolling, expanding and paging a tree of 500+ rows is as responsive as
  before this change — the behavior adds no measurable per-row cost, because nothing is evaluated
  until a row is pointed at or focused.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The shared folder tree in the new Angular UI, and every place
  that renders it — Content Drive, the Site/Folder Field in the new Edit Contentlet, the Browser
  Selector sidebar, the Asset Picker sidebar, and the Roles panel. This is modern product surface,
  not the older JSP/legacy back-end UI; no `com.dotmarketing.*` code is involved. The old
  Site/Folder field and the legacy Browse portlet are untouched.
- **Backward-compatibility expectations**: Purely presentational. No content, stored data, API
  contract, or admin workflow changes. Two consumer-visible behavior changes are intended and
  expected: consumers stop styling folder-name overflow themselves (FR-007), and the Browser
  Selector's tooltip stops showing the folder's full path and shows the folder name instead
  (FR-011). Everything a consumer does today to customize a row's *content* keeps working
  (FR-008).
- **Known related decisions**: This follows the folder-tree unification landed in
  [#36733](https://github.com/dotCMS/core/issues/36733) / PR
  [#36848](https://github.com/dotCMS/core/pull/36848), whose whole point was one tree instead of
  several — this specification applies that same principle to long-name handling, which the
  unification left with each consumer. QA feedback:
  [#36733 comment](https://github.com/dotCMS/core/issues/36733#issuecomment-5197164875). The plan
  phase will formally consult `dotCMS/platform-adrs`.

## Assumptions

- **The refinement decision is settled**: truncation with an ellipsis plus a tooltip is the agreed
  answer, per the issue. Alternatives (wrapping to a bounded number of lines, widening the panel,
  middle-ellipsis, showing the path instead) are not re-litigated here.
- **No click-to-reveal and no touch-specific affordance** is added; hover and keyboard focus
  (FR-003, FR-010) are the only triggers, matching the tooltips already used in these panels.
- **The tooltip reveals the name, not the path** (FR-011, decided during specification). The
  accepted cost is that the Browser Selector sidebar, the only consumer showing the full path
  today, stops doing so. The reasoning: the tooltip exists to undo the truncation, so showing
  something other than the truncated value makes it a second, inconsistent source of information
  — and a full path in a deep tree is itself long enough to need truncating. Where a folder's
  location is genuinely ambiguous, the tree's own indentation and the field's path summary already
  answer it.
- **The Roles panel and any consumer with a structured row are in scope for not regressing**, and
  inherit the behavior through the shared container that wraps whatever they contribute, but their
  row layout is not redesigned by this work.
- **The folder *search results* list is out of scope.** It is a separate presentation from the tree
  (flat, no indentation, no expansion) with its own rows; if it has the same defect that is a
  separate report.
- **The trigger/summary control of the Site/Folder Field is out of scope** — it already truncates
  its selected path and is not a tree row.
- **Tooltip dwell delay** matches the delay already in use for these panels' tooltips rather than
  introducing a new timing.
- **No new configuration surface**: the behavior is not made opt-in or tunable per consumer. A
  consumer that wants something else replaces the row content entirely, as it can today.
