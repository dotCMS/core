# UVE Editor Architecture

> Part 1 covers the layout & overlay subsystem — the contract that
> took the most iteration to get right. Part 2 zooms out to seams,
> boundaries, and rules. For implementation conventions see
> [`portlet/CLAUDE.md`](./portlet/CLAUDE.md) and
> [`portlet/src/lib/store/CLAUDE.md`](./portlet/src/lib/store/CLAUDE.md).

---

# Part 1 — Layout & overlay subsystem

The editor renders an iframe (the page being edited) and overlays
floating UI on top: a selection border, a hover toolbar, a drag
dropzone. Anything that moves contentlets — scroll, zoom, device
switch, sidebar opening, lazy images, media queries — invalidates
the overlay's pinned coordinates. Keeping overlays anchored without
flicker, races, or jumps is the central problem.

## Three layers

```
┌─ Editor (Angular host) ────────────────────────────┐
│  overlays, toolbars, dropzone                      │
│  reads UVEStore signals to render                  │
└──────────────────────┬─────────────────────────────┘
                       │ postMessage
┌──────────────────────┴─────────────────────────────┐
│ SDK (runs INSIDE the iframe)                       │
│  measures layout, dispatches events                │
│  owns the bounds-sync channel                      │
└────────────────────────────────────────────────────┘
```

Communication is exclusively `postMessage`. The SDK never imports
editor code; the editor never reaches into iframe DOM.

## The bounds-sync contract

**One channel, push-based.** The SDK runs a debounced `ResizeObserver`
on `documentElement` and every `[data-dot-object="container"]`, plus
a `MutationObserver` for containers that mount/unmount, plus a
passive `scroll` listener. Whenever any fires, after a 100ms
trailing-edge debounce, the SDK measures the page and posts
`SET_BOUNDS` to the editor. The editor doesn't ask — it receives.

**Escape hatch**: drag-drop needs synchronous bounds (the dropzone
has to know container rectangles before the user moves another
pixel). The editor sends `UVE_FLUSH_BOUNDS` and the SDK emits
immediately, bypassing the debounce.

## State on the editor side

The store has flat state with domain prefixes (`editor*`, `view*`,
`page*`). Two pieces matter here:

- **`editorState`** — single enum: `IDLE | DRAGGING | SCROLLING |
  SCROLL_DRAG | RESIZING | INLINE_EDITING | ERROR`. Mixes user-driven
  modes with layout-driven phases (a known compromise).
- **`$iframeLayoutLocked`** — named computed: `state === SCROLLING ||
  SCROLL_DRAG || RESIZING`. The single answer to *"is the iframe in
  a transient layout phase, so overlays should hide and bounds are
  stale?"* Consumers gate on this instead of enumerating enum members.

## Lifecycle of a reflow

```
Trigger fires
  → editorState flipped to RESIZING / SCROLLING / SCROLL_DRAG synchronously
Overlays hide (gate on !$iframeLayoutLocked)
Layout settles inside the iframe
  → SDK ResizeObserver / scroll fires
  → 100ms debounce
  → SDK posts SET_BOUNDS
withSelectionAnchor.applyBoundsForSelection(bounds):
  → patch editorBounds
  → look up selected by (inode, containerId, containerUuid)
  → patch editorSelected.bounds with fresh coords
  → flip editorState to IDLE if lock was held
Overlays reappear at the correct position
```

### Critical invariant

**`SET_BOUNDS` is the only thing that flips IDLE for transient
phases.** Earlier we had multiple sites flipping IDLE on rAF after
the trigger; that raced the bounds round-trip and caused visible
"jumps" — overlay flashed at stale coordinates before snapping. The
destroy/cancel paths still have a manual `updateEditorOnResizeEnd()`
as a safety net (e.g. resize-handle pointerup with no actual size
change), but it's a no-op when `SET_BOUNDS` got there first.

### Match key

The same contentlet identifier can appear in multiple containers, or
in the same container under different uuids. The re-anchor lookup
matches on `(contentlet.inode, container.identifier, container.uuid)`
— inode alone would re-anchor to whichever instance iterates first.

## Overlays

The contentlet-tools component renders two overlays:

