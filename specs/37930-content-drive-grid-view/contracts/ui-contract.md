# UI Contract: Grid view for Content Drive (#37930)

## URL query parameters (Content Drive route)

| Param | Values | Written by the Drive? | Meaning |
|-------|--------|-----------------------|---------|
| `viewType` | `list` \| `card` (any case on read; lower case on write) | Yes, on every view change (history `replace`) | Explicit view. Wins over everything. |
| `defaultViewType` | `list` \| `card` | No (left as found) | A custom tool's default (#37931). Loses to the remembered view. |

## Browser storage

- Key: `dotcms.content-drive.view-type.<portletId>`; value `list` or `card`.
- `portletId` = `DotRouterService.currentPortlet.id` (first URL segment).
- Written only when the user picks a view with the switcher.

## `dot-content-drive-grid-view` (portlet-local)

Inputs: `items`, `selection`, `loading`, `busyRows`, `totalItems`, `offset`, `rows`,
`languagesMap`, `activeKey`, `readOnly` (default `false`).

Outputs: `selectionChange(DotContentDriveItem[])`, `paginate(DotContentDrivePaginateEvent)`,
`open(DotContentDriveItem)`, `rightClick(ContextMenuData)`, `dragStart(DotContentDriveItem[])`,
`dragEnd()`, `drop(DotContentDriveItem)`, `activeKeyChange(string | null)`.

Public method: `revealActive()` — scrolls the tab-stop card into view.

## `dot-folder-list-view` (shared) — additive, optional

- `rows` input (default 20 — today's value).
- `activeKey` input / `activeKeyChange` output (unbound → no change).
- `revealActive()` public method.
