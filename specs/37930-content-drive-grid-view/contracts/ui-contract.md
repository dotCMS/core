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

## `dot-folder-list-view` (shared) — additive, optional

Unbound, every addition behaves as before, so the other consumers (pickers, Action Center,
edit-content, publishing-queue, experiments) are unchanged.

- `layout` input: `'table'` (default) or `'grid'`. One component, two layouts: selection and
  ranges, the anchor and tab stop, the page lock and paginator, drag and drop, the skeleton and the
  empty state are shared, so a switch keeps the user's place. The grid pages server-side only.
- `#card` content template, required by the grid, with context `DotFolderListViewCardContext`:
  `$implicit` (item), `id`, `selected`, `busy`, `dragOver`, `readOnly`, `languagesMap`, and the
  callbacks `toggle(event)`, `open(event)`, `menu(event)`. The listing labels each card by
  `{id}-label` and describes it by `{id}-description`; the drag picture is copied from the body's
  `[data-drag-preview]` element.
- `rows` input (default 20), `sortField` (default `modDate`) and `sortOrder` (default `-1`):
  a switch back rebuilds the table, which must show the caller's page size and sort.
- A drag is accepted and highlighted over folders only; `dragleave` clears the highlight.

## `dot-content-drive-grid-card` (portlet-local)

The Drive's card body. Inputs: `item`, `cardId`, `selected`, `busy`, `readOnly`, `dragOver`,
`languagesMap`. Outputs: `open`, `toggleSelect`, `menu` (each a `MouseEvent`).
