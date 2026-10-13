# UI Contract: New Edit Contentlet UI polish (#37958)

What tests and other code may rely on after this change. Selectors that exist today keep
working; the only removed selectors are the PrimeIcons attributes/classes being migrated.

## Kept (must not change)

- Every `data-testid` / `data-testId` on the migrated buttons, the drag handle
  (`relationship-drag-handle`), the copy button (`host-folder-copy`, `host-folder-copy-icon`),
  the calendar (`calendar-input-<variable>`, `calendar-clear-button`) and the side panel
  (`edit-content-side-panel`, `side-panel-*`).
- Accessible names: `ariaLabel` / `aria-label` / `label` already on a button stay as they are.
  New glyphs are `aria-hidden="true"`, so they add nothing to a button's name.
- `pReorderableRowHandle` and `tabindex="0"` on the drag handle; no handle when disabled.

## Changed

| Element | Before | After |
|---------|--------|-------|
| Migrated buttons | `icon="pi pi-…"` attribute; small ones `size="small"` / `p-button-sm` | no `icon` attribute; `<span class="material-symbols-outlined" aria-hidden="true">name</span>` inside; default size |
| Category remove (`pButton`) | `icon="pi pi-times"`, `p-button-sm` | `<span pButtonIcon class="material-symbols-outlined">close</span>` child; default size |
| Date picker trigger | `[icon]="'pi pi-calendar' \| 'pi pi-clock'"` | `#triggericon` with `calendar_today` / `schedule` |
| Date picker overlay | inline `min-width` = input width wins | `panelStyleClass="min-w-0!"` |
| Drag handle | two `i.pi.pi-ellipsis-v` | one `span.material-symbols-outlined` = `drag_indicator` |
| Copy button | `size="small"`, glyph `text-lg` | default size, glyph without `text-lg` |
| Side-panel drawer header | no border | `pt.header.class` = `border-b border-surface-200` |
| `dot-chip-filter` `icon` | class string | Material Symbol name |
