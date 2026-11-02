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
     * The opener's injector. The panel resolves its dependencies through it, never through where the
     * outlet sits: route-level providers, and PrimeNG MessageService for toasts, so it must sit under
     * a component that provides one and renders a <p-toast /> (the root Injector reaches none). The
     * panel closes when it is destroyed. Required.
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
2. `mode: 'edit'` without `contentType`, without `title`, or with the `url` option and without `identifier` → fetch the contentlet by inode, fill `contentType`, `identifier`, `title`, `languageId` from it where the request left them out. (The Publishing Queue dialogs name the inode and the content type, not the title the heading needs or the identifier the URL needs.)
3. `resolveEditor(request.contentType)`.
4. `editor === 'legacy'` and `mode === 'new'` → `DotActionUrlService.getCreateContentletUrl(contentType, languageId)`. A failure → standard error, close the pending panel.
5. Call `DotSidePanelService.open(componentPromise, { heading, inputs: { request, createUrl? }, outputs, injector })`, where `componentPromise` resolves to `loadDotEditContentSidePanel()` or `loadDotLegacyEditorSidePanel()` only after steps 2 to 4 complete. The panel's spinner shows meanwhile.
6. Wire outputs: the five events to the caller's handlers; `openContent` (from either panel, see open-content-from-fields.md) to a nested `open()` with the hosting panel's injector, which the panel puts on the event (`DotOpenContentEvent.injector`), so the stacked panel closes when its host does; routing that panel's `saved` to the event's `onSaved`.
7. `closed` is taken from the ref, so it fires even when the content component never loaded.
8. With the `url` option, register the panel with `DotPanelUrlStateService` after step 2 (identifier and language known, before the editor is chosen) or at once for a create, and route `languageChanged` and the first `saved` of a create to it as `update` calls; the service forgets the panel on the ref's `closed` itself (next section).

### The panel in the URL (FR-044 to FR-050)

```ts
export interface DotEditContentOpenOptions extends DotEditContentPanelHandlers {
    injector: Injector;
    /**
     * When set, the open panel is written to the current URL and browser Back closes it. Every surface
     * except UVE sets it.
     */
    url?: DotEditContentUrlOption;
}

export interface DotEditContentUrlOption {
    /** The surface's route. Nothing reads it in slice 1; kept for the deep links of slice 3. */
    route: ActivatedRoute;
    /**
     * Written beside the panel params (Content Drive's folder for a create) and cleared with them.
     * Not passed on to the panels stacked from inside this one.
     */
    extraParams?: Record<string, string>;
}

/** The query params that name an open panel, the same on every surface. */
export const DOT_PANEL_URL_PARAM = {
    EDIT_CONTENT: 'editContent', // identifier
    EDIT_CONTENT_LANG: 'editContentLang', // language id
    CREATE_CONTENT: 'createContent', // content type variable or id
    EDIT_CODE: 'editCode' // inode, Edit Source
} as const;

export type DotPanelUrlIntent =
    | { kind: 'none' }
    | { kind: 'edit'; identifier: string; languageId?: number }
    | { kind: 'create'; contentType: string }
    | { kind: 'editCode'; inode: string }
    | { kind: 'conflict' };

/**
 * What a URL asks to open, read once by a surface on init. Accepts only identifier-shaped, content
 * type-shaped and inode-shaped values (anything else reads as absent); more than one panel named is a
 * `conflict`, and the surface opens nothing and clears the params.
 */
export function readPanelUrlIntent(queryParams: Record<string, unknown>): DotPanelUrlIntent;

/**
 * Root service the opener registers every panel opened with the `url` option with. Keeps a stack of
 * open panels and their params; the URL shows the front one merged with the surface's own query
 * string. Read from and written to `Location` only, never through the router, so the surface
 * underneath is not reloaded or recreated.
 */
export class DotPanelUrlStateService {
    /** Pushes one history entry with the panel's params, and forgets the panel on its `closed`. Called by the opener once identifier and language are known. */
    register(ref: DotSidePanelRef, params: Record<string, string>): void;
    /** Rewrites the registered panel's params in place (language switch, create's first save). */
    update(ref: DotSidePanelRef, params: Record<string, string>): void;
    /** Removes every panel param, and `extraKeys`, in place. For a surface whose URL names more than one panel on load. */
    clear(extraKeys?: readonly string[]): void;
}
```

Behaviour: register → `Location.go`, unless the URL already names the panel (a link opened on load): then nothing is written, since a second entry for the same URL would make Back do nothing. `languageChanged` → `update` with the new `editContentLang`, `replaceState`. A create's first `saved` → `update` swapping `createContent` for `editContent` + `editContentLang`, `replaceState` (whether `extraParams` survive that swap is for slice 2 to settle; for now they stay with the panel). `closed` → pop; `replaceState` with the new front panel's params, or with the params removed when the stack is empty, so Forward never reopens a closed panel. A `Location` popstate whose URL lacks the front panel's params → `replaceState` restoring them, then `ref.requestClose()`; the panel underneath is untouched. The surface's own params are read from `Location.path(true)`, parsed and written back with Angular's `UrlSerializer`, not from the router: the router does not see URLs written with `replaceState`, such as the Query Tool's search params, so a router-built URL can drop them. The service subscribes to `Location` on its first `register`. Nested opens from `openContent` (step 6) inherit the option's `route` but not its `extraParams`, so a stacked panel pushes its own entry. `injectEditContentPanel` passes the option through its binding.

Known gap (as before this change, in Content Drive and the Query Tool alike): Angular's router answers a popstate by navigating to the popped URL unless that URL is the one it last navigated to, and such a navigation ends with its own `replaceState`. A surface that writes its own params with `replaceState` (the Query Tool's search, Content Drive's filters) leaves the router behind, so Back from a panel there is followed by a router navigation that overwrites the restored params while the unsaved-changes prompt is open, and on a `reuseRoute: false` portlet may recreate it.

## The signal helper

```ts
export interface DotEditContentPanelBinding extends DotEditContentPanelHandlers {
    /** Called when the panel closes itself (X, Escape, mask, delete, Back). Not called when the request is cleared. */
    closed?: () => void;
    /** Passed to the opener on every open; see `DotEditContentOpenOptions.url`. */
    url?: DotEditContentUrlOption;
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

| Surface | Entry point | `url` option |
|---|---|---|
| Content Drive shell | `injectEditContentPanel` (one call replaces two `injectSidePanel` calls) | yes, with the folder param as `extraParams` for a create; `editCode` for Edit Source |
| UVE editor | `injectEditContentPanel` on the editor component | no, the URL names the page |
| UVE shell (Properties outside the content route) | `open()` with the shell's injector | no |
| Query Tool | `injectEditContentPanel` | yes |
| Pages portlet | `open()` from the actions service with the component's injector | yes |
| Publishing Queue dialogs | `open()` with the dialog component's injector | yes; on load the panel reopens over the list, not the dialog |
| Custom-event handler (legacy JSP portlets) | `open()` with the root injector |
| Deep-link component | `open()` with the component's injector |
| `RouterEditContentHost`, `OverlayEditContentHost` | `open()` with the shell's or the panel's injector |
