# UI Contract: folder-tree node labels

**Feature**: [../spec.md](../spec.md) | **Plan**: [../plan.md](../plan.md) | **Date**: 2026-09-03

This feature exposes no REST endpoint. The contract it changes is the one between the shared
folder tree and its five consumers: **who owns the presentation of a node label**. This file is
the reference a reviewer or a future consumer needs; it is written to survive the PR.

---

## 1. `<dot-truncated-label>` (new, exported from `@dotcms/ui`)

A single-line label that clips with an ellipsis and reveals its full text on hover or focus —
**only when the text is actually clipped**.

| Aspect | Contract |
|--------|----------|
| Selector | `dot-truncated-label` |
| Content | `<ng-content>` — the caller provides the text (and any inline markup around it) |
| Inputs | **None.** The behavior is not configurable per call site; that is the point (FR-006) |
| Tooltip text | Derived from the component's own rendered text content at interaction time — never from a caller-supplied string, so it can never disagree with what is displayed (FR-012) |
| Tooltip trigger | Pointer dwell (800 ms, the delay already used by these panels) and keyboard focus of the containing row (FR-003, FR-010) |
| Tooltip suppression | No tooltip when the text fits (FR-004). The gate is a real measurement (`offsetWidth < scrollWidth`) taken at interaction time, not a character count (FR-005, FR-005a) |
| Tooltip position | `right` |
| Escaping | PrimeNG default `escape: true`. **Callers must not turn this off** — a folder name is user-supplied content |
| Layout requirement on the caller | The element must be allowed to shrink: give it (or its flex parent) `min-w-0`. Inside the shared folder tree this is already guaranteed |
| Test id | `data-testid="tree-node-label-clip"` on the clipping element. **Not** `tree-node-label` — Playwright helpers count that one (see [research.md](../research.md) R7) |
| Focus hook | The clipping element also carries `data-dot-truncated-label`. The folder tree uses it to find the label of a focused row; it is a runtime hook, not a test id, so renaming the test id cannot silently break keyboard support |

### How to use it

```html
<!-- Plain label -->
<dot-truncated-label>{{ node.label | dotFolderName }}</dot-truncated-label>

<!-- Inside a row that has its own controls: wrap only the part that should clip -->
<span class="flex w-full items-center gap-2">
    <span class="material-symbols-outlined">folder</span>
    <dot-truncated-label class="min-w-0 flex-1">{{ node.label }}</dot-truncated-label>
    <span class="shrink-0">…badge…</span>
</span>
```

The second form is why this is a component and not markup inside the tree: the tooltip must sit on
whichever element actually clips, and only the caller knows that when the row holds more than a
name.

---

## 2. `<dot-folder-tree>` — what changes for consumers

The component's public inputs and outputs are **unchanged**. What changes is the division of
responsibility for the node label.

### The tree now owns

- The single-line clipping of **its own** node label, and the overflow tooltip on it.
- Forwarding row keyboard focus to whichever `<dot-truncated-label>` the row contains — including
  one a consumer placed itself. Gated on the focus target being the `treeitem` itself: a focusable
  control *inside* a row (the Roles panel's add-child button) must not reveal that row's name.
- The label slot's layout, in the component's own SCSS: `min-width: 0` on
  `.p-tree-node-content` and `.p-tree-node-label` (without which `truncate` has nothing to
  truncate into), plus `flex: 1 1 auto` on the label so it takes the width the toggler and icon
  leave — which is what lets a consumer push a trailing element to the end of the row.

### A consumer that projects `#folderTreeNodeLabel` must

Place `<dot-truncated-label>` around the part of its row that should clip, and nothing else:

```html
<ng-template #folderTreeNodeLabel let-node>
    <dot-truncated-label>
        <span data-testid="tree-node-label" class="font-normal">{{ node.label | dotFolderName }}</span>
    </dot-truncated-label>
</ng-template>
```

The tree deliberately does **not** wrap a projected template for you. It cannot: a row that
carries more than a name — the Roles panel's `icon + name + user-count badge` — has a flex child
that shrinks, so an outer wrapper never overflows, clips the badge rather than the name, and
would read the whole row out in the tooltip. Only the consumer knows which part is the name.
This was found by the Roles test in the Red phase and amended in spec.md's Clarifications.

### A consumer must no longer

| No longer | Why |
|-----------|-----|
| Set `overflow`, `text-overflow`, `white-space` or `truncate` on the node label | Owned by `dot-truncated-label` (FR-006/FR-007). A local rule now either duplicates it or fights it |
| Pass `nodeLabel` / `nodeContent` layout through `pt` — `min-width`, `overflow`, or `flex-1` | Same — and `pt` is the one channel a consumer and the tree would have to share, which is why this moved to SCSS. `pt` also only reaches the top-level tree, so nested rows never received these rules |
| Declare `[pTooltip]` / `[tooltipDisabled]` / `[showDelay]` / `pTooltipPT` on a node label | Owned by `dot-truncated-label`. In particular the `label.length <= 10` heuristic is gone: it is wrong under indentation and at narrow panel widths |
| Reach into tree internals with `::ng-deep` for label overflow | Owned by the tree |

### A consumer still owns

- The node data, loading and empty-state UX, drag-and-drop, overlay chrome — unchanged.
- **What a row says**: wording (including substituting a localized label), emphasis, `data-*`
  attributes, extra row controls. All of it keeps working, and inherits the truncation and the
  tooltip.
- Its own `data-testid` on its own elements. Existing ids keep matching exactly one element per
  row.

### Behavior changes a consumer will see

| Consumer | Change |
|----------|--------|
| Content Drive sidebar | Long names stop wrapping; rows keep a constant height. Tooltip added |
| Site/Folder Field overlay | Tooltip restored (the regression in the report) |
| Browser Selector sidebar | Tooltip now reveals the **folder name**, not the full path (FR-011, decided during specification). Its local rules are deleted; visible behavior is otherwise identical |
| Asset Picker sidebar | Tooltip added. The localized root row's tooltip matches the row's own wording, not the underlying hostname |
| Roles panel | Adopts `<dot-truncated-label>` on its name element; icon and user-count badge unaffected |

---

## 3. Invariants a reviewer can check

1. `data-testid="tree-node-label"` matches **exactly one element per row** in every consumer — the
   Playwright helpers in `apps/dotcms-ui-e2e/src/pages/contentDrive.page.ts` and
   `.../content-drive/helpers/content-drive-tree.ts` count and index it.
2. `grep -rn "pTooltip" ` over the five consumers' node-label templates returns nothing.
3. `grep -rn "truncate\|text-ellipsis\|whitespace-nowrap"` over the five consumers' node-label
   templates and `pt` objects returns only `<dot-truncated-label>` element tags — no CSS class and
   no `pt` key doing the clipping locally.
4. No consumer SCSS contains `::ng-deep` rules for `.p-tree-node-label` overflow.
5. Content Drive's drop highlight (`.p-tree-node-content:has(span.active)`) still matches — the
   wrapper is an ancestor of the `active` span, and `:has()` matches descendants. Covered by a
   test rather than left to inspection.
6. `escape` is never set to `false` on the label tooltip.
