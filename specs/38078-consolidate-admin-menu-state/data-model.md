# Data Model: Menu state in GlobalStore

**Spec**: [spec.md](./spec.md) · **Research**: [research.md](./research.md)

Frontend-only. Nothing here is persisted except `isNavigationCollapsed` (existing `localStorage` key
`dotcms.menu.status`, unchanged). No backend, database or search-index change.

## Two kinds of state

| Kind | Fields | Replaced by a menu load? | Persisted? |
|------|--------|--------------------------|------------|
| **Server snapshot** | menu item entities, `status` | Yes, as a whole | No |
| **UI state** | `isNavigationCollapsed`, `openParentMenuId`, `activeKey` | **No** | `isNavigationCollapsed` only |

Today the active flag lives on the entities (`active: boolean`), so a replace erases it. Moving it to
`activeKey` is the structural fix for defect 4.

## Server response (input, unchanged)

`GET /api/v1/menu` → `entity: DotMenu[]`, one element per group, already sorted by the user's `tabOrder`.

| Field | Meaning |
|-------|---------|
| `id` | Layout UUID (full) |
| `name`, `tabName`, `tabIcon` | Group labels/icon |
| `url` | URL of the group's first portlet |
| `menuItems[]` | Items: `id` (portlet id), `label`, `url`, `angular`, `ajax`, `initParams` |

`DotMenu.active`, `isOpen`, `label`, `tabDescription` and `DotMenuItem.active` are unused leftovers: the
resource never sends them and no code may use them as state (FR-012). They are marked `@deprecated` in the
models rather than removed, to keep this change inside its scope.

## Entity: `MenuItemEntity` (one per portlet per layout)

| Field | Type | Source / rule |
|-------|------|---------------|
| `id` | string | Portlet id (from API). **Not** the entity key. |
| `label`, `url`, `angular`, `ajax`, `initParams` | as API | Copied unchanged; `url` stays raw for the iframe. |
| `parentMenuId` | string | Full layout UUID. |
| `parentMenuLabel`, `parentMenuIcon` | string | `tabName`, `tabIcon` of the group. |
| `menuLink` | string | `item.angular ? item.url : '/c/' + item.id` (unchanged rule). |
| `shortLayoutId` | string | First 4 characters of `parentMenuId`; the value `mId` carries. |
| `tabOrder` | number | Index of the group in the response array (server sort position). |
| `itemIndex` | number | Index of the item within its group. |

**Entity key (primary id)**: `` `${id}__${parentMenuId}` `` — portlet id + **full** layout UUID. Two layouts
sharing a 4-character prefix and a portlet id therefore stay two entities (FR-009).

**Validation / invariants**
- Keys are unique within a snapshot (portlet id appears at most once per layout; a layout with no portlets is
  omitted by the backend and never renders as an empty group).
- `shortLayoutId === parentMenuId.substring(0, 4)` always.
- Replacing the snapshot removes every previous entity and adds the new set; it never merges.

## State: `MenuSlice`

```text
status:                 'idle' | 'loading' | 'ready' | 'error'      (initial 'idle')
isNavigationCollapsed:  boolean                                      (initial true; persisted)
openParentMenuId:       string | null   -- full layout UUID           (initial null)
activeKey:              string | null   -- an entity key              (initial null)
```

`activeKey` starts `null` after a browser reload (event 1); the resolver fills it once the menu has loaded.

## Derived (computed) views

| Name | Definition |
|------|------------|
| `menuGroup` | Entities grouped by `parentMenuId`, sorted by `tabOrder` then `itemIndex`; each group carries `id`, `label`, `icon`, `menuItems`, `isOpen` (`id === openParentMenuId`). Sorting is explicit, not insertion order (FR-010). |
| `activeMenuItem` | `entityMap[activeKey] ?? null` |
| `firstMenuItem` | First item of the first group in `menuGroup` |
| `isGroupActive` | `activeKey !== null` and its group exists |
| `hasPortlet(id, { jspOnly })` | Any entity with `id`, and `!angular` when `jspOnly` |
| `layoutIdForPortlet(id)` | `parentMenuId` of the first entity with that `id`, else `null` |

## State transitions

```text
idle ──ensureLoaded/reload──▶ loading ──success──▶ ready
                                  │                  │
                                  └──failure──▶ error ◀─ (a failed reload keeps the entities that were showing)
ready ──reload──▶ loading   (entities stay on screen while loading)
error ──ensureLoaded/reload──▶ loading
```

| Event | Entities | `activeKey` | `openParentMenuId` | `isNavigationCollapsed` |
|-------|----------|-------------|--------------------|-------------------------|
| Browser reload (F5) | empty until load | `null` → resolver fills it after load | `null` | restored from storage |
| Load/replace succeeds | replaced | kept if the key still exists, else `null`; then re-resolved from the URL if `null` | kept | kept |
| Load fails, snapshot showing | unchanged | unchanged | unchanged | unchanged |
| Load fails, first load | empty | `null` | `null` | unchanged |
| `NavigationEnd` | unchanged | resolver result (may be `null`) | opened to the item's group | unchanged |
| `loadFreshPortletUrl` | **unchanged** | **unchanged** | unchanged | unchanged |

## Resolver input/output

`resolveMenuItem({ url, items, openParentMenuId, breadcrumbs }) → MenuItemEntity | null`, pure. Order (FR-013):
1. Portlet id from `url` + `mId` against `shortLayoutId`; on a short-id tie prefer the entity whose
   `parentMenuId === openParentMenuId`, otherwise the lowest `tabOrder` (FR-014).
2. If `mId` is absent: first breadcrumb with a URL matched by label, then portlet id.
3. Exact `menuLink` path equality (never `indexOf`).
