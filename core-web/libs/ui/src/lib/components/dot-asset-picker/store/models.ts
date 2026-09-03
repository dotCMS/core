import { ComponentStatus, DotContentDriveBrowseItem, TreeNodeItem } from '@dotcms/dotcms-models';

/**
 * Everything the host hands the picker when it opens.
 *
 * All of it is explicit — nothing is read from the URL or from global navigation state — which is
 * what lets the picker run inside a dialog on top of a screen that owns its own routing.
 *
 * Two shapes matter today (AssetPicker 6/7 builds them):
 * - **File field**: `{ site, languageId, allowedBaseTypes: [DOTASSET, FILEASSET] }` — nothing
 *   pre-selected.
 * - **Image field**: the same, plus `baseTypes: [DOTASSET, FILEASSET]` pre-selected and
 *   `mimeTypes: ['image/*']`.
 */
/**
 * The browse capabilities a host can opt into.
 *
 * Absent, the picker behaves exactly as it always has: assets only, no folders, no links, not
 * archived. That is the point of nesting these rather than flattening six flags onto
 * {@link DotAssetPickerConfig} — a File-field change cannot set `showFolders` by accident, and the
 * opt-in is visible at every call site.
 *
 * Only the `browse` entry point (`openBrowserModal`) sets this today.
 */
export interface DotAssetPickerBrowseOptions {
    /**
     * List folders alongside assets, and allow one to be returned.
     *
     * Folders are still navigated through the sidebar tree; this is about what the *list* may
     * contain and return.
     */
    showFolders?: boolean;

    /**
     * List menu links alongside assets, and allow one to be returned.
     *
     * Unsatisfiable together with {@link DotAssetPickerConfig.mimeTypes}: the browse endpoint drops
     * links whenever a mimetype filter is set, because a link has no file metadata.
     */
    showLinks?: boolean;

    /**
     * Which versions to browse, as the endpoint understands it.
     *
     * Three states, and the omitted one is not the restrictive one: **omit** and the endpoint's own
     * default applies, which *includes* working versions; **`true`** is the same thing said
     * explicitly; **`false`** is the only value that narrows to live-only (the request then carries
     * `live: true`).
     */
    showWorking?: boolean;

    /** Include archived content. Omit to exclude it, which is the picker's standing behaviour. */
    showArchived?: boolean;

    /** Sort descending rather than ascending. */
    sortByDesc?: boolean;

    /**
     * Field to sort by, e.g. `modDate` or `title`.
     *
     * Seeds the picker's initial sort alongside {@link sortByDesc}; omit for the picker's default.
     * Both are a starting point, not a lock — the editor can re-sort from the table header.
     */
    sortField?: string;
}

export interface DotAssetPickerConfig {
    /**
     * Site the picker was opened from — the editor's current site.
     *
     * **Optional, and only a starting point.** {@link browseSite} (the remembered location) wins
     * over it, and the sidebar can move the picker elsewhere afterwards. Omit it and the picker
     * resolves the current site itself; pass it when the caller already has one in hand, to save
     * the request.
     *
     * Narrowed to id + hostname because that is all the picker ever reads — a caller holding those
     * two strings does not need to fetch a whole `DotSite` to open a dialog.
     */
    site?: DotAssetPickerSite;

    /**
     * Site to open on, when it differs from {@link site} — the remembered last-used site.
     *
     * Separate from `site` because the two answer different questions: which site the editor is on,
     * and which site the picker should land on. Collapsing them would make an upload with nothing
     * selected land on whichever site the user last browsed.
     */
    browseSite?: DotAssetPickerSite;

    /**
     * Dialog title, already translated ("Add File" / "Add Image").
     *
     * Travels in `DynamicDialogConfig.data` rather than `DynamicDialogConfig.header` because the
     * picker renders its own header — PrimeNG's chrome has nowhere to put the full-screen button.
     */
    title?: string;

    /**
     * Folder to open on. Omit to browse the site root.
     * AssetPicker 6/7 feeds this from the globally remembered last-used path.
     */
    path?: string;

    /** Locale pre-selected from the contentlet's language. */
    languageId?: string;

