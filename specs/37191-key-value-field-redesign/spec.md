# Feature Specification: Redesigned Key/Value Field Across All Consumers

**Feature Branch**: `issue-37191-key-value-field-redesign`

**Created**: 2026-08-27

**Status**: Draft

**GitHub Issue**: [dotCMS/core#37191](https://github.com/dotCMS/core/issues/37191)

**Input**: User description: "we will start working in this one https://github.com/dotCMS/core/issues/37191, there are 3 places where we use this key/value field, we are implementing this new one and it has to be used in those 3 different places: Edit content, Content Type field variable dialog, and Apps custom properties. Also we have to use PrimeNG default as the source of truth (reference mockups provided). Remember that each row can be dragged and dropped to change the order, and the icons are hidden until we hover them."

---

## Overview

dotCMS exposes one shared Key/Value editor that lets a user maintain a list of key → value
pairs. The same editor is embedded in three distinct product surfaces. Today each surface
renders it with ad-hoc styling and inconsistent affordances: the value cell is always a live
input even for rows the user is only reading, action icons compete for attention on every row,
the "hidden value" affordance is a toggle switch in its own column rather than attached to the
value it protects, and reordering is offered in only one of the three surfaces.

This feature replaces the editor's presentation and interaction model with a single redesigned
version and rolls that version out to all three consumers.

### Scope decision on ordering (recorded)

Reordering is offered in all three consumers, unconditionally — there is no per-consumer switch.

### Process note

The developer has chosen to carry **spec and implementation in a single PR**, rather than the
repository's default two-PR Spec-Kit flow (spec PR approved first, implementation PR second). The
spec still requires review — it simply gets reviewed alongside the code it describes.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Content editor maintains key/value pairs while editing content (Priority: P1)

A content editor opens a contentlet that has a Key/Value field. They see a compact table with a
KEY and a VALUE column, and a persistent entry row at the top with "Enter key" / "Enter value"
placeholders and an add control. Existing pairs are listed below as calm, readable text — no
input borders, no row of icons shouting for attention. Hovering a row reveals its actions.
Clicking a value turns it into an input for editing. The editor adds a pair, corrects a typo,
drags a row to a new position, deletes an obsolete pair, and saves.

**Why this priority**: The primary surface named in the issue and the reason the redesign
exists (part of the New Edit Contentlet effort). It is frontend-only — no persistence work —
so it is the fastest path to demonstrable value.

**Independent Test**: Open a contentlet whose content type has a Key/Value field, perform add /
edit / reorder / delete, save, reopen — the field round-trips without loss.

**Acceptance Scenarios**:

1. **Given** a contentlet with an empty Key/Value field, **When** the editor types a key and a value
   in the entry row and confirms, **Then** a new pair appears in the list and the field's value
   reflects it.
2. **Given** existing pairs, **When** the editor clicks an existing row's value, **Then** that value
   becomes an editable input focused for typing.
3. **Given** an existing row's value is being edited, **When** the editor confirms or moves focus
   away, **Then** the change is committed and the row returns to its at-rest text presentation.
4. **Given** an existing row's value is being edited, **When** the editor cancels, **Then** the
   original value is restored unchanged.
5. **Given** existing pairs, **When** the editor is not hovering any row, **Then** no per-row action
   icons are visible; **When** they hover a row, **Then** that row's actions become visible and operable.
6. **Given** three or more pairs, **When** the editor drags the third row above the first, **Then** the
   new order is stored and survives a save and reload.
7. **Given** the editor types a key that already exists, **When** they try to confirm, **Then** the entry
   is rejected with a duplicate-key indication and no pair is added.
8. **Given** the editor confirms with an empty key or empty value, **Then** the entry is rejected and
   the offending control is flagged.
9. **Given** a field with no pairs, **When** the editor views it, **Then** an illustrative icon and the
   message "Add your key and value here." are shown in place of the row list, while the entry row
   above remains present and usable.

---

### User Story 2 - Content-type admin maintains Field Variables (Priority: P2)

An administrator editing a field in the Content Type portlet opens the field dialog and switches
to the **Field Variables** tab. The same redesigned editor appears inside the dialog. They add a
variable, edit an existing one's value, and remove one. Each change persists against the field
immediately, and the list reflects the server's response.

**Why this priority**: Second consumer named in the issue, and the surface where the editor lives
in a constrained dialog width — it validates the redesign at its narrowest. Its per-row server
persistence must survive the refactor untouched.

**Independent Test**: Open any content-type field's Field Variables tab, add / edit / delete a
variable, close and reopen — the variables persist and render in the new design.

**Acceptance Scenarios**:

1. **Given** a field with existing variables, **When** the admin opens the tab, **Then** the variables
   render in the redesigned editor, excluding the reserved keys owned by dedicated settings
   sections.
2. **Given** the tab is open, **When** the admin adds a variable, **Then** it is saved against the field
   and appears in the list.
3. **Given** an existing variable, **When** the admin edits its value, **Then** the change is saved and
   the list shows the updated value.
4. **Given** an existing variable, **When** the admin removes it, **Then** it is deleted and disappears
   from the list.
5. **Given** a save or delete fails on the server, **When** the response returns, **Then** the failure is
   surfaced through standard error handling and the list is not left in a misleading state.

---

### User Story 3 - Administrator maintains Apps custom properties, including secret values (Priority: P2)

An administrator navigates to **Settings → Apps** and opens an app configuration (for example
SSO — SAML). Below the app's declared fields, the **Custom Properties** section presents the same
redesigned editor, with one addition: the value control carries a visibility (eye) affordance.
Marking a value hidden masks it in the list — rendered as dots rather than plain text — and
supporting text explains the effect. The administrator adds a hidden property and a plain one,
and can see at a glance which are hidden.

**Why this priority**: Third consumer named in the issue, and the only one exercising hidden
values. Equal in importance to Story 2; sequenced after it only because the hidden-value
affordance is the largest single behavioral delta in the redesign.

**Independent Test**: Open an app configuration with custom properties enabled, add a hidden
value and a plain value, reload — the hidden one renders masked, the plain one as text, both
persist.

**Acceptance Scenarios**:

1. **Given** the Custom Properties editor, **When** the administrator marks a value hidden while adding
   a pair, **Then** the pair is stored with its hidden flag and its value renders masked.
2. **Given** a pair whose value is hidden, **When** the administrator views the list, **Then** the value is
   masked and the hidden indicator on that row is **permanently visible** — it does not require hover,
   because it communicates state rather than offering an action.
3. **Given** a pair whose value is not hidden, **Then** the value is shown in plain text.
4. **Given** the Custom Properties editor, **Then** supporting text explains the effect of hiding a value.
5. **Given** a consumer that does not enable hidden values (Edit Content, Field Variables), **Then** no
   visibility affordance is rendered anywhere in the editor.
6. **Given** an app whose descriptor does not allow extra parameters, **Then** the Custom Properties
   section is not shown at all.

---

### Edge Cases

**Editing and validation**

- **Duplicate key on entry**: rejected with a visible duplicate-key indication; the existing pair is
  untouched.
- **Empty key or empty value**: entry rejected, offending control flagged. Blank values are not
  silently accepted — this matches current behavior and is preserved.
- **Edit abandoned mid-flight**: cancelling an in-progress value edit restores the original value.
- **Null value already stored**: pairs whose stored value is null continue to load and render rather
  than dropping out of the list.
- **Very long key or value**: the row does not break the layout; content wraps, truncates, or scrolls
  within its own container rather than widening the table beyond its host.
- **Narrow host container** (the field dialog): the editor stays usable at dialog width — entry row,
  both columns, and actions remain reachable with no horizontal page scroll.
- **Zero pairs**: empty-state message replaces the row list; the entry row remains available.

**Accessibility and input modality**

- **Keyboard-only user**: every affordance revealed on hover — row actions, drag handle, visibility
  toggle, click-to-edit — is also reachable and operable via keyboard. Hover-only exposure would
  otherwise strand keyboard and touch users entirely.
- **Touch device**: hover-revealed actions remain reachable where there is no hover.
- **Screen reader**: reordering a row announces the change; masked values are not read aloud in clear.

**Ordering and persistence**

- **Reorder cancelled**: a drag dropped outside a valid target, or escaped, leaves the order unchanged.
- **Server rejects a per-row save** (Field Variables): the row does not silently appear saved.
- **Reserved/blacklisted keys** (Field Variables): remain filtered out of the displayed list.

---

## Requirements *(mandatory)*

### Functional Requirements

**Single shared editor**

- **FR-001**: The system MUST provide exactly one Key/Value editor, and all three consumers (Edit
  Content field, Field Variables, Apps custom properties) MUST render that same editor.
- **FR-002**: The editor MUST look and behave identically across the three consumers, except for the
  capabilities each consumer explicitly enables (hidden values).

**Core editing loop**

- **FR-003**: The editor MUST present a persistent entry row containing a key input, a value input,
  and an add control, positioned above the list of existing pairs.
- **FR-004**: Users MUST be able to add a pair from the entry row, either by activating the add control
  or by confirming from the keyboard.
- **FR-005**: Existing rows MUST render their key and value as plain text at rest, without input
  chrome.
- **FR-006**: Users MUST be able to edit an existing pair's value by activating it, which converts that
  value into a focused, editable input.
- **FR-007**: An in-progress value edit MUST be committable (by confirming or moving focus away) and
  cancellable (restoring the original value).
- **FR-008**: A pair's key MUST NOT be editable after creation; changing a key is accomplished by
  removing the pair and adding a new one.
- **FR-009**: Users MUST be able to remove an existing pair.
- **FR-010**: The editor MUST reject a new pair whose key duplicates an existing key, and MUST indicate
  why.
- **FR-011**: The editor MUST reject a new pair with an empty key or an empty value, and MUST indicate
  which control is at fault.
- **FR-012**: After a successful add, the entry row MUST clear and return focus to the key input so
  consecutive pairs can be entered without reaching for the pointer.
- **FR-013**: The editor MUST emit the complete, current list of pairs to its host on every change
  (add, edit, remove, reorder), and MUST additionally signal the specific add / edit / remove event so
  consumers that persist per row can continue to do so.
- **FR-014**: When the list is empty, the editor MUST display an empty state in place of the rows,
  consisting of an illustrative icon above the message "Add your key and value here." The entry row
  MUST remain present and usable above the empty state, so the empty state never blocks the only path
  out of it. The empty state MUST render inside the same bordered container as the rows it replaces.

**Presentation**

- **FR-015**: The editor's visual presentation MUST derive from the platform's standard
  component-library defaults (table, inputs, buttons) without bespoke per-consumer styling overrides.
- **FR-016**: The editor MUST be assembled from existing shared and component-library building blocks.
  A new bespoke component is permitted only where no existing option fits, and MUST be justified.
- **FR-017**: Per-row **action** affordances — the drag handle and the remove control — MUST be visually
  suppressed at rest and revealed when the row is hovered or focused.
- **FR-018**: The hidden-value indicator is a **state** affordance, not an action, and MUST therefore be
  permanently visible on any row whose value is hidden, independent of hover or focus.
- **FR-019**: Every hover-revealed affordance MUST also be reachable and operable via keyboard, and
  MUST expose an accessible name.
- **FR-020**: The editor MUST render correctly and remain fully operable inside a constrained dialog
  width without introducing horizontal scrolling of the host page.

**Hidden values**

- **FR-021**: In consumers that enable hidden values, the visibility affordance MUST be attached to the
  value control rather than occupying its own column.
- **FR-022**: A pair marked hidden MUST have its value rendered masked in the list.
- **FR-023**: A row whose value is hidden MUST carry a permanently visible hidden indicator
  distinguishing it from plain rows (see FR-018).
- **FR-024**: Hidden values are enabled for the **Apps custom properties** consumer only. The Edit
  Content field and the Field Variables editor MUST render no visibility affordance and no
  hidden-value column.
- **FR-025**: The hidden flag MUST be carried in the emitted pair so the consumer persists it exactly
  as it does today.

**Ordering**

- **FR-026**: The editor MUST support reordering rows by dragging a row handle and dropping it at a new
  position, as a capability of the shared editor available to **all three** consumers.
- **FR-027**: A completed reorder MUST emit the reordered list to the host.
- **FR-028**: A cancelled or invalid drop MUST leave the order unchanged.
- **FR-029**: The drag handle MUST be rendered in **all three** consumers, unconditionally. There is
  no per-consumer switch for reordering — the shared editor always offers it.
- **FR-030**: Newly added pairs MUST take a deterministic, documented position within an existing order.
- **FR-031**: Removing a pair MUST leave the relative order of the remaining pairs intact.
- **FR-032**: If persisting a reorder fails, the system MUST surface the failure rather than leaving
  the displayed order silently diverged from the stored order.

**Consumer integration & data safety**

- **FR-033**: The Edit Content Key/Value field MUST continue to read its stored pairs and write them
  back in its existing shape, with no data loss on round-trip.
- **FR-034**: The Field Variables editor MUST continue to save, update, and delete each variable against
  the field through its existing path, and MUST continue to filter reserved keys out of the displayed
  list.
- **FR-035**: The Apps custom properties editor MUST continue to read and write its pairs, including the
  hidden flag, through its existing path, and MUST remain hidden for apps whose descriptor does not
  allow extra parameters.
- **FR-036**: Existing stored key/value data in every consumer MUST continue to load and save without
  loss or silent transformation after the change.

### Design Constraints (given)

Stated by the requesting developer as binding constraints on *how* the redesign is built. Listed
separately from the Functional Requirements because they name specific tooling rather than
user-observable behavior. All three align with existing repository standards.

- **DC-001 — Component library defaults are the source of truth**: presentation comes from the
  component library's own defaults (PrimeNG 21.x, the version this workspace is on), not from
  hand-written CSS reproducing a mockup. Where a mockup and the library default disagree on
  incidental styling, the library default wins; where they disagree on structure or affordance
  placement, the mockup wins.
- **DC-002 — No unnecessary custom components**: reuse before creating, in order — existing dotCMS
  shared components, then the component library, and only then something new, with a stated
  justification. Reproducing something that already exists is a defect, not a feature. This mirrors
  the repository's standing "Reuse Before Creating" rule.
- **DC-003 — Material Symbols for icons**: all icons in authored markup use Material Symbols. This is
  already the repository standard; PrimeIcons (`pi pi-*`) is legacy-only and the deprecated `dot-icon`
  component must not be used. Because this work rewrites the editor's markup wholesale, every icon it
  renders is newly authored and therefore in scope for this rule. Icons drawn internally by
  component-library components are a theming concern and out of scope.

### Key Entities

- **Key/Value Pair**: the unit the editor manages. Carries a **key** (unique within one editor instance,
  non-empty, immutable after creation), a **value** (non-empty text), an optional **hidden** flag
  indicating the value should be masked in the UI, and a **position** within its list.
- **Editor Capabilities**: the per-consumer switch determining whether hidden values are offered.
  Reordering is no longer a per-consumer capability — it is universal.
- **Ordered Collection**: the per-consumer store of pair positions. Each of the three consumers persists
  this through its own mechanism; the editor itself is agnostic to how.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All three consumers render the redesigned editor; a reviewer comparing them side by side
  finds no visual or interaction differences other than the hidden-value affordance.
- **SC-002**: 100% of existing key/value data in each of the three consumers loads and saves without
  loss or alteration — verified by round-tripping a populated example in each surface.
- **SC-003**: A user can add a key/value pair from an empty field in under 10 seconds using only the
  keyboard, without reaching for the pointer.
- **SC-004**: Zero regressions in existing behavior: duplicate-key rejection, empty-input rejection,
  per-row persistence in Field Variables, and hidden-value masking in Apps all behave as before.
- **SC-005**: Every action available on hover is reachable by keyboard alone, verified by completing
  add / edit / remove / reorder in each surface with the pointer unused.
- **SC-006**: The editor is fully operable inside the field dialog at its standard width with no
  horizontal scrolling of the host page.
- **SC-007**: Automated unit coverage exercises add, edit, cancel-edit, remove, reorder, duplicate-key
  rejection, empty-input rejection, hidden-value rendering, and empty-state rendering; one
  end-to-end smoke per consumer confirms values persist across save and reload.

---

## Assumptions

- **Visual source of truth**: see [Design Constraints](#design-constraints-given) — DC-001 through
  DC-003 capture the component-library, reuse, and icon constraints the requester set.
- **Empty state copy**: the empty state reads "Add your key and value here." above a key-shaped icon,
  per the mockup. The exact glyph is a design detail to be matched against the linked design document.
  The same empty state is used in all three consumers.
- **Third consumer identity confirmed**: the mockup titled "Page / Container — Custom Properties" refers
  to the **Apps** configuration screen's Custom Properties section, reached via Settings → Apps → an app
  such as SSO — SAML. Confirmed by the requester. There is no separate Page/Container key-value surface
  in scope.
- **"Column" means "row"**: the request said each *column* can be dragged to change order. The editor
  has two fixed columns (KEY, VALUE); the orderable axis is the list of pairs, so this is read as *row*
  reordering. Column reordering is out of scope.
- **Ordering already works in Edit Content**: the platform preserves insertion order for the Edit
  Content Key/Value field end to end, storing the value as an ordered array specifically to defeat the
  alphabetical re-sorting applied elsewhere. Story 1 therefore requires no persistence work; Stories 4
  and 5 exist to bring the other two consumers up to that same guarantee.
- **Hidden-value semantics are preserved**: the eye affordance in the mockup carries the same persisted
  hidden flag the current toggle switch carries — it relocates and restyles the control rather than
  redefining it. Masking is a UI concern, not a security boundary; the underlying value is transmitted
  as it is today.
- **Reserved-key filtering stays with the consumer**: the Field Variables blacklist of keys owned by
  dedicated settings sections remains that consumer's responsibility, not the shared editor's.
- **No API contract removals**: ordering is introduced additively. No existing endpoint, field, or
  payload is removed or repurposed.
- **Edit Content mockup not received**: of the reference images provided, those received cover the
  Field Variables dialog (populated and empty) and the Apps custom properties panel. The Edit Content
  mockup did not come through. Its layout is assumed to be the same editor inside the Edit Content
  field card, with hidden values disabled. This should be confirmed against the linked design before
  implementation.
- **The linked design document** (`claude.ai/design/...Key+Value+Field.dc.html`) is the definitive visual
  reference and takes precedence over the static screenshots where they diverge.

---

## Dependencies

- The redesigned editor is a shared component; all three consumers must be updated and verified
  together to avoid a period where surfaces disagree visually.
- Field Variables depends on the existing field-variable save/delete path, unchanged by this work.
- Apps custom properties depends on the existing app configuration save path, unchanged by this work.
- No backend change is required for this feature.
