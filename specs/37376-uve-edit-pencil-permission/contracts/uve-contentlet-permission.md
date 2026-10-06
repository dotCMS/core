# Contract: per-contentlet edit permission in UVE

**Feature**: `37376-uve-edit-pencil-permission`
**Date**: 2026-09-04 (regenerated after clarification)

Four interfaces. C1 is documented because this feature makes it load-bearing for the first time;
C2–C4 change.

---

## C1 — Server → browser: the DOM attribute *(existing, unchanged)*

**Producer**: `com.dotcms.rendering.velocity.services.ContainerLoader:358`
**Consumers** *(new)*: the `@dotcms/uve` client script, and the editor's two inline-edit guards.

```html
<div class="dotcms-contentlet"
     data-dot-object="contentlet"
     data-dot-identifier="..." data-dot-inode="..." data-dot-type="..."
     data-dot-can-edit="true|false"
     ...>
```

| Property | Value |
|---|---|
| Emitted when | `trackingWrapperEnabled` — EDIT mode, or LIVE with analytics tracking |
| Meaning | Logged-in back-end user holds `PERMISSION_WRITE` (= `PERMISSION_EDIT`, `2`) on this contentlet **instance** |
| Evaluated by | `$contents.doesUserHasPermission(inode, 2, true)` → `ContentsWebAPI:1112` → `permissionAPI.doesUserHavePermission(...)` |
| On error | Server-side fail-closed: an unreadable contentlet yields `"false"` |
| Absent when | Page is rendered headlessly or by an SDK that builds its own wrappers |
| Reachable via | `GET /api/v1/page/render/...` → `page.rendered`. **Not** in `GET /api/v1/page/json/...` |

**Stability**: now depended upon by four gates. Removing or renaming the attribute breaks all of
them *silently* — they would fail open, restoring the defect with no test failing. Guarded by the
SDK unit test in C2.

---

## C2 — SDK: `readContentletDataset()` *(changed — additive)*

**Module**: `core-web/libs/sdk/uve/src/lib/dom/dom.utils.ts:443`
**Published as**: `@dotcms/uve/internal` (the explicitly-internal subpath; see `src/internal.ts`)

```ts
readContentletDataset(element: HTMLElement): {
    identifier?: string; title?: string; inode?: string;
    contentType?: string; baseType?: string; widgetTitle?: string;
    onNumberOfPages?: string;
    canEdit: boolean;          // NEW — true unless the attribute is exactly "false"
    dotStyleProperties?: object;
}
```

| Aspect | Contract |
|---|---|
| Change type | **Additive.** No existing field removed, renamed, or retyped. |
| Default | `true` when the attribute is absent, empty, or any value other than `"false"` |
| Versioning | Per **ADR-0019** the SDK is date-lockstep with the CMS release. **Do not hand-edit `package.json` `version`** — the release pipeline sets it. |
| Compatibility | Older client + newer CMS ignores the attribute; newer client + older CMS sees it absent and fails open. Both degrade to pre-fix behavior, never to a locked editor. |

The value flows unchanged into the two existing `postMessage` payloads — `set-contentlet` (hover,
`internal/events.ts:324`) and `set-selected-contentlet` (click, `:416`). No new message type; no
change to the message protocol.

**Shipped artifact**: `dotCMS/src/main/webapp/ext/uve/dot-uve.js`, rebuilt with
`pnpm nx run sdk-uve:build:js`. The committed bundle is part of this contract — and mandatory here,
since the Block Editor click binding (`src/script/utils.ts:255`) is inside its entry graph.

---

## C3 — Editor components *(changed — internal)*

### C3a — `DotUveContentletToolsComponent`

**Module**: `.../edit-ema-editor/components/dot-uve-contentlet-tools/`

| Member | Change | Contract |
|---|---|---|
| `contentletArea` (input) | unchanged | `payload.contentlet` may now carry `canEdit` |
| `allowContentDelete` (input) | unchanged | Page-level delete rule; **not** repurposed |
| `openFullEditor` (output) | **narrowed** | MUST NOT emit when `canEdit === false` |
| `openQuickEdit` (output) | **narrowed** | MUST NOT emit when `canEdit === false` |
| `canEditContentlet` (computed) | new | `contentContext()?.contentlet?.canEdit !== false` |

No new `input()`. The permission is per-contentlet and already in the payload the component receives.

| Surface | `canEdit === false` | otherwise |
|---|---|---|
| Icon-row pencil (`hover-edit-button`, template :99) | `disabled`, tooltip `uve.contentlet.no.edit.permission` | enabled, `uve.tooltip.edit.full` |
| Icon-row Quick Edit (`hover-quick-edit-button`, :82) | `disabled`, same tooltip | enabled, `uve.tooltip.edit.quick` |
| Overflow-menu "Edit" + "Quick Edit" (`actionsMenuItems`, `.ts:294`) | `disabled: true` | enabled |
| Delete / drag / add / style | **unchanged** — never gated on this permission | unchanged |

Icon row and overflow menu MUST agree: they are two renderings of one action set, and narrow
contentlets show only the menu.

### C3b — `DotUveContentletQuickEditComponent` / quick-edit form

| Aspect | Contract |
|---|---|
| Input data | `$contentletEditData()` MUST preserve `canEdit` from `editorSelected().payload.contentlet` across the page-asset swap (`edit-ema-editor.component.ts:228-256`) |
| Denied rendering | Form is read-only, shows the `uve.contentlet.no.edit.permission` notice, offers no save |
| Trigger | Applies whenever the selection changes while the panel is open — not only when opened via the ⚡ button |

---

## C4 — Inline-editing guards *(changed — behavioral)*

Two independent paths. Both MUST refuse **and** notify.

| Path | Entry | Guard | Notification |
|---|---|---|---|
| Plain / WYSIWYG | `handleInlineEditing(e)` (`edit-ema-editor.component.ts:861`), click on `[data-mode]` | resolve `e.target.closest('[data-dot-object="contentlet"]')`, refuse when `dataset.dotCanEdit === 'false'` | toast, `uve.contentlet.no.edit.permission` |
| Block Editor | SDK binds `[data-block-editor-content]` (`sdk/uve/src/script/utils.ts:255-278`) → `initInlineEditing('BLOCK_EDITOR')` → `INIT_INLINE_EDITING` → `handleInlineEditingEvent` (`dot-uve-actions-handler.service.ts:401`) | resolve the owning contentlet wrapper in the iframe DOM from the payload `inode`, refuse when denied | toast, same key |

**Silent refusal is a contract violation.** Unlike the toolbar buttons there is no control to grey
out and no hover target for a tooltip; a click that does nothing is indistinguishable from a broken
editor. The editor already injects `MessageService` (`edit-ema-editor.component.ts:263`, used by
`triggerCopyToast` at `:1964`); the actions handler needs it injected for the Block Editor path.

---

## Out of contract

- `handleOpenFullEditor()` (`edit-ema-editor.component.ts:1339`) reached from the quick-edit panel's
  own "open full editor" button. Covered indirectly once C3b lands — the panel is read-only for a
  denied contentlet, so the button is not reachable in that state.
- `getDotContentletAttributes()` (`dom.utils.ts:272`), the headless attribute emitter. Unchanged;
  headless pages continue to omit the attribute and fail open.
- The legacy `@dotcms/client` editor script (`html/js/editor-js/sdk-editor.js`), used only behind
  `FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION` (off by default).
- The public SDK `initInlineEditing(...)` API itself. A customer app may call it directly; the
  portlet-side guard in C4 is the choke point, not the SDK function.