    /**
     * Base types that start **selected**, by name (`DOTASSET`, `FILEASSET`, …). Seeds the
     * base-type filter, so it also narrows the first search.
     *
     * Not the same thing as {@link allowedBaseTypes}: this is a starting point the editor can
     * clear, not a boundary.
     */
    baseTypes?: string[];

    /**
     * Base types the content-type selector may **offer**, by name. Omit for no restriction.
     *
     * Separate from {@link baseTypes} because the two questions are genuinely different: a File
     * field starts with nothing selected but must still never offer Widget or Content. Deriving
     * one from the other is what made the File field's selector list every base type.
     */
    allowedBaseTypes?: string[];

    /**
     * Mimetype restriction applied to every request — e.g. `['image/*']` for an Image field.
     *
     * Deliberately NOT part of {@link DotAssetPickerFilters}: it must never surface as a chip or be
     * clearable, because an Image field that could return a PDF is broken. Keeping it out of the
     * filter bag makes that structural rather than a convention a future component could break.
     */
    mimeTypes?: string[];

    /**
     * Browse capabilities beyond plain asset picking — folders, menu links, version state and
     * sorting.
     *
     * Omit for today's behaviour. See {@link DotAssetPickerBrowseOptions}.
     */
    browse?: DotAssetPickerBrowseOptions;
}

/**
 * The filters the editor can see and change, by their known keys.
 *
 * Base types are held by **name**, unlike Content Drive, which encodes them as numbers so they
 * survive a round-trip through the URL. The picker has no URL, so it skips that encoding entirely.
 */
export interface DotKnownAssetPickerFilters {
    /** Free-text search term. */
    title: string;
    /** Selected language ids. */
    languageId: string[];
    /** Selected content-type variables. */
    contentType: string[];
    /** Selected base-type names. */
    baseType: string[];
    /**
     * Whether assets shared across every site (SYSTEM_HOST content) are included.
     *
     * `'true'` / `'false'` rather than a boolean, so it reads identically to Content Drive's, whose
     * values have to survive a URL. Written explicitly on both transitions — an absent key means
     * state that predates the toggle, not a deliberate opt-out.
     */
    sharedAssets: string;
    /** Selected content conditions — `ARCHIVED`, `UNPUBLISHED`, `LOCKED`. OR-combined. */
    status: string[];
}

/**
 * The filter bag.
 *
 * Known keys stay named so they keep their types and their autocompletion; the index signature
 * admits the ones that cannot be enumerated — the `us_<variable>` keys the "More" overflow mints
 * per content-type field. Same shape Content Drive uses for the same reason
 * (`DotContentDriveFilters`), which is what lets one shared chip write to either surface.
 *
 * Note what is **not** here: `mimeTypes` and `allowedBaseTypes` are caller restrictions, not
 * filters, and keeping them out of this type is what makes that structural rather than a
 * convention. See {@link DotAssetPickerConfig.mimeTypes}.
 */
export type DotAssetPickerFilters = Partial<DotKnownAssetPickerFilters> & {
    [key: string]: string | string[];
};

/** Sort order for the asset list. */
export type DotAssetPickerSortOrder = 'asc' | 'desc';

export interface DotAssetPickerSort {
    field: string;
    order: DotAssetPickerSortOrder;
}

export interface DotAssetPickerPagination {
    /** Page size. */
    limit: number;
    /** 1-based page number. */
    page: number;
}

/**
 * Cursor bookmark for one visited page.
 *
 * The drive API pages by cursor, not offset, so reaching page N means having walked pages 1..N-1.
 * Contentlets, folders and menu links each page independently, so a bookmark has to record all
 * three cursors — one cursor cannot reconstruct a page of a mixed list.
 *
 * The folder and link cursors stay at 0 while their stream is switched off, which is the case for
 * every entry point that does not opt into {@link DotAssetPickerBrowseOptions}.
 */
export interface DotAssetPickerPage {
    contentCursor: number;
    hasMoreContent: boolean;
    folderCursor: number;
    hasMoreFolders: boolean;
    linkCursor: number;
    hasMoreLinks: boolean;
}

/** Site the list is currently scoped to. */
export interface DotAssetPickerSite {
    identifier: string;
    hostname: string;
}

