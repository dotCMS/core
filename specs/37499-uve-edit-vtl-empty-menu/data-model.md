# Phase 1 Data Model: UVE contentlet-tools contexts

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-09-14

No persisted entities. This defect lives entirely in transient client-side signal state, so the
"data model" here is the **shape of the two payloads the toolbar reads** and where each loses
`vtlFiles`. That divergence *is* the bug, which is why it is worth writing down.

## Existing types (unchanged by this fix)

All in `core-web/libs/portlets/edit-ema/portlet/src/lib/shared/models.ts` unless noted.

```ts
interface VTLFile {
    inode: string;   // resolves the file asset to open in the editor dialog
    name: string;    // shown as the menu item label
}

interface ClientData {
    contentlet?: ContentletPayload;
    container: ContainerPayload;
    newContentlet?: ContentletPayload;
    vtlFiles?: VTLFile[];        // <-- optional; that optionality is what the bug exploits
}

interface PositionPayload extends ClientData { position?: 'before' | 'after'; }

interface ActionPayload extends PositionPayload {
    language_id: string;
    pageContainers: PageContainer[];
    pageId: string;
    personaTag?: string;
    newContentletId?: string;
}
```

```ts
// edit-ema-editor/components/ema-page-dropzone/types.ts
interface ContentletArea { x; y; width; height: number; payload: ActionPayload; }

// store/models.ts
interface SelectedContentlet { bounds: {...}; payload: ActionPayload; }
```

**Key observation**: hover and selection are both typed `ActionPayload`. The type system cannot
tell them apart, so nothing prevented one consumer from silently reading the wrong one. `vtlFiles`
being optional means the empty case is indistinguishable from "not populated" at compile time.

## The two contexts the component derives

| Context | Computed | Source signal | Carries `vtlFiles`? |
|---|---|---|---|
| **Hover** | `contentContext()` (`:196`) | `contentletArea` input | **Always.** `events.ts:326-335` calls `findDotCMSVTLData()` unconditionally on every hover. |
| **Selected** | `selectedContentContext()` (`:206`) | `uveStore.editorSelected` | **Only until the first re-anchor.** |

### How the selected context loses `vtlFiles`

| Step | Where | Effect on `vtlFiles` |
|---|---|---|
| User clicks a contentlet | `events.ts:418-430` → `SET_SELECTED_CONTENTLET` | Present — click payload computes it |
| Layout shifts (scroll, resize, image load) | SDK `AUTO_BOUNDS`, debounced trailing edge | — |
| `SET_BOUNDS` arrives | `withSelectionAnchor.ts:103` `applyBoundsForSelection()` | — |
| Payload rebuilt from the bounds snapshot | `getPageSavePayload(parsed)`, `withEditor.ts:437` | **Dropped** |

The bounds payload (`libs/sdk/uve/src/lib/dom/dom.utils.ts:84-110`) emits only
`{ container, contentlet: { identifier, title, inode, contentType, canEdit } }`. It structurally
cannot supply `vtlFiles`, so the rebuild is lossy by construction, not by oversight.

**Also dropped by the same rebuild**: `baseType`, `onNumberOfPages`, `dotStyleProperties`. After
this fix none of the four has a reader on the selected context. Recorded in the spec as a
standing constraint for future consumers.

## Consumers of `vtlFiles` — before and after

| Consumer | Line | Reads today | Reads after fix |
|---|---|---|---|
| `hasVtlFiles()` — gates the `</>` button | :213 | hover | hover (unchanged) |
| `selectedHasVtlFiles()` — **dead**, unreferenced | :233 | selected | **deleted** (AC-010) |
| `vtlMenuItems()` — fills the menu | :331 | selected-preferred | **hover** (AC-001/005) |

After the fix the only reader of `vtlFiles` anywhere in `core-web` is the hover context. That is
precisely why the store carry-forward (AC-009) was withdrawn — it would have preserved a field
with no remaining reader.

## Invariant this fix establishes

> The `</>` button's visibility and its menu's contents are derived from **the same contentlet** —
> the hovered one — and `vtlMenuItems()` is always an array.

AC-005 is the direct test of the first half, AC-004 of the second. A future change that makes
either half read `selected()` again reintroduces #37499.

## State transitions

None. No entity has a lifecycle here; `vtlFiles` is read-only derived data recomputed from the
DOM on every hover event. There is nothing to migrate, repair, or version.