- **Hover overlay** — follows the pointer. Shows the content-type
  label AND the action toolbar (drag, pencil, delete, palette, add
  buttons). Hides on a `null` hover signal from the SDK (pointer
  moved to dead space).
- **Selected overlay** — pinned to the clicked contentlet. Label and
  border, **no tools**. Persists across hover changes; hides during
  transient layout phases.

Tools live exclusively on hover. When the user clicks pencil/palette
in the hover toolbar, the component calls `promoteHoverToSelected()`
first, then emits — so the side panel opens with the right contentlet
selected.

The SDK's hover tracker fires `null` on `pointermove` over dead space,
but **not** on `pointerleave` of the iframe document — leaving the
iframe usually means heading for the floating toolbar in parent
chrome, and killing the overlay there would yank it away mid-reach.

## Slice composition (excerpt)

`withSelectionAnchor` is composed *after* `withEditor` because it
calls editor primitives (`setEditorBounds`, `setSelected`,
`setEditorState`, `getPageSavePayload`). Cross-feature method access
uses a typed cast (`StoreWith*Deps<typeof store>`) — the convention
is documented in store CLAUDE.md.

## Related files

- `store/dot-uve.store.ts` — slice composition order
- `store/features/editor/withEditor.ts` — `$iframeLayoutLocked`, primitives
- `store/features/editor/withSelectionAnchor.ts` — `applyBoundsForSelection`
- `services/dot-uve-actions-handler/` — message router (thin dispatcher)
- `services/iframe-messenger/` — postMessage envelope (`flushBounds`, etc.)
- `edit-ema-editor/components/dot-uve-contentlet-tools/` — the overlays
- `libs/sdk/uve/src/internal/events.ts` — SDK observers
- `libs/sdk/uve/src/script/utils.ts` — SDK ↔ editor dispatcher

---

# Part 2 — System at a glance

## The seam

The parent window owns chrome (toolbars, sidebars, dialogs, overlays);
the iframe owns the page being edited. They share no code —
everything goes through `postMessage`, with the SDK as the
iframe-side broker.

This is the single most important architectural choice and the source
of every other rule. The iframe could be a Next.js app, a VTL page,
or anything else; the editor works the same way.

## Where state lives

There is **one store** (`UVEStore`) and it lives in the parent. Every
editor concern — what page is loaded, what's selected, what's being
dragged, what the iframe layout is doing — is a property on the
store, with strict naming prefixes and a flat shape.

The iframe holds DOM and observers, no state. Anything the SDK
observes is immediately posted back; the SDK never accumulates. The
store is the source of truth; the iframe is a measuring instrument.

## Glossary: two contentlet signals

### `editorContentArea` — the **hovered** contentlet

- Shape: `ContentletArea` (bounds + `ActionPayload`).
- Updated on every pointermove inside the iframe.
- Drives the **hover overlay** (border + action toolbar).
- Cleared when the SDK signals null-hover (pointer moved to dead
  space inside the iframe) or on navigation.

### `editorSelected` — the **selected** contentlet

- Shape: `SelectedContentlet` = `{ bounds, payload }`.
- Set by: the SDK's CONTENTLET_CLICKED event (full contentlet click
  in the iframe), and the hover toolbar's bolt (quick-edit) / palette
  (style-editor) buttons via `promoteHoverToSelected`. **Not set by
  the pencil button** — pencil is intentionally stateless with
  respect to selection.
- Drives both the **selected overlay** (border anchored to `bounds`)
  AND the **side panel's data binding** (quick-edit form, style
  editor read `payload`). One signal, two surfaces.
- Re-anchored on every iframe reflow by `withSelectionAnchor`'s
  `applyBoundsForSelection` — looks up by inode + container key and
  patches `bounds` with fresh coords.
- Hides — but doesn't clear — during `$iframeLayoutLocked` phases.
- Cleared on navigation, lock, full-editor cancel, etc.

### Two setters

- `setSelected({ bounds, payload })` — replace the whole record.
  Used when the user picks a new contentlet (CONTENTLET_CLICKED,
  promote-from-hover).
- `setSelectedPayload(payload)` — patch only the payload, preserving
  bounds. Used after a save / fork where the contentlet's data
  changed but its on-screen position did not.

