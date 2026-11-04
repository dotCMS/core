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

1. Validate the request (edit-request.md). An edit names an `inode`, or an identifier-shaped `identifier` (it reaches a Lucene query). Invalid → standard error, return a ref that is already closed.
2. `mode: 'edit'` without `inode` → find the working version by `identifier`: `+identifier:<id> +working:true +languageId:<lang>` through `DotContentSearchService`, then without the language when the content has none in that one (Content Drive's two-step lookup, moved here); nothing found → standard error (404), close the panel. The version found is what opens: its inode and its language fill the request, whatever language was asked for. `mode: 'edit'` with `inode` but without `contentType`, without `title`, or with the `url` option and without `identifier` → fetch the contentlet by inode, fill `contentType`, `identifier`, `title`, `languageId` from it where the request left them out. (The Publishing Queue dialogs name the inode and the content type, not the title the heading needs or the identifier the URL needs.)
3. `resolveEditor(request.contentType)`.
4. `editor === 'legacy'` and `mode === 'new'` → `DotActionUrlService.getCreateContentletUrl(contentType, languageId)`. A failure → standard error, close the pending panel.
5. Call `DotSidePanelService.open(componentPromise, { heading, inputs: { request, createUrl? }, outputs, injector })`, where `componentPromise` resolves to `loadDotEditContentSidePanel()` or `loadDotLegacyEditorSidePanel()` only after steps 2 to 4 complete. The panel's spinner shows meanwhile.
6. Wire outputs: the five events to the caller's handlers; `openContent` (from either panel, see open-content-from-fields.md) to a nested `open()` with the hosting panel's injector, which the panel puts on the event (`DotOpenContentEvent.injector`), so the stacked panel closes when its host does; routing that panel's `saved` to the event's `onSaved`.
7. `closed` is taken from the ref, so it fires even when the content component never loaded.
8. With the `url` option, register the panel with `DotPanelUrlStateService`: before step 2 for an edit that names its `identifier`, in the language it asks for, so a lookup that finds nothing closes a panel the URL names and that close takes its params out (and step 2's version renames it in place when it is in another language); after step 2 for an edit whose identifier only the fetch tells (identifier and language known, before the editor is chosen); at once for a create. In place when the option says `inPlace`. Route `languageChanged`, the first `saved` of a create and the first `saved` of a new translation to it as `update` calls; the service forgets the panel on the ref's `closed` itself (next section).

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

/** Most surfaces pass it empty: `url: {}`. It names no route: the params go beside the surface's own. */
export interface DotEditContentUrlOption {
    /**
     * Written beside the panel params and cleared with them. No surface passes any yet: Content
     * Drive's folder for a create is its own `path` param, already in the URL. Not passed on to the
     * panels stacked from inside this one.
     */
    extraParams?: Record<string, string>;
    /**
     * Writes the panel into the current history entry instead of adding one, for a panel that takes
     * the place of one the surface has just closed (Content Drive's reopen in the legacy editor), so
     * Back still leaves it in one step. Not passed on to stacked panels. Remove with the legacy editor.
     */
    inPlace?: boolean;
}

// DOT_PANEL_URL_PARAM, DotPanelUrlIntent and readPanelUrlIntent live in @dotcms/utils
// (libs/utils/src/lib/panel-url/read-panel-url-intent.ts) and are re-exported from @dotcms/edit-content.
// The admin app loads @dotcms/edit-content on demand and may not import it statically, so its screens
// read their URL from @dotcms/utils without loading the editor library.

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
export interface DotPanelUrlRegisterOptions {
    /** Rewrites the current history entry instead of adding one. Remove with the legacy editor. */
    inPlace?: boolean;
}

export class DotPanelUrlStateService {
    /**
     * Pushes one history entry with the panel's params (or rewrites the current one with `inPlace`),
     * records the surface's path, and forgets the panel on its `closed`. Called by the opener once
     * identifier and language are known, and by a surface for a panel the opener does not open
     * (Content Drive's Edit Code, from `injectSidePanel`'s `opened` hook).
     */
    register(
        ref: DotSidePanelRef,
        params: Record<string, string>,
        options?: DotPanelUrlRegisterOptions
    ): void;
    /** Rewrites the registered panel's params in place (language switch, a create's or a new translation's first save). */
    update(ref: DotSidePanelRef, params: Record<string, string>): void;
    /** Removes every panel param, and `extraKeys`, in place. For a surface whose URL names more than one thing on load (Content Drive passes `createFolder`, `editFolder` and `folderPermissions`), or a param it does not open (Content Drive's `editCode`). */
    clear(extraKeys?: readonly string[]): void;
}
```

Behaviour: register → `Location.go` (`replaceState` with `inPlace`), unless the URL already names the panel (a link opened on load): then nothing is written, since a second entry for the same URL would make Back do nothing. The path of the URL, without its query string, is recorded with the panel. `languageChanged` → `update` with the new `editContentLang`, `replaceState`. A create's first `saved` → `update` swapping `createContent` for `editContent` + `editContentLang`, `replaceState`; `extraParams` stay with the panel. A `saved` of the content the URL names in another language → `update` with that `editContentLang`: switching to a language the content lacks reports no language, so a new translation's first save is the first the URL hears of it; a `saved` of other content (related content opened in place) changes nothing. `closed` → pop; `replaceState` with the new front panel's params, or with the params removed when the stack is empty, so Forward never reopens a closed panel. The surface's own params are read from `Location.path(true)`, parsed and written back with Angular's `UrlSerializer`, not from the router: the router does not see URLs written with `Location`, such as the Query Tool's search params, so a router-built URL can drop them. The service subscribes to `Location` on its first `register`. Nested opens from `openContent` (step 6) get `url: {}` when the opener had the option, not its `extraParams` or `inPlace`, so a stacked panel pushes its own entry. `injectEditContentPanel` passes the option through its binding, read on every open, so a getter can decide per open.

Back and Forward. A `Location` `popstate` on the path the front panel was registered on, whose URL lacks that panel's params → `replaceState` restoring them, then `ref.requestClose()`; the panel underneath is untouched. The restore waits for the router: dotcms-ui sets `onSameUrlNavigation: 'reload'`, so once the router has navigated it answers every Back with a navigation of its own, even to a URL it holds, and that navigation ends with a `replaceState` of the URL Back landed on. So the service waits for the first `NavigationEnd`, `NavigationCancel`, `NavigationError` or `NavigationSkipped` after the `popstate`, then checks the URL again; it restores at once only when the router has never navigated. A `NavigationCancel` with `code === NavigationCancellationCode.SupersededByNewNavigation` does not end the wait: a second Back pressed before the first navigation ended cancels it while its own navigation still runs, and that navigation's `replaceState` would undo a restore written then. The `hashchange` that hash routing reports after a `popstate` is the same move and is ignored. A `popstate` on another path has left the surface: nothing is restored and nothing is asked to close, the panel goes with the surface's injector, and its close leaves that other route's URL alone (it may name a panel of its own).

A route with `data: { reuseRoute: false }` is rebuilt by the router's navigation on Back, which discards the panel and its prompt. The Query Tool's and the Publishing Queue's routes drop the flag (neither has path params, so the flag only ever mattered there), and so does the `/c/:id/:asset` child, whose Workflow Tasks route keeps a panel in its URL (FR-025a): `DotWorkflowTaskComponent` follows the route's `asset` param and `DotPortletDetailComponent` the parent's `id`, so another task or another portlet on the reused route still shows. `DotCustomReuseStrategyService` is unchanged, because UVE (`/edit-page`) reads its page from the URL once and depends on a query-only navigation rebuilding it. Content Drive's and Pages' routes have no flag.

The legacy (JSP) portlets and Pages read their panel params from the router's `queryParams`, on load and on every navigation of the app's own, not once: a deep link redirects there with a router navigation, after the portlet was built. They skip an emission while the router's `currentNavigation()` has `trigger === 'popstate'`: Back and Forward are the service's, and the router replays an entry that may name a panel still open, which opening again would duplicate. So Forward to an entry that still names a closed panel reopens nothing there either. What is left is deduplicated with `panelUrlIntentKey(intent)` (beside `readPanelUrlIntent` in `@dotcms/utils`), skipped emissions left out, since an app navigation that changes only other params carries the panel params the router last saw. Content Drive, the Query Tool and the Publishing Queue read their URL once.

## The signal helper

```ts
export interface DotEditContentPanelBinding extends DotEditContentPanelHandlers {
    /** Called when the panel closes itself (X, Escape, mask, delete, Back). Not called when the request is cleared. */
    closed?: () => void;
    /** Passed to the opener on every open, read each time, so a getter can decide per open; see `DotEditContentOpenOptions.url`. */
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
| Content Drive shell | `injectEditContentPanel` (one call replaces two `injectSidePanel` calls) | yes, `{}` (its own `path` param carries a create's folder), `{ inPlace: true }` on the reopen in the legacy editor; Edit Code is an `injectSidePanel` whose `opened` hook registers `editCode` with `DotPanelUrlStateService` |
| UVE editor | `injectEditContentPanel` on the editor component | no, the URL names the page |
| UVE shell (Properties outside the content route) | `open()` with the shell's injector | no |
| Query Tool | `injectEditContentPanel` | yes, `{}` |
| Pages portlet | `open()` through `DotPageActionsService.openContent(request, refresh)` with the component's injector, for Edit and for what the URL names on load | yes, `{}` |
| Publishing Queue dialogs | `open()` with the dialog component's injector | yes, `{}` |
| Publishing Queue shell | `open()` with the shell's injector, for what the URL names on load: the panel reopens over the list, not the dialog | yes, `{}` |
| Custom-event handler (legacy JSP portlets) | `open()` through its public `openContent(request)`, with the side panel host's injector, for the portlets' events and for what a portlet's URL names (`IframePortletLegacyComponent`) | yes, `{}` |
| Deep-link component | none: it redirects to the portlet's URL with the panel params | — |
| `RouterEditContentHost`, `OverlayEditContentHost` | `open()` with the shell's or the panel's injector | no |
