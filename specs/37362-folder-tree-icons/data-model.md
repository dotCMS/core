# Phase 1 Data Model: folder-tree node presentation

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-09-03

No entity is created, and no persisted or transported data changes. What changes is which
*presentation* fields the frontend node factories populate on the tree-node objects they hand to
PrimeNG. This document is the before/after of that shape, per consumer, plus the rule the shared
component applies to it.

## The entity in play

**Tree node** — a row in the folder tree. Structurally it is PrimeNG's `TreeNode`, extended in this
repo as `TreeNodeItem` (`@dotcms/dotcms-models`) and, for Content Drive, `DotFolderTreeNodeItem`
(`@dotcms/portlets/content-drive/ui`).

Fields that matter here — all of them optional members that already exist on PrimeNG's `TreeNode`,
so nothing below is a type change:

| Field | Meaning | Owner after this change |
|-------|---------|-------------------------|
| `icon` | A fixed icon for this row, state-independent. PrimeNG draws it and it wins over the pair below. | **Consumer** — for rows that are not folders (the site row) |
| `expandedIcon` | Icon while expanded, drawn by PrimeNG | **Nobody** — no factory sets it any more |
| `collapsedIcon` | Icon while collapsed, drawn by PrimeNG | **Nobody** — no factory sets it any more |
| `expanded` | Whether the row is open. Mutated by PrimeNG on toggle. | PrimeNG, with one startup write by the Site/Folder field store |
| `leaf` | `false` forces an expand affordance; otherwise PrimeNG infers from `children` | **Consumer** — from the API's `hasChildren` |
| `type` | Selects which `pTemplate` renders the row | **Consumer** — `LOAD_MORE_NODE_TYPE` on load-more rows only |
| `children` | Loaded child rows | Consumer stores |
| `data` | Domain payload (id, path, hostname, permissions…) | Consumer stores — **untouched by this work** |

## The rule the shared component applies

For each row rendered through the `default` node template, when folder icons are enabled:

```
if (node.icon || node.expandedIcon || node.collapsedIcon)  →  draw nothing
                                                              (PrimeNG already drew the row's own icon)
else if (node.expanded)                                    →  draw the open-folder icon
else                                                       →  draw the closed-folder icon
```

Three consequences worth stating, because each one is a requirement:

- **Load-more rows never reach this rule.** They carry `type: LOAD_MORE_NODE_TYPE`, so PrimeNG
  dispatches them to the `load-more` template instead of `default` (FR-007).
- **The icon does not depend on `children`.** PrimeNG's own `getIcon()` requires
  `node.expanded && node.children?.length` before it will draw an expanded icon; this rule reads
  `expanded` alone, so a row that is open while its children load still looks open (FR-002, and the
  "expanded but empty" edge case).
- **Leaf rows still get an icon.** The rule lives in the node row, not the toggler, and PrimeNG only
  renders a toggler for non-leaf rows — so a childless folder shows a closed-folder icon and no
  expand affordance (FR-001 + FR-005 together).

## Before / after, per factory

### Content Drive — `libs/portlets/dot-content-drive/portlet/src/lib/utils/tree-folder.utils.ts`

| Factory | Row | Before | After |
|---------|-----|--------|-------|
| `createSiteNode` | site root | `icon: 'pi pi-globe'` | unchanged — the site stays a site (FR-006) |
| `createTreeNode` | folder | *no icon field at all* → **the bug** | unchanged (still no icon field) — the shared component now supplies it |

The fix for US1 is therefore **not** in this file. `createTreeNode` is already correct under the new
rule; what was missing was a renderer. The file is listed in the plan only because its behaviour is
asserted by the new factory test.

### Browser Selector / Site/Folder field / Asset Picker — `libs/ui/src/lib/services/dot-browsing/dot-browsing.service.ts`

| Factory | Row | Before | After |
|---------|-----|--------|-------|
| `mapSiteToTreeNode` | site | `expandedIcon: 'pi pi-globe'`, `collapsedIcon: 'pi pi-globe'` | `icon: 'pi pi-globe'` |
| `#createFolderTreeNode` | folder | `expandedIcon: 'pi pi-folder-open'`, `collapsedIcon: 'pi pi-folder'` | *removed* |
| `#createFolderSearchTreeNode` | folder (search) | same pair | *removed* |
| site-root branch in the preselected-path builder | site | same globe pair | `icon: 'pi pi-globe'` |

The globe pair collapsing to a single `icon` is not a behaviour change: both slots already held the
same value, which is the definition of a state-independent icon.

### Asset Picker — `libs/ui/src/lib/components/dot-asset-picker/store/features/with-asset-folder-tree.feature.ts`

| Before | After |
|--------|-------|
| `ROOT_ICONS = { expandedIcon: 'pi pi-folder-open', collapsedIcon: 'pi pi-folder' }`, applied to the one root to *override* the globe `mapSiteToTreeNode` gave it | `ROOT_ICONS` deleted. The root declares no icon, so the shared rule gives it the folder icon — which is what the override was for. |

This is the one place the change makes a consumer strictly smaller. The reason `ROOT_ICONS` exists —
the picker shows a globe in its site selector above the tree, so a second globe on the root reads as
a duplicate control — is preserved by the new default, so the explanatory comment moves rather than
disappearing.

## State transitions

One transition, and it is PrimeNG's:

```
collapsed ──user clicks toggler──▶ expanded ──user clicks toggler──▶ collapsed
   │                                   │
   └─ closed-folder icon               └─ open-folder icon
```

`UITreeNode.toggle()` writes `node.expanded` and, being a `(click)` listener inside that `OnPush`
view, dirties it — so the next check re-evaluates the icon expression. There is no second source of
truth to keep in sync: the icon is a function of `node.expanded`, computed at render time.

The one place application code writes `expanded` is the Site/Folder field store's
`expandFoldersToTarget`, which opens the ancestors of a preselected folder during startup
resolution. Under this rule those ancestors draw the open icon on first paint with no extra work
(the deep-branch edge case), and because that function does not run on later rebuilds, a subsequent
manual collapse is not undone.

## Validation rules

There are none to add. Every field above is optional in PrimeNG's `TreeNode`, the shared component
reads them defensively (an absent field is a valid state that selects the closed icon), and no value
crosses a trust boundary — these objects are built in the browser from already-validated API
responses and are never sent anywhere.
