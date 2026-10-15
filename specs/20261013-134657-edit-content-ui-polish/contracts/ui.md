# UI Contract: New Edit Contentlet UI polish (#37958)

What tests and other code may rely on after this change. Selectors that exist today keep
working; the only removed selectors are the relationship table's PrimeIcons attributes/classes.

## Kept (must not change)

- `data-testid` on the relationship add/remove buttons (`relationship-add-button`,
  `relationship-delete-button`), the drag handle (`relationship-drag-handle`), the empty state
  (`relationship-field-empty`), the copy button (`host-folder-copy`, `host-folder-copy-icon`),
  the calendar (`calendar-input-<variable>`, `calendar-clear-button`) and the side panel
  (`side-panel`, `side-panel-*`).
- New glyphs are `aria-hidden="true"`, so they add nothing to a button's accessible name.
- `pReorderableRowHandle` and `tabindex="0"` on the drag handle; no handle when disabled.
- The date picker trigger keeps `[icon]` with `pi pi-calendar` / `pi pi-clock`.

## Changed

| Element | Before | After |
|---------|--------|-------|
| Relationship add / remove | `icon="pi pi-plus"` / `icon="pi pi-times"`, `size="small"` | no `icon` attribute; `#icon` template with `add` / `close`; default size; `[rounded]="true"` |
| Relationship empty state | `<i class="pi pi-folder-open">` | `<span class="material-symbols-outlined" aria-hidden="true">folder_open</span>` |
| Drag handle | two `i.pi.pi-ellipsis-v` | one `span.material-symbols-outlined` = `drag_indicator` |
| Date picker overlay | inline `min-width` = input width wins | `(onShow)` → `onPopupShow()` clears it and re-aligns |
| Copy button | `size="small"`, glyph `text-lg` | default size, glyph without `text-lg` |
| Side-panel content area (Edit Content only) | `contentStyleClass` = `p-0!` | `p-0! border-t border-surface-200` |
