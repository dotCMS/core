# Contract: the opener

**Lives in**: `core-web/libs/edit-content/src/lib/opener/` (new): `dot-edit-content-opener.service.ts` and `inject-edit-content-panel.ts`, both exported from the barrel.

**Builds on**: `DotSidePanelService` and `injectSidePanel` from `@dotcms/ui`; `DotContentletEditUrlService` from `@dotcms/data-access` for the editor decision (extended, see below); `DotActionUrlService` for the legacy create URL; `DotContentletService` for fetching contentlets by inode.

## The editor decision (extension of an existing service)

```ts
// libs/data-access/src/lib/dot-contentlet-edit-url/dot-contentlet-edit-url.service.ts
export type DotContentEditor = 'new' | 'legacy';

export interface DotContentEditorChoice {
    editor: DotContentEditor;
    /** The resolved content type, or null when the lookup failed (then `editor` is 'legacy'). */
    contentType: DotCMSContentType | null;
}

export class DotContentletEditUrlService {
    /**
     * Which editor a content type uses: 'new' when its metadata has CONTENT_EDITOR2_ENABLED,
     * 'legacy' otherwise or when the lookup fails. Cached per variable for the application's life.
     */
    resolveEditor(contentTypeVariableOrId: string): Observable<DotContentEditorChoice>;

    /** Unchanged signature; now implemented on top of resolveEditor. */
    resolveEditUrl(contentlet: DotCMSContentlet): Observable<string>;
}
```

## The service

```ts
export interface DotEditContentOpenOptions extends DotEditContentPanelHandlers {
    /**
     * The opener's injector. The panel resolves its dependencies through it (MessageService for toasts,
     * route-level providers) and closes when it is destroyed. Required.
     */
    injector: Injector;
}

@Injectable({ providedIn: 'root' })
export class DotEditContentOpenerService {
    /**
     * Opens the side panel for a request. Resolves everything server-side first (the editor, and the
     * create URL for a legacy create), so the outlet's spinner covers the wait and a refused request
     * shows the standard error and opens no editor. Returns the panel ref, so the caller can route
     * browser Back through `requestClose()`.
     */
    open(request: DotEditContentRequest, options: DotEditContentOpenOptions): DotSidePanelRef;
}
```

Behaviour of `open()`:

1. Validate the request (edit-request.md). Invalid → standard error, return a ref that is already closed.
2. `mode: 'edit'` without `contentType` → fetch the contentlet by inode, fill `contentType`, `identifier`, `title`, `languageId` from it.
3. `resolveEditor(request.contentType)`.
4. `editor === 'legacy'` and `mode === 'new'` → `DotActionUrlService.getCreateContentletUrl(contentType, languageId)`. A failure → standard error, close the pending panel.
5. Call `DotSidePanelService.open(componentPromise, { heading, inputs: { request, createUrl? }, outputs, injector })`, where `componentPromise` resolves to `loadDotEditContentSidePanel()` or `loadDotLegacyEditorSidePanel()` only after steps 2 to 4 complete. The panel's spinner shows meanwhile.
6. Wire outputs: the five events to the caller's handlers; `openContent` (from either panel, see open-content-from-fields.md) to a nested `open()` with the hosting panel's injector, routing that panel's `saved` to the event's `onSaved`.
7. `closed` is taken from the ref, so it fires even when the content component never loaded.

## The signal helper

```ts
export interface DotEditContentPanelBinding extends DotEditContentPanelHandlers {
    /** Called when the panel closes itself (X, Escape, mask, delete, routed Back). Not called when the request is cleared. */
    closed?: () => void;
}

export interface DotEditContentPanelHandle {
    readonly isOpen: Signal<boolean>;
    /** Routes a close through the content's unsaved-changes guard (for browser Back). */
    requestClose(): void;
}

/**
 * Same shape as `injectSidePanel`: opens when `request` becomes set, updates heading and inputs while
 * open, closes (unguarded) when it is cleared. Call from an injection context.
 */
export function injectEditContentPanel(
    request: Signal<DotEditContentRequest | null | undefined>,
    binding: DotEditContentPanelBinding
): DotEditContentPanelHandle;
```

Behaviour notes carried over from `injectSidePanel`: a request that changes while the panel is open does not reopen it, so a surface that wants a fresh editor for a different inode clears the signal first; the editor panel's `@for` tracks `inode ?? contentType`, so the layout and its store are recreated when the identity changes.

## Who uses which

| Surface | Entry point |
|---|---|
| Content Drive shell | `injectEditContentPanel` (one call replaces two `injectSidePanel` calls) |
| UVE editor | `injectEditContentPanel` on the editor component |
| UVE shell (Properties outside the content route) | `open()` with the shell's injector |
| Query Tool | `injectEditContentPanel` |
| Pages portlet | `open()` from the actions service with the component's injector |
| Publishing Queue dialogs | `open()` with the dialog component's injector |
| Custom-event handler (legacy JSP portlets) | `open()` with the root injector |
| Deep-link component | `open()` with the component's injector |
| `RouterEditContentHost`, `OverlayEditContentHost` | `open()` with the shell's or the panel's injector |
