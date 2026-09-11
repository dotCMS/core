# Issue Resolution Specification: UVE "Edit VTL" menu opens empty after the selected contentlet is re-anchored

**Feature Branch**: `issue-37499-uve-edit-vtl-empty-menu`

**Created**: 2026-09-11

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37499](https://github.com/dotCMS/core/issues/37499)

**Input**: User description: "In the Universal Visual Editor, the Edit VTL (`</>`) button on the contentlet hover toolbar intermittently opens an empty menu; no VTL files are listed, and nothing happens. A customer reported it as *\"Sometimes the edit VTL on the UVE is not responding. Happens from time to time.\"* They were using a `VtlInclude` content type."

## Problem Statement *(mandatory)*

In the Universal Visual Editor (UVE), the **Edit VTL** (`</>`) button on a contentlet's hover
toolbar opens a menu that is supposed to list the VTL files that contentlet renders. After the
user has clicked a contentlet at least once, that menu intermittently opens **empty** — no file
entries, no error, nothing to click. To the user the button simply does not respond.

The button itself is still shown, because the decision to show it and the decision about what
to put in the menu are made from two different contentlets. The user is offered an action that
has already lost the data it needs.

**Severity / Impact**:

- **Severity**: Medium — editing VTL from UVE is blocked; no data is lost or corrupted.
- **Who**: Any user editing VTL from UVE on a page whose iframe layout shifts after a
  contentlet is selected — scroll, resize, device-preview switch, or late image/font load. In
  practice, most pages.
- **How often**: Intermittent by user perception, but deterministic given the trigger: it
  happens on every hover that follows a layout shift after a selection.
- **Workaround exists**: Clicking Quick Edit (bolt) before `</>` repopulates the menu. The
  customer found this on their own; it is not discoverable.
- **Regression**: Introduced 2026-04-14 by #34173 (`5431c635f8`, *Refactor(UVE): Real-Time
  Canvas*), first shipped in `26.04.20-01`, present in every release since. Later touched by
  `dfdeb9a8db` (#35539).

## Clarifications

### Session 2026-09-11

- Q: The `</>` button is hover-only, but `vtlMenuItems()` prefers the selected contentlet. Which contentlet should the menu list files from? → A: The **hovered** contentlet — `vtlMenuItems()` becomes hover-only, matching `hasVtlFiles()`, restoring the pre-#34173 contract.
- Q: The bounds payload also drops `baseType`, `onNumberOfPages` and `dotStyleProperties`. How wide should AC-009's carry-forward be? → A: **`vtlFiles` only.** The other dropped fields are out of scope for this fix and no follow-up is filed.
- Q: Which test layers does this fix get, given Principle V? → A: **Component/store specs only.** No E2E; AC-002, AC-006 and AC-007 stay manual, with the Principle V justification recorded below.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` (reproduced locally); demo starter site; UVE in Edit mode;
any browser. Requires a widget that renders a VTL file via `#dotParse` — the `VtlInclude`
content type in the demo starter does this out of the box.

**Steps to Reproduce**:

To make the timing deterministic, give the included `.vtl` a block that self-resizes on a
timer, so a layout change fires without user input:

```html
<div id="uve-repro-grow" style="height:40px;background:#fce8e6;"></div>
<script>
(function () {
    if (window.__uveVtlReproRunning) { return; }
    window.__uveVtlReproRunning = true;
    var box = document.getElementById('uve-repro-grow');
    var big = false;
    setInterval(function () {
        big = !big;
        box.style.height = big ? '180px' : '40px';
    }, 2000);
})();
</script>
```

1. Add the widget to a container on a page and open that page in UVE, Edit mode.
2. **Click** the widget once so it becomes the selected contentlet.
3. Wait ~4 seconds — the block resizes and the selection is re-anchored to the new bounds.
4. **Hover** the widget. The `</>` Edit VTL button appears.
5. **Click** the `</>` button.

**Without the self-resizing widget** (the customer's original path): put the widget on a page
tall enough to scroll, click it, scroll down and back up, hover, click `</>`.

**Expected Behavior**: The menu lists the widget's VTL file(s); clicking one opens it in the
VTL editor dialog.

**Actual Behavior**: The menu opens empty — an empty popup panel with no items. No console
error, no toast, no network call.

**Reproducibility**:

- **Always**, once the state is reached: a contentlet must have been selected, and a layout
  change must have re-anchored that selection afterwards.
- **Never** before the first click: with nothing selected, the menu populates correctly. This
  is the negative control — reload, hover the widget without ever clicking it, click `</>`,
  and the file is listed.
- Perceived as intermittent only because the triggering layout change is usually incidental
  (a scroll, a late-loading image) rather than deliberate.

## Scope of Investigation *(mandatory)*

- **Affected area**: Universal Visual Editor — contentlet hover toolbar and the editor store's
  selection anchoring. Specifically the `edit-ema` portlet in the `core-web` Nx workspace.
- **Suspected surface**: **Frontend only** (Angular/TypeScript under
  `core-web/libs/portlets/edit-ema/portlet/`). The modern-vs-legacy Java distinction does not
  apply here — no Java is involved. Backend is behaving correctly: `DotParse.java` emits the
  `vtl-file` marker as expected, and the page API returns the VTL file list. **No backend and
  no SDK change is required.**
  - Toolbar component:
    `core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-contentlet-tools/`
  - Store feature:
    `core-web/libs/portlets/edit-ema/portlet/src/lib/store/features/editor/withSelectionAnchor.ts`
- **Related known decisions**: None known that constrain this fix. The plan formally consults
  `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Two independent defects combine. Both are confirmed against current `main`.

**1. The button and the menu read two different contentlets.**

| Concern | Reads | Location |
|---|---|---|
| Is the `</>` button shown? | **Hovered** contentlet | `dot-uve-contentlet-tools.component.ts:213` — `hasVtlFiles()` reads `contentContext()` |
| What files does the menu list? | **Selected** contentlet, whenever one exists | `dot-uve-contentlet-tools.component.ts:331-338` — `vtlMenuItems()` does `this.selected() ? this.selectedContentContext() : this.contentContext()` |

So the button is gated on contentlet A while the menu is filled from contentlet B. Before
#34173, `vtlMenuItems` was hover-only and the two agreed.

**2. The selected contentlet's payload loses `vtlFiles` on every re-anchor.**

- `applyBoundsForSelection()` rebuilds `editorSelected.payload` from the incoming SDK bounds
  payload via `getPageSavePayload()` — `withSelectionAnchor.ts:130-137`.
- That bounds payload structurally cannot carry `vtlFiles`: `getDotCMSContentletsBound()`
  emits only `{container, contentlet:{identifier,title,inode,contentType,canEdit}}` —
  `libs/sdk/uve/src/lib/dom/dom.utils.ts:85-110`. `getPageSavePayload()` spreads it through
  and adds page-level fields, so nothing restores `vtlFiles`.
- The re-anchor is driven by `SET_BOUNDS` from the SDK's `AUTO_BOUNDS` channel, which observes
  *all* layout change inside the iframe on a debounce trailing edge — hence the apparent
  randomness.

**3. The failure is silent rather than loud.** `vtlMenuItems()` is declared
`computed<MenuItem[]>` but its body is `vtlFiles?.map(...)`, which returns `undefined` when
`vtlFiles` is absent. `tsconfig.base.json` sets `"strict": false`, so the compiler accepts the
type mismatch, and PrimeNG's `p-menu` renders an empty popup for an undefined model instead of
failing. The same `vtlMenuItems()` feeds `actionsMenuItems()`, so the collapsed
(small-contentlet) toolbar inherits the identical bug.

Fixing only the store (2) would make the reported symptom disappear, but would leave the
A-hovered/B-selected mismatch (1) in place. Both are in scope.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Make the `</>` button and the VTL menu read the **same** contentlet — the **hovered** one
  (clarified 2026-09-11). `vtlMenuItems()` becomes hover-only, matching `hasVtlFiles()`, so the
  menu can never list a contentlet other than the one whose toolbar was clicked.
- Make `vtlMenuItems()` return `[]` rather than `undefined`, so PrimeNG is never handed an
  undefined model and an empty list becomes an honest empty list.
- Harden `applyBoundsForSelection()` so re-anchoring carries `vtlFiles` forward from the
  previous selection instead of silently dropping data the bounds payload cannot supply.
- Remove the dead `selectedHasVtlFiles` computed (`dot-uve-contentlet-tools.component.ts:233`),
  confirmed unreferenced anywhere in `core-web` outside its own declaration.
- Cover both defects with specs, per Principle V.

**Explicitly out of scope / non-goals**:

- **No SDK change.** `getDotCMSContentletsBound()` keeps its current shape; widening the
  bounds payload to carry `vtlFiles` would push editor-only concerns into the public SDK
  contract.
- **No backend change.** `DotParse.java` and the page API are correct.
- **No redesign of the selection/hover model.** The two-context split in the toolbar is
  pre-existing architecture; this fix corrects one inconsistent consumer of it, not the model.
- **No change to when the `</>` button appears.** It stays hover-only and keeps its existing
  `hasVtlFiles()` gate; this fix moves the *menu's* source, not the button's visibility rule.
  No new selected-toolbar VTL button. If a selected
  toolbar VTL button is wanted, that is separate product work.
- **No change to `SET_BOUNDS` / `AUTO_BOUNDS` debounce behavior.** The re-anchor frequency is
  not the bug; losing data during the re-anchor is.
- **No carry-forward of the other dropped fields.** `getDotCMSContentletsBound()` also omits
  `ContentletPayload.baseType`, `onNumberOfPages` and `dotStyleProperties`, so re-anchoring
  drops those too. Decided 2026-09-11 to leave them alone: the style editor already carries its
  own rollback-based workaround for the `dotStyleProperties` loss, and widening the merge would
  change inputs this spec does not test. `vtlFiles` only.
- **No `strict: true` migration** for the workspace, tempting as the undefined return makes it.
  Tracked separately (#37401).

## Regression Risk *(mandatory)*

- **Blast radius**:
  - `vtlMenuItems()` is consumed by both the full hover toolbar and `actionsMenuItems()` (the
    collapsed toolbar for small contentlets). Both must be verified.
  - `applyBoundsForSelection()` runs on every `SET_BOUNDS` for the selected contentlet. It
    feeds `editorSelected.payload`, which is also read by Quick Edit, add-content, the style
    editor, and the page save path — so the change must be strictly additive (carry `vtlFiles`
    forward) and must not alter any other field or the bounds themselves. The narrowness is
    deliberate: keeping the merge to one field keeps the style editor's existing
    optimistic-save/rollback path untouched.
  - `promoteHoverToSelected()` already writes the hover context into `editorSelected`; the
    store change must not conflict with it.
- **Backward compatibility**: None at risk. No REST contract, no DB schema, no ES mapping, no
  serialized state, no public SDK surface. Not rollback-unsafe under
  [Rollback-Unsafe Change Categories](../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md).
- **Data considerations**: None. The defect is transient client-side state; nothing bad was
  ever persisted, so there is no existing data to repair.

## Acceptance & Verification *(mandatory)*

**Happy path**

- **AC-001**: With a contentlet selected and the selection re-anchored by a `SET_BOUNDS` event,
  clicking `</>` on that contentlet's hover toolbar lists its VTL files. (The reproduction
  above produces the expected behavior.)
- **AC-002**: Clicking a file in the menu still opens that VTL in the editor dialog, unchanged.
- **AC-003**: Hovering a contentlet with no prior selection still lists its VTL files — the
  negative-control path does not regress.

**Sad path / edge cases**

- **AC-004**: `vtlMenuItems()` returns `[]`, never `undefined`, when the contentlet has no VTL
  files.
- **AC-005**: The `</>` button and its menu both read the **hovered** contentlet. Hovering
  contentlet A while contentlet B is selected lists **A's** files, never B's.
- **AC-006**: Scrolling, resizing the canvas, or switching device preview after selecting does
  not change which files the menu lists.
- **AC-007**: A contentlet with multiple `#dotParse` includes lists all of them.
- **AC-008**: The collapsed (small-contentlet) actions menu shows the same VTL submenu contents
  as the full toolbar under all the conditions above.

**Store hardening**

- **AC-009**: `applyBoundsForSelection()` carries **`vtlFiles` and only `vtlFiles`** forward
  from the previous selection when re-anchoring, so `editorSelected.payload` no longer silently
  loses the data the bounds payload cannot supply. Every other field of the payload and the
  bounds are unchanged — including the other fields the bounds payload omits (see non-goals).

**Cleanup**

- **AC-010**: The dead `selectedHasVtlFiles` computed is removed.

**Verification method**:

Decided 2026-09-11: **component/store specs only — no E2E.**

- **Unit / component specs** (the layer where this defect lives; no backend involved):
  - `dot-uve-contentlet-tools.component.spec.ts` — selection re-anchored → hover → menu still
    lists the files (AC-001); hover-with-no-selection still populates (AC-003); empty case
    yields `[]` not `undefined` (AC-004); hovering A while B is selected lists A's files
    (AC-005); collapsed actions menu matches (AC-008).
  - `withSelectionAnchor.spec.ts` — re-anchor preserves `vtlFiles` on the selected payload and
    leaves every other field and the bounds untouched (AC-009).
  - Run: `cd core-web && pnpm nx test portlets-edit-ema-portlet`
- **Manual verification** for AC-002 (dialog opens), AC-006 (scroll / resize / device preview)
  and AC-007 (multiple `#dotParse` includes), using the self-resizing widget from the
  reproduction section.

**Principle V justification for the layers NOT covered** (the constitution requires this to be
stated explicitly rather than left silent):

- **E2E (Playwright) is deliberately not added.** The re-anchor is driven by a debounced
  `AUTO_BOUNDS` trailing edge inside the page iframe; driving it from Playwright means waiting
  on a timer the test does not control, which is the classic shape of a flaky spec. The cost of
  a spec that fails randomly in the merge queue was judged higher than the coverage it buys,
  given that both defects are fully reproducible at the component and store layer.
- **Known consequence, accepted**: the seam between the SDK bounds message and the store is
  exercised only by simulation in `withSelectionAnchor.spec.ts`, never end to end in a real
  iframe. A future regression in `getDotCMSContentletsBound()`'s payload shape would not be
  caught by this suite. `apps/dotcms-ui-e2e/src/tests/edit-page/layout-concurrent-save.spec.ts`
  is the precedent if that trade is ever revisited.
- **Integration / Postman / Karate**: not applicable — no Java, no REST endpoint, no backend
  behavior changes.
- **TDD gates**: these specs are written, approved by the developer, and confirmed failing
  (Red) for the right reason before any implementation code is written.

## Assumptions

- **Settled (clarified 2026-09-11)**: the `</>` menu lists the VTL files of **the hovered
  contentlet** — the contentlet whose toolbar you clicked — since the button is hover-only.
  AC-005 encodes this. The `</>` button's visibility rule is therefore unchanged by this fix;
  only the menu's source moves, back to where it was before #34173.
- `vtlFiles` on a given contentlet does not change during a UVE editing session without a page
  reload, so carrying it forward across a re-anchor cannot serve stale data.
- No consumer depends on `vtlMenuItems()` returning `undefined` as a signal — `actionsMenuItems()`
  guards with `?.length`, which is equally satisfied by `[]`.
- Removing `selectedHasVtlFiles` is safe: it is unreferenced in `core-web` outside its own
  declaration (verified by grep), and it is not part of any public API.