### The pencil button's stateless contract

Pencil (full-editor modal) does NOT write to `editorSelected`. It
receives the hovered contentlet's `ActionPayload` directly via the
`openFullEditor` output and passes it to `dialog.editContentlet(...)`
without touching editor selection state. The modal stays orthogonal
to the selection surface — closing it leaves the editor exactly
where it was.

### Why two, not three (history)

There used to be three signals: `editorSelectedContentletArea`
(bounds), `editorActiveContentlet` (payload), `editorContentArea`
(hover). The first two were always set and cleared in lockstep —
the split between "bounds for the overlay" and "payload for the
panel" was vestigial. They've been merged into the unified
`editorSelected` record. The hover signal stays separate because
it has a genuinely different lifecycle (pointer-driven, transient).

## Cross-cutting rules

- **Observed reflows are pushed.** The SDK debounces and emits;
  the parent doesn't poll. Drag-drop is the one exception, and it
  uses an explicit "flush now" message — still not polling.
- **Transient layout phases hide overlays.** `$iframeLayoutLocked`
  is the gate. When fresh bounds arrive, the lock releases. Prevents
  flicker (Part 1).
- **Optimistic updates with rollback.** Mutations apply immediately
  for responsiveness; a snapshot is taken before each mutation, and
  on save failure the snapshot is restored.
- **Async via `rxMethod`.** All HTTP/streaming flows go through it
  for cancellation. Raw `.subscribe()` inside the store is debt.
- **Errors flow through one handler** (`DotHttpErrorManagerService`).
  `catchError` → `handle(error)` → `return EMPTY`. No custom error
  dialogs.
- **Navigation keeps chrome mounted.** `pageLoad` flips `uveStatus`
  to `LOADING` and resets readiness/history but **does not** null
  `pageAssetResponse`. Toolbars, sidebars, navigation, and overlays
  keep rendering against the previous page's asset until the new one
  resolves and replaces it via `setPageAsset` — preventing a full
  unmount/remount flash on link clicks, persona/language switches,
  and any other path that fires `pageLoad`. The split is encoded as
  two methods: `resetClientConfiguration` (full teardown) and
  `markPageLoading` (in-flight reset, asset preserved).

## A user action, end-to-end

Inline editing illustrates how the rules compose:

1. Iframe captures the double-click; SDK posts a structured message.
2. Actions handler routes it; store flips a state-machine flag.
3. State change cascades through computeds — overlays reconfigure,
   TinyMCE mounts into the iframe via the SDK bridge.
4. User types; edits stay in TinyMCE.
5. On commit, a workflow action fires; the store optimistically
   updates the page asset.
6. On success, `pageReload`. On failure, the pre-edit snapshot is
   restored.

No special path — composes the cross-cutting rules and the seam.

## What this gets you

- **Substitutable iframe.** Headless apps and traditional pages plug
  in the same way; only bundle delivery differs.
- **Single source of truth.** Debugging "what does the editor think?"
  is reading the store, not the DOM.
- **Bounded blast radius.** Iframe pages can't crash the editor;
  editor bugs can't corrupt iframe content.

## How to extend

| Adding…                          | Touch                                                                  |
|----------------------------------|------------------------------------------------------------------------|
| A new transient layout phase     | `EDITOR_STATE` member, include in `$iframeLayoutLocked`, trigger site. |
| A new SDK → editor event         | Public+internal type, SDK subscriber, dispatcher, actions handler.     |
| A new editor → SDK event         | Internal type, messenger method, SDK listener.                         |
| A new store concern              | New slice, compose in order; cross-feature deps via `props` or cast.   |
| A new screen                     | Routed component under `edit-ema-*/`, register in app routes.          |

## Where to find things

- Store: `portlet/src/lib/store/`
- Editor screen + components: `portlet/src/lib/edit-ema-editor/`
- Layout-tab screen: `portlet/src/lib/edit-ema-layout/`
- Cross-screen components: `portlet/src/lib/components/`
- Services (drag/drop, inline edit, page API, messenger, actions handler): `portlet/src/lib/services/`
- SDK that runs inside the iframe: `libs/sdk/uve/`
- Event types: `libs/sdk/types/src/lib/`
