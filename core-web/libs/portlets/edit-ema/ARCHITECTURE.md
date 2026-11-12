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

## Drag and drop

### The dropzone decides where a drop goes

The dropzone (`edit-ema-editor/components/ema-page-dropzone/`) is the one place that resolves a drop target. On every `dragover` inside a container it works out the target from the bounds and the pointer, draws the insertion line there, and reports the target through its `dropTargetChange` output (an `EmaDropTarget`), only when it changes. The canvas keeps the last target and drops on it; it never reads `data-*` from the element under the pointer. The insertion line and the drop therefore cannot disagree.

- Over a contentlet: before or after it, by which half the pointer is in.
- Between contentlets: next to the contentlet whose vertical centre is nearest, before it when the pointer is above that centre. A container appends only when it holds no contentlets (`kind: 'empty-container'`; its overlay's `data-empty` says so).
- No target (`kind: 'none'`, insertion line hidden): over the moved contentlet's own place, right after the contentlet above it, right before the one below it, over a container's error zone, over the dropzone outside every container, and once the pointer leaves the dropzone.

While a contentlet on the page is moved, the dropzone draws a gray placeholder over its original place, only in the container it came from (matched on container identifier and uuid, since one contentlet can sit in several containers). The placeholder takes the pointer, so it does not fall through to the container, and resolves to no target.

The canvas resets the editor without saving when the target is `none`, when the drop lands outside the dropzone (`data-dropzone` on the drop's target is not `"true"`), or when a move would leave its container's order as it is (`doesMoveChangeOrder` in `utils/index.ts`). At the utils level a positioned insert whose pivot is gone from a non-empty container inserts nothing rather than appending.

### Auto-scroll during a drag

The page scrolls itself; the editor only sets the speed.

- **Editor → page, `uve-scroll-velocity`**: `DotUveDragDropService` turns the pointer's depth in the 100px edge zone into a signed velocity in px per 60fps frame, eased quadratically from `IFRAME_SCROLL_VELOCITY.MIN` to `MAX` (`shared/consts.ts`), negative near the top. It is sent only when it changes, and `0` is sent when the pointer leaves the zone, on drop, on drag end and when the drag leaves the window.
- **Page**: `registerUVEEvents` (`libs/sdk/uve/src/script/utils.ts`) runs a `requestAnimationFrame` loop of instant `scrollBy` steps scaled by frame time, clamped to 40px per frame, until the velocity is `0`, the page reaches its top or bottom, or the subscriptions end. Anything that is not a finite number counts as `0`.
- **Page → editor, `scroll`**: the report carries `payload.scrollY`. The store records every reported value in `editorScrollY` (reset to 0 when a new document loads in the iframe). During a drag, `updateEditorScrollState(scrollY)` keeps `editorState` at `DRAGGING` and moves every container in `editorBounds` by how far the page scrolled since the bounds were taken; contentlets are relative to their container, so they move with it. The dropzone stays mounted and aligned while the page scrolls.
- **Edge zone**: the editor no longer clears the bounds on `dragover`. At the top or bottom of the page nothing scrolls and no `scroll` report arrives, so the first and last contentlets keep their targets.

### Older `@dotcms/uve`

The editor keeps sending the one-step `uve-scroll-inside-iframe` direction request, at most once every `IFRAME_LEGACY_SCROLL_THROTTLE_MS` (250ms), for pages that do not know the velocity message; they answer with a smooth 120px step as before. A page that has received a velocity ignores the direction request for as long as its events are registered. A `scroll` report without `scrollY` takes the old path: bounds cleared, `SCROLL_DRAG`, then `SET_BOUNDS` brings them back. That is the only path that still uses `SCROLL_DRAG` during a drag. Each side discovers the other from what arrives, with no version handshake.

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
- `edit-ema-editor/components/ema-page-dropzone/` — drop targets, drop-target resolution, source placeholder
- `services/dot-uve-drag-drop/` — drag events, edge-zone velocity, legacy scroll throttle
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

The pencil (the full editor), and the full-edit item in the contentlet tools' menu, do NOT write to `editorSelected`. `DotUveContentletToolsComponent.editContent` reads the hovered contentlet's `ActionPayload` and dispatches `uveEditContentRequested` with the contentlet, and its container as `forkFrom`; it never calls `promoteHoverToSelected`. It sends nothing for a contentlet without an inode or whose `canEdit` is explicitly `false`.

`DotUveContentEditorComponent` is the only listener, and it decides the editor. A contentlet on more than one page first asks whether to edit it on every page or a copy on this page only; a copy is made in place, the page reloads, and the copy opens. Then the content type decides: the new editor when it enables `CONTENT_EDITOR2_ENABLED`, in the side panel when `FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL` is on and in a centered dialog otherwise; the legacy dialog for any other content type, or when the contentlet has none or it cannot be read.

The full editor stays orthogonal to the selection surface: the pencil leaves `editorSelected` as it was.

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
- Editor screen and its regions: `portlet/src/lib/edit-ema-editor/` (see below)
- Layout-tab screen: `portlet/src/lib/edit-ema-layout/`
- Cross-screen components: `portlet/src/lib/components/`
- Services (drag/drop, inline edit, page API, messenger, actions handler): `portlet/src/lib/services/`
- SDK that runs inside the iframe: `libs/sdk/uve/`
- Event types: `libs/sdk/types/src/lib/`

### The editor's regions

`EditEmaEditorComponent` (`edit-ema-editor/edit-ema-editor.component.ts`) is layout only: it places the regions on its grid and decides which show in edit and preview mode. It injects only `UVEStore`, binds no inputs or outputs on its regions, and its one call is to put the edit panel back on quick edit each time it is created, because the store outlives it. It also provides `DotPaletteListStore`, so the palette's lists keep their search while the palette hides and comes back, and places the confirm dialog the toolbar, canvas and content editor ask through.

Each region reads and writes `UVEStore` itself: store state for what persists, and the `uveContentEditorEvents` group (`edit-ema-editor/components/dot-uve-content-editor/dot-uve-content-editor.events.ts`) for one-off requests to open a content editor. Only the content editor listens to that group. Folders are under `portlet/src/lib/edit-ema-editor/components/`.

| Region | Folder | Shows | Owns | Store state it uses | Requests it sends |
|---|---|---|---|---|---|
| Toolbar | `dot-uve-toolbar/` | Always | Mode, API link, device and social-media selector, URL-mapped content edit, live-mode date, page lock, running experiment, language, persona, workflow actions | `viewMode`, `pageParams`, `pageLanguages`, `$viewDeviceSelectorState`, `viewSocialMedia`, `workflowToggleLock` | `uveTranslatePageRequested`, `uveEditUrlContentMapRequested` |
| Palette | `dot-uve-palette/` | Edit mode, while `editorCanEditContent()` | Content types, widgets, favorites and the layer tree; opens and closes itself | `editorPaletteOpen`, `pageURI`, `pageLanguageId`, `pageVariantId`, `$allowedContentTypes` | None |
| Browser toolbar | `dot-uve-browser-toolbar/` | Always, hidden during a social-media preview | Palette and edit-panel toggles, page URL, device controls, zoom, iframe size | `editorPaletteOpen`, `editorEditPanelOpen`, `$viewDeviceSelectorState`, `viewZoomLevel` | None |
| Canvas | `dot-uve-canvas/` | Always, hidden during a social-media preview | The page's iframe and the overlays on it, messages to and from the page, drops, deletes, link clicks, sizing; reads the page's og tags into the store | `editorSelected`, `editorContentArea`, `editorDragItem`, `editorState`, `$iframeURL`, `$viewCanvasOuterStyles`, `$viewCanvasInnerStyles`, `editorOgTags`, `viewOgTagsResults` | `uveCreateContentRequested`; through its contentlet tools and actions handler also `uveEditContentRequested`, `uveAddContentRequested`, `uveEditSourceRequested`, `uveEditBlockEditorFieldRequested`, `uveReorderMenuRequested`. The actions handler passes on the page's own edit, create and menu-reorder requests only while `editorCanEditContent()` |
| SEO results | `dot-uve-seo-results/` | Always placed; shows while a social-media preview is chosen and the og tags are read | How the page looks when shared on the chosen social media | `viewSocialMedia`, `editorOgTags`, `viewOgTagsResults` | None |
| Edit panel | `dot-uve-edit-panel/` | Edit mode | Quick edit and the style editor for the selected contentlet; opens and closes itself | `editorEditPanelOpen`, `editorEditPanelTab`, `editorSelected`, `$editorContentletEditData`, `$styleSchema`, `editorCanEditStyles` | `uveEditContentRequested`, from quick edit |
| Content editor | `dot-uve-content-editor/` | Always, in any mode | Every content editor: the legacy dialog, the new editor in a dialog or the side panel, the VTL source editor and the block editor sidebar; inserts what the user creates or picks, and reloads the page after a save | `pageAsset`, `pageParams`, `editorInsertContentlet`, `pageReload`; sets `editorContentEditorPlaced` while it is placed | None; it listens to every request in the group |

Outside the editor, the shell (`dot-ema-shell/`) sends `uveEditContentRequested` for the page's Properties while a content editor is placed, which it reads from `editorContentEditorPlaced`: the content editor sets it when it is created and clears it when it is destroyed. On the layout, rules and experiments screens, and on a page the user cannot read (the shell then renders no router outlet), no content editor is placed, so the shell opens its own legacy dialog.
