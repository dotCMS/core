# Contract: fields ask their host to open content

**Lives in**: `core-web/libs/edit-content/src/lib/models/dot-open-content-event.model.ts` (new) and an addition to `EditContentHost` (`libs/edit-content/src/lib/services/host/edit-content-host.model.ts`).

```ts
/** What a field hands its host when it wants other content opened. */
export interface DotOpenContentEvent {
    request: DotEditContentRequest;
    /** Called on every `saved` of the panel the host opens. */
    onSaved?: (contentlet: DotCMSContentlet) => void;
}

export interface EditContentHost {
    // ...existing members unchanged...

    /**
     * A field (Relationship, a block editor) wants other content opened. The host decides whether
     * to open a new panel, stack one, or replace its own content in place with a breadcrumb.
     */
    openContent(event: DotOpenContentEvent): void;
}
```

## Host behaviour

| Host | Edit request from a Relationship field | Create request | Edit request from a block editor |
|---|---|---|---|
| `RouterEditContentHost` (full-screen `#/content/:inode`) | `DotEditContentOpenerService.open` with the shell's injector; `saved` → `onSaved` | same | same |
| `OverlayEditContentHost` (a panel) | `goToRelatedContent(current, target)` in place with a breadcrumb, as today; the field's row refreshes when the author navigates back | `open()` stacked, with the panel's injector; `saved` → `onSaved` | `open()` stacked, with the panel's injector |

The stacked panel is opened with the **hosting panel's** injector, not the field's, so a re-render of the form underneath does not close it (the outlet closes a panel when its opener's injector is destroyed).

## Callers inside `edit-content`

| Caller | Today | After |
|---|---|---|
| `DotRelationshipFieldComponent.openRelated` (`dot-relationship-field.component.ts:305`) | flag + host check → own `DotSidePanelService.open` or `goToRelatedContent` or router | `host.openContent({ request: { mode: 'edit', inode, title }, onSaved: refreshRelatedItem })` |
| `DotRelationshipFieldComponent.showCreateNewContentDialog` (540) | flag → own panel or `DotEditContentDialogComponent`; `onContentSaved` adds the item once, on close | `host.openContent({ request: { mode: 'new', contentType, title, relationship }, onSaved: upsertRelatedItem })`. `onSaved` fires on **every** save of the stacked panel, so it is an upsert: add the row when no listed item has that identifier (`lastChangeSource: 'user'`), otherwise refresh it (`store.refreshItem`, `'load'`). Spec FR-030. Still disabled for legacy-editor types |
| `DotEditContentBlockEditorComponent` (new) | nothing; the editors navigate `window.parent` | binds the editors' edit output → `host.openContent({ request: { mode: 'edit', inode, contentType } })` |

## Block editor libs (no dependency on `edit-content`)

| Lib | Today | After |
|---|---|---|
| `libs/new-block-editor` toolbar (`toolbar.component.ts:552-577`) | emits `contentletEdit`, then confirms, then `writeRelationshipReturnBreadcrumb`, then `window.parent.location.href = url` | emits `contentletEdit` only. `navigateToContentEditor`, the confirm, `breadcrumb.utils.ts` and `ContentletEditUrlService` are deleted |
| `libs/block-editor` bubble menu (`dot-bubble-menu.component.ts:308-377`) | native `confirm`, content type lookup, localStorage breadcrumb, `window.parent.location.href` | emits a new `contentletEdit: output<{ inode; contentType }>()`; everything else in `goToContentlet` is deleted |

## The legacy JSP bridge

`apps/dotcms-block-editor/src/main.ts` registers both editors as custom elements for the legacy editor's iframe (`edit_contentlet.jsp:84-91`). It subscribes to each element's `contentletEdit` and dispatches on `document`:

```ts
document.dispatchEvent(new CustomEvent('ng-event', {
    detail: { name: 'edit-contentlet', data: { inode, contentType } }
}));
```

The legacy editor panel maps that to `openContent` (legacy-screen-panel.md), and the opener stacks a second panel. The relationship JSPs' own "Return to…" breadcrumb (`edit_contentlet_basic_properties.jsp:393-436`) is untouched; it simply never fires for block-editor hops anymore.
