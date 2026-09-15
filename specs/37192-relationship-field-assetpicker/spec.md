# Feature Specification: Relationship Field — "Add Relationships" picker on the shared search surface, and related-list fixes

**Feature Branch**: `issue-37192-relationship-field-assetpicker`

**Created**: 2026-09-09

**Type**: New Feature (design refactor) + bundled defect fixes

**Status**: Draft — the three scope questions were settled 2026-09-09 (see [Clarifications](#clarifications))

**Related GitHub Issue**: [#37192](https://github.com/dotCMS/core/issues/37192) — reuses the AssetPicker epic [#36702](https://github.com/dotCMS/core/issues/36702); cross-references [#36155](https://github.com/dotCMS/core/issues/36155) (kept separate) and [#36615](https://github.com/dotCMS/core/issues/36615) (form max-width, closed)

**Design**: [Asset Picker canvas](https://claude.ai/design/p/202fc776-9326-4926-b4c6-5996a8be9ea1?via=share&file=Asset+Picker.dc.html) — the "Add Relationships" artboard is the one this spec is written against.

**Product context**: [Slack thread](https://dotcms.slack.com/archives/C0893BF7GRJ/p1787232866639669)

**Input**: User description: "GitHub issue dotCMS/core#37192 — Refactor the Relationship field in the new Edit Content to the new design, reusing the AssetPicker component (#36702) filters/search and the api/v1/drive/search endpoint; plus relationship-component fixes: hover-only drag handle, right-aligned Status column, single-column form max width (#36615), and removal of pagination so drag-reorder works across the full related set."

---

## Scope Note *(read this first)*

The issue bundles **two independent bodies of work** that happen to live in the same field. They are
kept separate throughout, because they ship, test and fail independently:

| | What it covers | Where it lands |
|---|---|---|
| **A. The picker** | The **"Add Relationships"** dialog an editor opens to find and relate content. Today a bespoke table with its own search, its own filter popover behind a sliders icon, and its own paging. | User Stories 1–3 |
| **B. The related-content list** | The table of **already related** content rendered inside the form. Four defects, none of which involve the picker. | User Stories 4–6 |

**The two lists are different lists.** This matters because the issue's wording invites confusion:

- The **picker keeps its paging** — the design shows `Page 1 ‹ › [10 ▾]` at the foot of the dialog.
- The **form's related-content list loses its 6-row paging**, and gains a *Load more* control instead
  (User Story 4). Nothing about the picker changes here.

**Out of scope** — recorded so PR 2 is not reviewed against them:

- [#36155](https://github.com/dotCMS/core/issues/36155) — chip colors, Locales column, hint text,
  empty-state copy, `showFields` handling. A separate issue by an explicit scope decision; this spec
  must not change those behaviors in either direction.
- Any change to `api/v1/drive/search` or to the endpoints that persist relationships. Frontend-only
  work against contracts that already exist — the issue says so, and its Suggested Tests section
  explicitly rules out Integration and Postman work.
- The **legacy (Dojo) edit contentlet** relationship field. This spec covers the relationship field
  in the **new** Edit Content only; per [#37132](https://github.com/dotCMS/core/issues/37132) the new
  picker must not open inside the legacy editor.
- The **"New content"** path (creating related content in place, side panel or dialog per the feature
  flag). Only the "Existing content" path changes.

---

## Clarifications

### Session 2026-09-09

- **Q: How much of the AssetPicker shell does the Relationship picker adopt?** → **A: The shared
  search-and-filter surface, without the folder-tree sidebar.** Settled by the design: the "Add
  Relationships" artboard is a full-width search bar, a chip row (site/folder chip + Locale chip),
  a result table and a paging footer — no sidebar, no splitter. The content of an arbitrary content
  type is not folder-scoped, so the folder scope appears as a **chip**, not as a tree.
- **Q: How is multi-select satisfied, given the AssetPicker returns exactly one asset?** → **A: It
  already exists in the shared pieces; only the AssetPicker's own selection state is single.** The
  shared list view defaults to multiple selection (header select-all checkbox plus per-row
  checkboxes, which is exactly what the design shows), and Content Drive already holds a *set* of
  selected items. So multi-select is reached by configuring the shared list, not by widening the
  AssetPicker's single-slot selection and not by re-implementing selection for the relationship.
- **Q: With the form's 6-row paging removed, what happens to a relationship holding 200 items?** →
  **A: The same *Load more* pattern the Key/Value field uses (#37191).** Render the first 40 rows,
  withhold the rest from the DOM, and append a row carrying a **"Load more"** control that reveals
  the next 40. Withholding is a rendering limit only — the full set stays in the value, so nothing
  is lost by a drag, an edit or a save.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find related content the same way you find anything else (Priority: P1)

An editor filling a Relationship field wants to attach three articles. Today they get a dialog unlike
any other content-finding surface in the product: a different search box, a filter popover hidden
behind a sliders icon offering only locale and site, and a table that follows none of the product's
list conventions. One screen over, in Content Drive and in the Asset Picker, the same editor searches
with a familiar bar and narrows with removable chips. The **Add Relationships** dialog should be that
surface.

**Why this priority**: It is the issue's headline and the reason the work exists. Every other story
either depends on it (2, 3) or is an unrelated defect fix that could ship on its own.

**Independent Test**: Open a contentlet with a Relationship field, choose "Existing content", and
confirm the dialog presents the shared search-and-filter surface — full-width search bar, removable
filter chips, shared result table, paging footer — while listing only content of the relationship's
target content type.

**Acceptance Scenarios**:

1. **Given** a Relationship field pointing at a content type with existing content, **When** the
   editor opens the selection dialog, **Then** it is titled **"Add Relationships"**, carries a
   full-screen toggle and a close control in its header, and lists content using the shared result
   table.
2. **Given** the dialog is open, **When** the editor looks at the list, **Then** every listed item is
   of the relationship's **target content type**, and no filter offered by the dialog can widen the
   list beyond it.
3. **Given** the dialog is open, **When** the editor types in the search bar, **Then** the list
   narrows to matches without a separate confirm step, the way search narrows in Content Drive.
4. **Given** the dialog is open, **When** the editor looks below the search bar, **Then** the applied
   filters are shown as **chips** — the browsed **site/folder** and the **Locale** — each opening its
   own control and each removable where removal is meaningful.
5. **Given** the editor has applied filters, **When** they clear them, **Then** the full result set
   for the target content type returns.
6. **Given** the editor sorts by a sortable column header, **Then** the list re-sorts and the sort is
   reflected in the header.
7. **Given** the list holds more results than one page, **When** the editor reaches the foot of the
   dialog, **Then** a paging control is present with the current page, previous/next, and a **page
   size** selector — the picker keeps its paging.
8. **Given** a search that matches nothing, **Then** an empty state is shown rather than an
   unexplained empty table.
9. **Given** the relationship's target content type has no content at all, **Then** the dialog shows
   an empty state, not an error.

---

### User Story 2 - Relate and unrelate without losing anything that worked before (Priority: P1)

The current picker enforces rules editors depend on: a **single-cardinality** field takes one item and
a **many** field takes several at once; content already claimed by another parent through a
one-parent relationship is listed but **not selectable**, with the reason stated; and the editor can
review what they have picked before confirming. A refactor that drops any of these is a regression,
however good it looks.

**Why this priority**: These are the rules that keep relationship data correct. A picker that lets an
editor claim an already-claimed child, or put two items into a one-to-one field, produces broken
content — a worse outcome than the old dialog.

**Independent Test**: Exercise each rule against the new dialog with no reference to the old one: a
single-cardinality field, a many field, a target set containing an already-claimed item, and a
multi-item selection reviewed before confirming.

**Acceptance Scenarios**:

1. **Given** a Relationship field whose cardinality permits **many** related items, **When** the
   editor looks at the list, **Then** each row carries a checkbox and the header carries a select-all
   checkbox, and the confirm action reads **"Add Relationships"**.
2. **Given** that field, **When** the editor checks several rows and confirms, **Then** all of them
   are added to the field's related-content list.
3. **Given** a Relationship field whose cardinality permits **one** related item, **When** the editor
   selects an item, **Then** they can hold exactly one, and selecting a second replaces the first.
4. **Given** a listed item is already related to a **different** parent through a relationship that
   permits only one parent, **When** the editor tries to select it, **Then** it is not selectable and
   the reason is stated.
5. **Given** the field already holds related content, **When** the editor opens the dialog, **Then**
   those items' rows are already **checked** — the dialog opens on the editor's current
   relationships, not on an empty selection.
6. **Given** an already-related row is checked, **When** the editor **unchecks** it and confirms,
   **Then** that item is removed from the field's related content. The dialog edits the set; it does
   not only add to it.
7. **Given** the editor unchecks every row, **When** they look at the confirm action, **Then** it is
   **available** — it is never disabled — and confirming removes every related item from the field.
8. **Given** the editor has checked items on page 1, **When** they move to page 2, run a new search
   or change a filter, **Then** their earlier picks are still selected, and returning to those rows
   shows them still checked.
9. **Given** the field holds a related item that the picker's current result view does not return —
   it is beyond the first page, or the opening locale/site filters it out — **When** the editor
   confirms without touching it, **Then** it is **still related**. Confirmation reconciles against
   the whole selection, never against the rows on screen.
10. **Given** the editor has selected several items, **When** they review their current selection,
    **Then** the review lists their whole selection — including items not in the current result
    view — and they can deselect from there.
11. **Given** the editor confirms, **When** the dialog closes, **Then** the field's related content
    equals the dialog's selection; items that were already related and stayed checked are neither
    duplicated nor reordered.
12. **Given** the editor cancels, **When** the dialog closes, **Then** the field's related content is
    exactly what it was before the dialog opened.
13. **Given** the field is disabled (read-only for this user or this workflow step), **When** the
    editor looks at it, **Then** no dialog can be opened and no item can be removed.
14. **Given** a single-cardinality field that already holds its one item, **When** the editor looks at
    the field, **Then** the add affordance is unavailable — so exchanging that item is a
    remove-then-add, not an in-picker swap, and the editor cannot reach a state where a selection is
    silently dropped.

---

### User Story 3 - One search path, wired once (Priority: P2)

Today the Relationship picker searches through a different backend path than Content Drive and the
Asset Picker, and re-implements its own locale and site filters. That is how the surfaces drifted
apart, and it is why a filter added to Content Drive never reaches the Relationship field. After the
refactor there should be one search path and one set of filter controls, so the next filter is wired
once.

**Why this priority**: It is what makes Story 1 durable rather than a one-time repaint. It is also
independently verifiable without any visual change: same results, one code path.

**Independent Test**: Verify the dialog's searches leave through the shared Content Drive search
request carrying the target content type, and that the filter controls it renders are the shared ones
rather than relationship-specific copies.

**Acceptance Scenarios**:

1. **Given** the dialog is open, **When** any search, filter, sort or page change happens, **Then**
   the request issued is the shared content-drive search request (`api/v1/drive/search`), carrying
   the relationship's target content type as a constraint.
2. **Given** a filter exists on the shared filter row, **When** the Relationship picker opts into it,
   **Then** it works with no relationship-specific implementation of that filter.
3. **Given** a filter the Relationship picker deliberately does not offer, **When** the dialog
   renders, **Then** it is absent by explicit configuration, and the exclusion is recorded rather
   than being an accident of where the code lives.
4. **Given** the refactor has landed, **When** the codebase is inspected, **Then** the
   relationship-specific search service, filter controls and dialog that the shared ones replace are
   removed, not left behind unused.

---

### User Story 4 - Rearrange the whole related set, not one page of it (Priority: P1)

An editor curating a "Featured articles" relationship with 14 items wants the thirteenth moved to the
top. The form's list pages at six rows, so items 13 and 2 are never on screen together and can never
be dragged past one another. Editors work around it by unrelating and re-relating in order, which
loses their place and risks dropping an item.

**Why this priority**: It is the one item in group B that blocks a real editorial task rather than
looking wrong, and the only one that changes behavior rather than presentation.

**Independent Test**: Open a contentlet whose Relationship field holds more than six related items,
confirm no paging control is present, drag an item from the far end to the top, save, and reload.

**Acceptance Scenarios**:

1. **Given** a Relationship field with any number of related items, **When** the field renders,
   **Then** no paging control is shown — paging is gone, not resized.
2. **Given** a relationship holding **40 or fewer** items, **When** the field renders, **Then** every
   item is listed and no *Load more* control appears.
3. **Given** a relationship holding **more than 40** items, **When** the field renders, **Then** the
   first 40 rows are listed and a **"Load more"** control is appended after the last one.
4. **Given** that control, **When** the editor uses it, **Then** the next 40 rows are revealed; the
   control disappears once every item is on screen.
5. **Given** all rows are on screen, **When** the editor drags the last item to the first position,
   **Then** the list reflects the new order immediately.
6. **Given** rows are still withheld, **When** the editor drags, edits or removes a visible row,
   **Then** the withheld items are still in the field's value and are neither lost nor reordered by
   accident — withholding is a rendering limit, never a change to the value.
7. **Given** rows have been revealed, **When** the editor relates, unrelates or reorders something,
   **Then** the revealed rows stay revealed rather than collapsing back to the first 40.
8. **Given** the editor has reordered items, **When** they save and reopen the contentlet, **Then**
   the related content is in the order they left it.
9. **Given** the editor removes an item from a long list, **When** the row disappears, **Then** the
   editor stays where they were rather than being returned to the top.

---

### User Story 5 - A quieter row (Priority: P3)

The drag handle sits permanently at the left of every related row, so a list of ten items shows ten
sets of grip dots whether or not anyone intends to reorder anything. The remove button already
appears only on hover; the handle should match, so a row reads as content and reveals its controls
when the editor reaches for them.

**Why this priority**: Presentation only. Real polish, but nothing an editor cannot do today.

**Independent Test**: Render a related-content list, confirm the handle is not visible at rest, hover
a row, and confirm the handle appears on that row and can start a drag.

**Acceptance Scenarios**:

1. **Given** a related-content list at rest, **When** the editor looks at a row, **Then** the drag
   handle is not visible.
2. **Given** the editor hovers a row, **Then** that row's drag handle becomes visible and can start a
   drag.
3. **Given** the editor moves the pointer away, **Then** the handle is hidden again.
4. **Given** the editor is navigating by keyboard, **When** focus reaches a row's controls, **Then**
   the reorder affordance is reachable without a pointer.
5. **Given** the field is disabled, **When** the editor hovers a row, **Then** no drag handle is
   offered, because the list cannot be reordered.

---

### User Story 6 - Columns that line up (Priority: P3)

Two layout defects in the form's related-content list: the **Status** column is left-aligned where the
design has it right-aligned, and in a **single-column** form the field does not respect the form's
single-column maximum width, so the relationship table is wider than every field above and below it.

**Why this priority**: Presentation only, and the narrowest of the six stories — but the width defect
shows on every single-column content type, which is most of them.

**Independent Test**: Render the field in a single-column form and in a multi-column form and compare
its width against the form's other fields; compare the Status column's alignment against the design.

**Acceptance Scenarios**:

1. **Given** a related-content list, **When** it renders, **Then** the Status column's contents are
   right-aligned in the header and in the body rows.
2. **Given** a content type whose form is single-column, **When** the Relationship field renders,
   **Then** the field is no wider than the form's single-column maximum and lines up with the fields
   around it.
3. **Given** a content type whose form has multiple columns, **When** the Relationship field renders,
   **Then** its width is unchanged from today.
4. **Given** the field shows extra columns configured through the content type, **When** the total
   column width exceeds the field's width, **Then** the table scrolls horizontally **within** the
   field rather than pushing the form wider.

---

### Edge Cases

- **Empty relationship, disabled field**: the empty state offers a "relate content" link today, and
  suppresses it when the field is disabled. Both must survive the picker swap and the paging removal.
- **A related item is deleted or archived elsewhere** while the contentlet is open: the row still
  refers to it. Behavior is unchanged by this spec, but neither the paging removal nor *Load more*
  may turn one unresolvable row into a rendering failure for the whole list.
- **Locale switching / manual translation**: the field re-initializes its list against a target locale
  and must not mark the form dirty when it does. Removing paging must not disturb that — the
  "programmatic load vs. user edit" distinction is what keeps the unsaved-changes guard from firing on
  a content the editor never touched.
- **Reorder, then unrelate, then reorder**: with no pages to keep honest, the position bookkeeping
  that existed only for paging is dead weight; removing it must not lose the editor's place.
- **A drag that crosses the *Load more* boundary**: a drag must never move, drop or reorder a withheld
  item as a side effect.
- **Unchecking every row**: today the confirm action is disabled with an empty selection, so the
  dialog cannot unrelate *everything* — the editor has to remove the last item from the field's own
  list. **This spec changes that** (FR-012): the action is never disabled, and confirming with an
  empty selection clears the relationship. On a **required** relationship field the result is an
  empty required field, which the form's own validation reports the same way it does when the editor
  removes the last row by hand — the picker does not gain a validation rule of its own.
- **An already-related item that the picker's own search does not return** — it sits beyond the
  first page, or the locale/site the picker opens on filters it out. Under FR-011 it is still in the
  selection and survives confirmation. Worth calling out because the **current** implementation
  computes the pre-selection by filtering the first response
  (`data.filter(item => selectedItemsIds.includes(item.inode))`), so such an item is not pre-checked
  today and confirming drops it. Whether that is a live defect on `main` or masked by the present
  page size is a question for `/speckit-plan`; either way FR-011 is what stops the refactor from
  inheriting it.
- **Picker opened from a contentlet that has never been saved**: the "already related to another
  parent" check compares against the current contentlet's identifier, which does not exist yet. The
  new picker must handle that the way the current one does.
- **A target content type with no site/folder relevance**: the site/folder chip must still resolve to
  something sensible rather than blocking the search.

## Requirements *(mandatory)*

### Functional Requirements — A. The "Add Relationships" picker

- **FR-001**: The Relationship field's "select existing content" experience MUST be a dialog titled
  **"Add Relationships"**, presenting the shared search-and-filter surface rather than a
  relationship-specific dialog built from scratch. Its header MUST carry a full-screen toggle and a
  close control; its footer MUST carry **Cancel** and **Add Relationships**.
- **FR-002**: The dialog MUST present a **full-width search bar** above the results, and the applied
  filters as a **chip row** beneath it — the browsed **site/folder** and the **Locale**. It MUST NOT
  present a folder-tree sidebar.
- **FR-003**: Every search, filter, sort and page change the picker issues MUST go through the shared
  Content Drive search request (`api/v1/drive/search`), not through the relationship-specific search
  path used today.
- **FR-004**: The picker MUST constrain results to the relationship's **target content type**. No
  filter it offers may widen the result set beyond that content type.
- **FR-005**: The picker MUST render the shared filter controls rather than relationship-specific
  copies. The set of filters it offers MUST be declared explicitly, and a filter it does not offer
  MUST be excluded by that declaration rather than by absence of code.
- **FR-006**: The result table MUST offer **multiple** selection for a many-cardinality relationship —
  a per-row checkbox plus a header select-all — reached by configuring the shared list view, not by
  re-implementing selection.
- **FR-007**: The picker MUST honor the field's cardinality: exactly one selectable item for a
  single-cardinality relationship, several for a many relationship.
- **FR-008**: The picker MUST list, but MUST NOT allow selection of, content already related to a
  different parent through a relationship permitting only one parent, and MUST state why.
- **FR-009**: The picker MUST open with the field's **already related** items **pre-selected** —
  their rows come in checked — so the editor sees their current relationships as the starting point
  rather than an empty selection.
- **FR-010**: The picker MUST behave as a **selection editor**, not an add-only dialog: checking a
  row adds that item, and **unchecking an already-related row removes it**.
- **FR-011**: The picker's selection MUST be an **accumulated set**, not a reading of the rows
  currently on screen. It MUST survive a page change, a search and a filter change; the field's
  already-related items MUST enter that set **in full** when the picker opens, including any that
  fall outside the current result view; and the selection review (FR-014) MUST read from the set
  rather than from a page of results. Confirmation reconciles against the whole set.

  This is the requirement that keeps FR-013 safe. Today the picker fetches once and pages in the
  browser, so "the rows on screen" and "everything that matched" are accidentally the same thing.
  Moving to `api/v1/drive/search` (FR-003) ends that accident — pages come from the server — and
  without this requirement an item the editor never scrolled to would be dropped on confirm.
- **FR-012**: Confirming MUST leave the field's related content equal to the picker's selection at
  the moment of confirmation. Items that were already related and stayed checked are neither
  duplicated nor reordered.
- **FR-013**: The confirm action MUST always be available — it MUST NOT be disabled when the
  selection is empty. Confirming with nothing selected removes **every** related item, which is the
  only reading consistent with FR-010: if unchecking a row unrelates it, unchecking the last row
  cannot be the one case that silently does nothing.
- **FR-014**: The editor MUST be able to review their current selection before confirming, and
  deselect from that review.
- **FR-015**: Cancelling MUST leave the field's related content exactly as it was.
- **FR-016**: When the field is disabled, or a single-cardinality field already holds its item, the
  picker MUST NOT be openable. **This bounds FR-007 rather than contradicting it**: the
  "a second selection replaces the first" rule applies *within* an open dialog — while the field is
  still empty, or across picks made before confirming. Exchanging an item a single-cardinality field
  already holds is a **remove-then-add**, not an in-picker swap.
- **FR-017**: The picker MUST keep its own **paging**: current page, previous/next, and a page-size
  selector at the foot of the dialog.
- **FR-018**: Result columns MUST be the shared table's — title with thumbnail, the columns the
  content type configures, Locale, Status and Last Modified — with sortable headers where the shared
  table sorts.
- **FR-019**: The relationship-specific search service, filter controls and dialog replaced by the
  shared ones MUST be removed from the codebase, not left in place unused.
- **FR-020**: The picker MUST NOT be used inside the legacy Dojo edit-contentlet page (#37132).

### Functional Requirements — B. The related-content list in the form

- **FR-021**: The related-content list MUST NOT render a paging control.
- **FR-022**: When the relationship holds more than **40** items, the list MUST render only the first
  40 rows and withhold the rest from the DOM.
- **FR-023**: A row MUST be appended after the last rendered one carrying a control that reveals the
  next 40. It MUST read **"Load more"**, with no count, matching the Key/Value field's precedent
  (#37191).
- **FR-024**: The control MUST disappear once every row is rendered, and MUST NOT appear at all for a
  relationship that fits in the first page.
- **FR-025**: Withholding MUST be a **rendering** limit only. Every related item MUST remain in the
  field's value and in what it emits, so reorder, remove and the saved payload behave as though every
  row were on screen. A drag MUST NOT lose, move or reorder a withheld item.
- **FR-026**: Revealed rows MUST survive a change: relating, unrelating or reordering MUST NOT collapse
  the list back to the first 40. A field opened afresh starts at the first page again.
- **FR-027**: Drag-reordering MUST work between **any two** rendered items, with no page boundary
  between them.
- **FR-028**: A reorder MUST be persisted on save and MUST be the order shown when the contentlet is
  reopened.
- **FR-029**: The drag handle MUST be hidden at rest and revealed when its row is hovered; it MUST
  remain reachable for keyboard users.
- **FR-030**: A disabled field MUST NOT offer a drag handle at all.
- **FR-031**: The **Status** column MUST be right-aligned in the header and in the body rows.
- **FR-032**: In a **single-column** form, the Relationship field MUST NOT exceed the form's
  single-column maximum width, and MUST align with the other fields in that form.
- **FR-033**: In a **multi-column** form, the Relationship field's width MUST be unchanged.
- **FR-034**: When the configured columns exceed the field's width, the table MUST scroll
  horizontally inside the field rather than widening the form.

### Functional Requirements — Cross-cutting

- **FR-035**: Relationship changes made by the editor (relate, unrelate, reorder) MUST mark the form
  as edited, while programmatic loads (initial load, locale re-initialization) MUST NOT. This holds
  today and MUST survive the refactor, because the unsaved-changes guard depends on it.
- **FR-036**: Saving after any combination of relate / unrelate / reorder MUST persist exactly the
  resulting set and order — no additions, no losses.
- **FR-037**: The behaviors listed as out of scope (#36155: chip colors, Locales column, hint text,
  empty-state copy, `showFields`) MUST be unchanged by this work.

### Key Entities

- **Relationship field**: a field on a content type pointing at a **target content type** and carrying
  a **cardinality** (one or many children, one or many parents). Its value is an ordered set of
  related contentlets.
- **Related content item**: one contentlet in that ordered set. Presented with a title, an optional
  thumbnail, a locale, a status, and any extra columns the content type configures.
- **Constrained item**: a contentlet of the target content type already related to a different parent
  through a relationship permitting only one parent. Visible in the picker, unselectable.
- **Picker selection**: the editor's in-progress set inside the dialog. Discarded on cancel, merged
  into the field's related content on confirm.
- **Withheld row**: a related item present in the field's value but deliberately not rendered, waiting
  behind *Load more*. Invisible to the editor, fully present to every operation on the value.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An editor finds and relates content using the same search-and-filter gestures they use
  in Content Drive, with no relationship-specific gesture to learn.
- **SC-002**: An editor can move any related item to any position, in a single drag, without
  unrelating anything — for a relationship of any size, once its rows are revealed.
- **SC-003**: A relationship holding 200 items renders 40 rows on open and stays fully operable:
  revealing the rest costs one control per 40, and nothing in the value is lost along the way.
- **SC-004**: 100% of the relationship rules that hold today — cardinality, already-claimed content,
  disabled fields, ordering, and what gets persisted on save — still hold after the refactor, proven
  by tests that fail if any one of them is dropped.
- **SC-005**: Exactly one search path serves content-finding across Content Drive, the Asset Picker
  and the Relationship field; adding a filter to the shared row requires no relationship-specific work
  to reach the Relationship picker.
- **SC-006**: The Relationship field lines up with every other field in a single-column form — same
  left edge, same right edge.
- **SC-007**: No backend contract changes: no new endpoint, no change to an existing request or
  response shape, and therefore no integration or Postman coverage.

## Assumptions

- **The design canvas is the visual authority.** This spec states observable behavior; where the
  design and this spec disagree on a purely visual detail (spacing, iconography, exact copy), the
  design wins and the spec is amended. Where they disagree on **behavior**, the acceptance scenarios
  win until the disagreement is resolved explicitly.
- **The shared list view already does multiple selection.** It defaults to it, and Content Drive
  already holds a set of selected items — so FR-006 is a configuration, not a new capability. If it
  turns out the shared browse state cannot carry a set without change, that is a finding for
  `/speckit-plan`, not a silent scope expansion.
- **`api/v1/drive/search` can already answer "content of type X across the site"** with the filters
  the picker needs. The issue states the endpoint is reused as-is and lists no backend work. If a
  needed filter proves unreachable, that too is a finding for `/speckit-plan`.
- **40 is the page size for the form's list**, matching the Key/Value field (#37191) so the two
  fields behave the same way for the same reason. The picker's page size is the shared table's, with
  its own selector.
- **Relationship persistence is unchanged.** The field continues to hand the form the same ordered
  identifier value it does today; only the way the editor arrives at that value changes.
- **#36615 is closed and its 720px / 1000px rule already implemented** at the form level. FR-032 is
  about the Relationship field **honoring** that container, not re-deriving the values.
