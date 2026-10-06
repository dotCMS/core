# Contract: `DotUveContentletToolsComponent`

**Feature**: [../spec.md](../spec.md) · **Date**: 2026-09-14

`core-web/libs/portlets/edit-ema/portlet/src/lib/edit-ema-editor/components/dot-uve-contentlet-tools/`

This component is **internal to the edit-ema portlet** — not exported from any `@dotcms/*`
package, not reachable by customer code. There is no REST endpoint, no `postMessage` action, and
no public SDK surface in this feature. What follows is the component's own input/output contract
plus the two behavioral invariants the fix establishes, recorded so `/speckit-tasks` and the
reviewer can check the diff against something concrete.

## Inputs — unchanged by this fix

| Input | Type | Meaning |
|---|---|---|
| `contentletArea` | `ContentletArea \| null` | Bounds + payload of the **hovered** contentlet, from the iframe hover event. |
| `allowContentDelete` | `boolean` (default `true`) | Disables the delete action (e.g. on personalization). |
| `showStyleEditorOption` | `boolean` (default `false`) | Feature flag for the palette button. |

Selected-contentlet state is **not** an input — it is read from `UVEStore.editorSelected` via
the injected store.

## Outputs — unchanged by this fix

| Output | Payload | Notes |
|---|---|---|
| `editVTL` | `VTLFile` | Emitted when a file in the `</>` menu is clicked. **The subject of this fix.** |
| `openQuickEdit` | `void` | Bolt. Promotes hover → selected first. |
| `openFullEditor` | `ActionPayload` | Pencil. Deliberately stateless. |
| `deleteContent` | `ActionPayload` | `×`. |
| `addContent` | `{ type: 'content' \| 'form' \| 'widget'; payload: ActionPayload }` | Add menu. |
| `selectContent` | `ActionPayload` | Palette / drag handle. Promotes hover → selected first. |

**No output signature changes.** `editVTL` still emits a `VTLFile`; only *which* contentlet's
files can reach it changes.

## Invariant 1 — button and menu read the same contentlet

```
hasVtlFiles()   reads contentContext()   // hover  — :213, unchanged
vtlMenuItems()  reads contentContext()   // hover  — :331, CHANGED (was selected-preferred)
```

- **Holds**: hovering A while B is selected lists **A's** files. (AC-005)
- **Violated today**: the button is shown from A, the menu filled from B.
- **Regression guard**: any future edit that makes either read `this.selected()` reintroduces
  #37499. AC-005 is the spec that catches it.

## Invariant 2 — `vtlMenuItems()` is always an array

```ts
readonly vtlMenuItems = computed<MenuItem[]>(() => { ... ?? [] });
```

- **Holds**: returns `[]` when the contentlet has no VTL files. (AC-004)
- **Violated today**: returns `undefined`, so PrimeNG's `[model]` is undefined and the popup
  renders empty instead of the button being absent.
- **Downstream**: `actionsMenuItems()` (`:351`) guards with `vtlSubmenu?.length`, which `[]`
  satisfies identically. No consumer treats `undefined` as a distinct signal — verified.

## Invariant 3 — `</>` does not change the selection

Decided 2026-09-14 (PR #37519 review). The hover toolbar splits on **what the action opens**:

| Button | HTML line | Opens | Calls `promoteHoverToSelected()` |
|---|---|---|---|
| `</>` VTL | :68 | dialog | **no** |
| bolt — Quick Edit | :82 | side panel | yes |
| palette — Style editor | :91 | side panel | yes |
| pencil — full editor | :100 | dialog | **no** |
| `×` — delete | :108 | confirm dialog | **no** |

Side-panel actions promote because their panels bind to `editorSelected` and have no other input.
Dialog actions take the hovered payload directly and leave the selection alone. `</>` reaches the
same dialog the pencil does (`handleEditVTL()` → `openContentForEdit()`), so it stays with the
pencil.

This invariant is **not newly introduced** — it is current behavior, made observable by the fix
and now recorded as a decision. No test is added for it; it is a non-goal, not an AC.

## Out of contract (explicitly)

- **`getDotCMSContentletsBound()`** in `libs/sdk/uve` — its payload shape is frozen. Per ADR-0019
  the `postMessage` protocol is a versioned compatibility surface; editor-only fields do not go
  there.
- **`applyBoundsForSelection()`** in `withSelectionAnchor.ts` — untouched. Still rebuilds the
  selected payload lossily. Any future consumer reading `vtlFiles`, `baseType`,
  `onNumberOfPages` or `dotStyleProperties` off `editorSelected.payload` must repopulate it.
- **`menuItems()`** (add-content, `:308`) — keeps its selected-preferred dual-context pattern.
  It never reads `vtlFiles`; preferring the selection is intended there.
