# Contract: title status (UI-to-UI)

The seam between the Edit Content editor and the three title areas.

## Producer: `EditContentHost.setTitleStatus(status: DotTitleStatus): void`

Added to `libs/edit-content/src/lib/services/host/edit-content-host.model.ts`.

| Host | Behavior |
|------|----------|
| `RouterEditContentHost` | `GlobalStore.setTitleStatus(status)`; on destroy `GlobalStore.clearTitleStatus()` (not on navigation: the route is reused for locale and related-content moves) |
| `OverlayEditContentHost` | writes a per-instance `titleStatus` signal. The side panel component forwards it to `DotSidePanelRef.titleStatus`; the dialog writes it to the holder read by the dialog header |

The editor calls it from one effect that watches the store's `titleStatus` computed, so any state
change (workflow action, save, restore, locale switch, history view) updates every surface.

## Consumers

| Surface | Reads | Renders |
|---------|-------|---------|
| Global header | `GlobalStore.titleStatus()` in `DotCrumbtrailComponent` | `dot-contentlet-status-badge` right after the title; the title truncates, the chip does not shrink |
| Side panel | `DotSidePanelRef.titleStatus` via `dot-side-panel` | badge right after the heading; heading truncates |
| Dialog | holder keyed by `DynamicDialogRef`, via the new header component | badge right after the dialog title |

## Rules

- `null` renders nothing; the chip never appears on pages that did not set it.
- A consumer must not derive state itself; it renders the `state` it was given.
- Test ids: `title-status-chip` wraps the badge in each surface (the badge keeps `status-tag`).
