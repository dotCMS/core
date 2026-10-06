# Phase 1 Data Model: UVE contentlet-permission gating

**Feature**: `37376-uve-edit-pencil-permission`
**Date**: 2026-09-04 (regenerated after clarification)

No persisted entity is added or changed. The "data model" is one boolean threaded from the rendered
page, through the UVE client SDK, into the Angular editor — and then preserved across the one
transformation that currently drops it.

---

## Entity: contentlet edit permission

Never stored, never sent to a server, never survives a reload. Re-derived from the DOM on every
hover and click.

| Stage | Shape | Representation | Change |
|---|---|---|---|
| Rendered page | `data-dot-can-edit` on `[data-dot-object="contentlet"]` | string `"true"` / `"false"` | **none** — `ContainerLoader.java:358` |
| SDK read | return of `readContentletDataset()` | `canEdit: boolean` | **new field** |
| Editor payload | `ContentletPayload` (`shared/models.ts:86`) | `canEdit?: boolean` | **new optional field** |
| Panel data | return of `$contentletEditData()` | `canEdit` carried through | **preserved across the page-asset swap** |

### Value states

| DOM attribute | `canEdit` | Behavior |
|---|---|---|
| `"false"` | `false` | All four gates deny |
| `"true"` | `true` | Today's behavior |
| absent (headless / SDK-rendered) | `true` | Today's behavior |
| empty or any other string | `true` | Today's behavior — fail open |

### Validation rule

> **Denial requires an explicit `"false"`.** Every other input resolves to allowed.

Expressed as `dataset['dotCanEdit'] !== 'false'`, never `=== 'true'`. Inverting this is the single
most damaging mistake available: headless pages never emit the attribute, so a fail-closed default
disables all four gates on all of them. Pinned by AC-005 and research R6.

### Sentinel: empty container

`contentletForEmptyContainer` (`internal/events.ts:314-322`) stands in for a container with no
content. Not a real contentlet, no permission to check — carries `canEdit: true` explicitly.
Guarded by AC-006.

---

## Derived state

| Name | Where | Derivation |
|---|---|---|
| `canEditContentlet` | `DotUveContentletToolsComponent` | `contentContext()?.contentlet?.canEdit !== false` |
| `editButtonTooltip` | same | allowed → `uve.tooltip.edit.full`; denied → `uve.contentlet.no.edit.permission` |
| `quickEditButtonTooltip` | same | allowed → `uve.tooltip.edit.quick`; denied → `uve.contentlet.no.edit.permission` |
| `canEditSelected` | `DotUveContentletQuickEditComponent` (or its parent) | `$contentletEditData()?.contentlet?.canEdit !== false` |

All are per-contentlet and derived from the payload the components already receive. Deliberately
**not** modelled as component `input()`s like `allowContentDelete`, which is a page-level rule
(persona personalization) supplied by the editor. This value changes with whichever contentlet is
hovered or selected, so it belongs to the payload — a parent-supplied input would duplicate state
and let the two sources disagree.

---

## Guard points

Four places consult the flag. Three of them are *behavioral* guards that must hold even if the
presentation state is bypassed.

| # | Guard | Location | On denial |
|---|---|---|---|
| G1 | Full editor | `handleEditWithCopyDecision` (`edit-ema-editor.component.ts:1509`) | return before the copy-decision modal |
| G2 | Quick Edit open | `handleOpenQuickEdit` (`:1339`) | return before opening the panel |
| G3 | Inline edit — plain/WYSIWYG | `handleInlineEditing` (`:861`) | return + toast |
| G4 | Inline edit — Block Editor | `handleInlineEditingEvent` (`dot-uve-actions-handler.service.ts:401`) | return + toast |

G3 and G4 resolve the permission from the DOM rather than a payload: the clicked element's
`closest('[data-dot-object="contentlet"]')` wrapper. The inline-edit service already uses this exact
lookup pattern (`inline-edit.service.ts` `isInMultiplePages`), so it is an established idiom here.

G1 and G2 read `payload.contentlet.canEdit`. Neither belongs in `openContentForEdit(...)`, which is
also called by `DotEmaShellComponent` for the page's own contentlet via the "Properties" nav action —
a path with no DOM payload and therefore no `canEdit` to consult.

---

## Message catalog

| Key | File | Used by |
|---|---|---|
| `uve.contentlet.no.edit.permission` | `dotCMS/src/main/webapp/WEB-INF/messages/Language.properties` | pencil tooltip, Quick Edit tooltip, read-only panel notice, inline-edit toast |

One key, four placements, by decision — so the copy must read acceptably as a short tooltip *and* as
a panel notice *and* as a toast body. Added next to the existing
`uve.disable.delete.button.on.personalization`, the established precedent for a "why is this
disabled" tooltip.

---

## What is explicitly *not* modelled

- **No Page API field.** `ContainerRaw` maps and `PageViewSerializer` output are untouched, so
  `pageAsset.containers[...].contentlets[...]` gains nothing. Headless stays uncovered by design.
- **No new store state.** Nothing is added to the UVE signal store; the flag rides the existing
  hover/selection payload.
- **No persistence, cache, or serialization.** Nothing here is rollback-relevant.
