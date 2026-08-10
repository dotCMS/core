import {
    ComponentStatus,
    DotCMSContentlet,
    DotContentDriveItem,
    DotSite,
    TreeNodeItem
} from '@dotcms/dotcms-models';

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
export interface DotAssetPickerConfig {
    /**
     * Site the picker was opened from — the editor's current site.
     *
     * Also the upload fallback when no folder is selected. `SYSTEM_HOST` is not browsable and
     * suppresses the search.
     */
    site: DotSite;

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
}

/**
 * The filters the editor can see and change.
 *
 * Base types are held by **name**, unlike Content Drive, which encodes them as numbers so they
 * survive a round-trip through the URL. The picker has no URL, so it skips that encoding entirely.
 */
export interface DotAssetPickerFilters {
    /** Free-text search term. */
    title?: string;
    /** Selected language ids. */
    languageId?: string[];
    /** Selected content-type variables. */
    contentType?: string[];
    /** Selected base-type names. */
    baseType?: string[];
}

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
 * Only the content cursor is tracked: the picker always sends `showFolders: false`, so the folder
 * cursor never advances.
 */
export interface DotAssetPickerPage {
    contentCursor: number;
    hasMoreContent: boolean;
}

/** Site the list is currently scoped to. */
export interface DotAssetPickerSite {
    identifier: string;
    hostname: string;
}

export interface DotAssetPickerState {
    /** `null` until the host calls `initPicker`. No search is issued before that. */
    config: DotAssetPickerConfig | null;
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
    items: DotContentDriveItem[];
    status: ComponentStatus;
    pagination: DotAssetPickerPagination;
    sort: DotAssetPickerSort;
    /** Cursor bookmarks for pages visited so far, index 0 = page 1. */
    pages: DotAssetPickerPage[];
    /** Total assets matching the current query, for the paginator. */
    totalItems: number;
}

export interface DotAssetPickerFolderTreeState {
    /** Tree roots: one node per site the user can browse, each expandable into its folders. */
    folders: TreeNodeItem[];
    /** Highlighted node — a site root or a folder. `null` until the tree resolves. */
    selectedNode: TreeNodeItem | null;
    foldersStatus: ComponentStatus;
    /**
     * Sidebar "Search sites & folders" term. Separate from `filters.title`, which searches assets:
     * this one narrows what the *tree* shows.
     */
    treeSearch: string;
}

export interface DotAssetPickerSelectionState {
    /** The single asset the editor has picked, or `null`. */
    selectedAsset: DotCMSContentlet | null;
}
