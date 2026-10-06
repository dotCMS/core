# UI Contract: `DotFolderTreeComponent` folder icons

**Feature**: [../spec.md](../spec.md) | **Plan**: [../plan.md](../plan.md) | **Date**: 2026-09-03

`core-web/libs/ui/src/lib/components/dot-folder-tree` — exported from `@dotcms/ui` as
`DotFolderTreeComponent`, selector `dot-folder-tree`. This is the only interface this feature adds
or changes. Everything already on the component stays as it is.

## New input

```
showFolderIcons  (boolean, default false)
```

Signal input in the component's house style — `input(false, { alias: 'showFolderIcons' })` on a
`$showFolderIcons` property, JSDoc'd like every sibling input.

**Semantics**: when `true`, the tree draws a state-reflecting folder icon at the start of each
folder row. When `false` (the default), the tree renders exactly as it does today.

**Why it defaults to off**: `dot-roles-tree` renders a non-folder hierarchy through this component
and draws its own icons. Off-by-default keeps it untouched, and matches the existing
`showLoadMorePlusIcon` convention on this component ("Off by default — consumers opt in").

Naming note for the implementer: if `/speckit-tasks` or review prefers a mode string
(`folderIcons: 'none' | 'state'`) over a boolean, that is an acceptable substitution — the issue's
acceptance criterion says "e.g. a toggler/icon mode input" — but there is no second mode to express
today, so the boolean is what this contract specifies. The `first-only` mode is explicitly not
coming back (spec Assumptions).

## Rendering contract

For a row rendered through the `default` node template, with `showFolderIcons` true:

| Row state | What the tree draws |
|-----------|---------------------|
| Declares `icon`, `expandedIcon` or `collapsedIcon` | **Nothing** — PrimeNG already drew that row's own icon; drawing again would double it |
| `expanded` truthy, no own icon | Open-folder icon (`pi pi-folder-open`) |
| `expanded` falsy, no own icon | Closed-folder icon (`pi pi-folder`) |
| `type === LOAD_MORE_NODE_TYPE` | **Nothing** — PrimeNG dispatches these to the `load-more` template, which this contract does not touch |

With `showFolderIcons` false, the tree draws nothing in all four cases.

Invariants the implementation must hold:

1. **The icon is positioned ahead of the label**, in both the default label branch and the projected
   `#folderTreeNodeLabel` branch, so a consumer's own label template does not have to know about it.
2. **The icon does not replace the expand/collapse affordance.** The `togglericon` template stays
   chevron-only. A leaf row therefore shows a folder icon and no toggler.
3. **The icon depends on `node.expanded` alone** — never on `node.children.length`. This is a
   deliberate divergence from PrimeNG's own `getIcon()`, which would draw a *closed* icon on a row
   that is open with children still loading.
4. **No new per-node state.** The icon is a template expression, not a signal, computed, effect or
   store field. Adding one would be a contract violation, not an implementation detail.
5. **Nothing is written to the nodes.** The component never mutates `icon`, `expandedIcon`,
   `collapsedIcon` or any other field on the `TreeNode` objects it receives — several consumers pass
   NgRx signal-store state, which is frozen in dev builds.

## Test hooks

**Settled in T003. Every test task depends on this; do not change it without updating them.**

The icon element carries two attributes:

| Attribute | Value | Purpose |
|-----------|-------|---------|
| `data-testid` | `tree-node-folder-icon` | Locates the icon — one id, present on every folder row that has one |
| `data-expanded` | `"true"` / `"false"` | Makes the icon's *state* assertable without reading class strings |

So a test reads state directly off the element:

```ts
const icons = spectator.queryAll(byTestId('tree-node-folder-icon'));
expect(icons[0]).toHaveAttribute('data-expanded', 'false');   // collapsed row
```

**Why a `data-expanded` attribute rather than asserting the glyph class**: the glyph choice is a
judgment call flagged for review (research Decision 3 — PrimeIcons vs the Material Symbols house
standard), and swapping it must not invalidate a dozen state assertions. State assertions use the
attribute; **exactly one** test pins the actual glyph classes (`pi-folder` / `pi-folder-open`), so
the visual decision is still covered in one place and cheap to update if it changes.

`data-expanded` mirrors `node.expanded` and is not a second source of truth — both the attribute and
the glyph derive from the same expression in the same element.

Existing test ids on the component are unchanged: `tree-node-label`, `tree-toggler-loading`, and the
consumer-configurable `treeTestId` / `loadMoreTestId`.

## Consumers of this contract

| Consumer | Template | `showFolderIcons` | Note |
|----------|----------|-------------------|------|
| Content Drive sidebar | `dot-tree-folder.component.html` | `true` | The regression being fixed |
| Browser Selector sidebar | `dot-sidebar.component.html` | `true` | Preserves today's appearance once its node icon fields are dropped |
| Asset Picker sidebar | `dot-asset-picker-sidebar.component.html` | `true` | Also lets `ROOT_ICONS` be deleted |
| Site/Folder field picker | `host-folder-field.component.html` | `true` | Tree branch only; the flat search-results list is a different component |
| Roles tree | `dot-roles-tree.component.html` | **not set** | Non-goal. Draws its own Material Symbols icons. |

## Compatibility

Additive. A consumer that does not set the input is byte-for-byte unaffected, which is what makes the
roles tree safe and what makes this landable without coordinating the five call sites in one step.
No exported type changes: `expandedIcon` and `collapsedIcon` remain optional members of PrimeNG's
`TreeNode` that `TreeNodeItem` inherits — the node factories simply stop populating them.