/**
 * A request that failed, in the only terms the host needs to report it.
 *
 * Deliberately not an `HttpErrorResponse`: the store's job is to say *what* could not be loaded, and
 * the host's job is to decide how to say it. Kept as an object rather than a bare key so two
 * identical failures in a row are still two distinct values, and the host re-reports the second.
 */
export interface DotAssetPickerRequestError {
    /** Translation key for the toast summary. */
    messageKey: string;
}

export interface DotAssetPickerState {
    /** `null` until the host calls `initPicker`. No search is issued before that. */
    config: DotAssetPickerConfig | null;
    /**
     * Last request failure, or `null` before anything has failed.
     *
     * The picker reports errors through its own toast rather than `DotHttpErrorManagerService`: that
     * service transitively needs `Router` and `DotEventsSocket`, which the legacy Dojo binary-field
     * host does not have, and injecting it made the whole dialog fail to construct there.
     */
    requestError: DotAssetPickerRequestError | null;
    /**
     * Site currently being browsed. Starts as `config.site` and changes as the user picks another
     * root in the sidebar — the picker is not pinned to the site that opened it.
     *
     * `undefined` until `initPicker` runs, which is what keeps the store from searching before it
     * has been configured.
     */
    browsingSite: DotAssetPickerSite | undefined;
    /** Folder currently being browsed. `undefined` means the site root. */
    path: string | undefined;
    filters: DotAssetPickerFilters;
    /**
     * Whether the dialog is expanded to fill the viewport.
     *
     * Only the flag lives here — resizing the host `.p-dialog` is the shell component's job, same
     * split as the image editor's `withView`.
     */
    isFullscreen: boolean;
}

export interface DotAssetPickerBrowseState {
    items: DotContentDriveBrowseItem[];
    status: ComponentStatus;
    pagination: DotAssetPickerPagination;
    sort: DotAssetPickerSort;
    /** Cursor bookmarks for pages visited so far, index 0 = page 1. */
    pages: DotAssetPickerPage[];
    /** Total assets matching the current query, for the paginator. */
    totalItems: number;
}

export interface DotAssetPickerFolderTreeState {
    /**
     * Tree roots. Exactly one: the browsed site's root, rendered as `All`.
     *
     * Sites used to be the roots, which is how the editor changed site — by expanding a different
     * one. The site is now chosen explicitly in the sidebar's own selector, so the tree shows one
     * site's folders and nothing else.
     */
    folders: TreeNodeItem[];
    /** Highlighted node — the `All` root or a folder. `null` until the tree resolves. */
    selectedNode: TreeNodeItem | null;
    foldersStatus: ComponentStatus;
    /**
     * Sidebar "Search folders..." term. Narrows the *folders* of the browsed site — never the
     * sites, which have their own search inside the selector, and never the assets, which are
     * `filters.title`.
     */
    folderSearch: string;
    /**
     * Flat, recursive matches for `folderSearch`, replacing the tree while a term is active.
     *
     * `null` and `[]` mean different things and both are load-bearing: `null` is "not searching",
     * `[]` is "searched, nothing matched". Collapsing them fires the empty state before the editor
     * has typed anything.
     */
    searchResults: TreeNodeItem[] | null;
    /**
     * Status of the *search* request, deliberately separate from `foldersStatus`.
     *
     * A failed search and a failed tree load render differently; one shared field makes a failure
     * indistinguishable from an empty result, which is the bug the old sidebar had.
     */
    searchStatus: ComponentStatus;
    /**
     * Whether more matches exist than the one page shown.
     *
     * Only the response's pagination knows this — nothing in `searchResults` records it. It drives
     * a "narrow your search" hint rather than a "load more": the results come from a recursive
     * query, and paging one would silently page the non-recursive query the tree uses.
     */
    searchHasMore: boolean;
}

export interface DotAssetPickerSelectionState {
    /**
     * The single item the editor has picked, or `null`.
     *
     * Widened from `DotCMSContentlet` because the list can now return folders and menu links, which
     * are not contentlets. What follows from that: only a contentlet can be re-fetched with its
     * full content before the picker closes — see `DotAssetPickerComponent.confirm`.
     */
    selectedAsset: DotContentDriveBrowseItem | null;
}
