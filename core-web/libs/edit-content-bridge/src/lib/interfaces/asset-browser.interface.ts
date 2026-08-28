/**
 * The browse contract exposed to custom-field VTL templates through
 * `DotCustomFieldApi.openBrowserModal()`.
 *
 * Replaces the previous `BrowserSelector*` shapes, which were named after the legacy
 * `DotBrowserSelectorComponent` and modelled every selection as a contentlet — so a folder or a
 * menu link had to carry a mimetype and a base type it does not have. That was safe to change
 * because the API is unshipped: the templates that call it (`*_new.vtl`) only render when the new
 * Edit Content is enabled, which is not the default.
 */

/**
 * What may be listed, and what was selected.
 *
 * `file` and `dotasset` are kept apart rather than collapsed into one `asset` value because callers
 * genuinely distinguish them — the shipped file-browser template asks for file assets while
 * excluding dotAssets.
 */
export type DotBrowserItemKind = 'file' | 'dotasset' | 'page' | 'folder' | 'link';

/**
 * What a caller asks the browser to show.
 *
 * Every field is optional: the defaults reproduce plain asset browsing, which is what every
 * pre-existing entry point expects.
 */
export interface DotBrowserOptions {
    /** Dialog title. The picker renders its own header, so this travels in the picker config. */
    title?: string;

    /**
     * What may be listed and returned.
     *
     * One list rather than five booleans: `showFiles`/`showPages`/`showFolders`/`showLinks`/
     * `showDotAssets` could express combinations that mean nothing, and could not say "these kinds"
     * without naming every other kind too.
     *
     * @default ['file', 'dotasset']
     */
    kinds?: DotBrowserItemKind[];

    /**
     * Which version state to browse.
     *
     * One value rather than the previous `showWorking` + `showArchived` pair, which encoded three
     * states in two booleans and left one combination meaningless.
     *
     * @default 'working'
     */
    status?: 'live' | 'working' | 'archived';

    /**
     * Folder to start in, in dotCMS path form — `//site/folder/`.
     *
     * Path-based, not the identifier the previous `hostFolderId` used: the browse endpoint takes a
     * path, so an identifier would have to be resolved through `POST /api/v1/folder/byPath`, which
     * is deprecated for removal. Optional because starting anywhere sensible is the common case —
     * neither shipped template passes one.
     */
    path?: string;

    /**
     * Narrow file assets by MIME type.
     *
     * Cannot be combined with `kinds: ['link', …]`: a menu link carries no MIME type, so the browse
     * endpoint drops links whenever this is set. Asking for both logs a warning rather than
     * silently returning fewer kinds than requested.
     */
    mimeTypes?: string[];

    // NOTE: no `extensions` yet. `BrowserQuery` supports it but `/api/v1/drive/search` does not
    // expose it, and it is absent from that endpoint's cursor-based path entirely — so an
    // `extensions` option here would be accepted and then silently ignored, which is exactly the
    // failure this contract is meant to prevent. It ships together with the endpoint support.

    /**
     * Sort field and direction.
     *
     * Replaces `sortByDesc: boolean`, which gave a direction with no field to apply it to and did
     * not match the browse endpoint's own `field:direction` form.
     *
     * @default { field: 'modDate', direction: 'asc' }
     */
    sort?: { field: string; direction: 'asc' | 'desc' };
}

/** What every selection carries, whatever kind it is. */
export interface DotBrowserSelectionBase {
    kind: DotBrowserItemKind;
    identifier: string;
    inode: string;
    title: string;
    /**
     * The value a field stores. **Always non-empty.**
     *
     * The one field every shipped template actually reads, so an empty value is a defect rather
     * than a degraded result: a contentlet's URL, a page's URL, a folder's path, or a link's
     * target.
     */
    url: string;
}

/** A file asset or dotAsset — the only kinds that carry contentlet metadata. */
export interface DotBrowserAssetSelection extends DotBrowserSelectionBase {
    kind: 'file' | 'dotasset';
    /** File name, when the contentlet has one. */
    name?: string;
    mimeType?: string;
    baseType?: string;
    contentType?: string;
}

/** An HTML page. Carries a content type, but no mimetype or file name. */
export interface DotBrowserPageSelection extends DotBrowserSelectionBase {
    kind: 'page';
    baseType?: string;
    contentType?: string;
}

/** A folder. `url` is its path. */
export interface DotBrowserFolderSelection extends DotBrowserSelectionBase {
    kind: 'folder';
}

/** A menu link. `url` is its target. */
export interface DotBrowserLinkSelection extends DotBrowserSelectionBase {
    kind: 'link';
}

/**
 * What the editor picked.
 *
 * A union discriminated by `kind` rather than one flat shape with everything optional: the flat
 * shape let a consumer read a mimetype off a folder and get `undefined` with no indication that the
 * question was meaningless. Branch on `kind` and each variant offers exactly the fields it has.
 */
export type DotBrowserSelection =
    | DotBrowserAssetSelection
    | DotBrowserPageSelection
    | DotBrowserFolderSelection
    | DotBrowserLinkSelection;

/**
 * Handle on an open browse dialog.
 *
 * Returns a promise rather than taking an `onClose` callback, so a template reads as
 * `const selection = await handle.result` instead of nesting. `close()` stays because a caller
 * still needs to dismiss the dialog itself — a promise alone cannot do that.
 */
export interface DotBrowserHandle {
    /**
     * Resolves with the selection, or `null` when the editor cancels.
     *
     * Never rejects on cancellation — cancelling is an ordinary outcome, not an error, and making
     * it a rejection would force every caller into a `try`/`catch` to handle the common case.
     */
    readonly result: Promise<DotBrowserSelection | null>;

    /** Closes the dialog programmatically; `result` resolves `null`. */
    close(): void;
}
