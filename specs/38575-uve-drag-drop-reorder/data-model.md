# Data Model: UVE drag-and-drop reorder

No stored data changes. These are the in-memory shapes the change adds or alters. Paths are relative to `core-web/libs/`.

## Resolved drop target (new, editor only)

Produced by the dropzone, consumed by the canvas drop handler.

| Field | Type | Meaning |
|-------|------|---------|
| `kind` | `'contentlet' \| 'empty-container' \| 'none'` | `none` when hovering the source placeholder or a position that leaves the order unchanged |
| `payload` | client data of the container, plus the pivot contentlet for `contentlet` | Same shape the drop handler parses from `data-payload` today |
| `position` | `'before' \| 'after' \| undefined` | Set only for `contentlet` |

Rules:

- `none` → drop is ignored: no save, editor reset to idle.
- `contentlet` → insert next to the pivot.
- `empty-container` → append; only produced when the container has no contentlets.

## Dropzone contentlet (existing, one derived flag)

`ContentletArea` in `portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/ema-page-dropzone/types.ts` gains nothing stored; the template derives `isSource` per contentlet by comparing its payload identifier with the dragged item's when the drag is a move.

## Scroll velocity (new, editor → page)

| Field | Type | Meaning |
|-------|------|---------|
| `name` | `'uve-scroll-velocity'` | Message name |
| `velocity` | `number` | Signed pixels per 16.67ms frame; negative scrolls up, `0` stops |

Validation in the page: non-finite values are treated as `0`; magnitude is clamped to the maximum the editor can send.

## Scroll report (existing, page → editor, one added field)

| Field | Type | Meaning |
|-------|------|---------|
| `action` | `'scroll'` | Existing action |
| `payload.scrollY` | `number`, optional | Page's vertical scroll offset; absent on older SDKs |

## Editor store (existing `withEditor` slice)

- New transient value: the `scrollY` at which `editorBounds` was last snapshotted, used to compute the delta. Reset whenever fresh bounds arrive.
- State transitions during a drag:
  - `DRAGGING` → (scroll report with `scrollY`) → stays `DRAGGING`, bounds shifted.
  - `DRAGGING` → (scroll report without `scrollY`) → `SCROLL_DRAG`, bounds cleared → (fresh bounds) → `DRAGGING`. Unchanged from today.
  - Any → (drop, drag end, drag leave) → velocity `0` sent, state as today.
