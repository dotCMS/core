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
 * - **File field**: `{ site, languageId }` — no type restriction at all.
 * - **Image field**: `{ site, languageId, baseTypes: [DOTASSET, FILEASSET], mimeTypes: ['image/*'] }`.
 */
export interface DotAssetPickerConfig {
    /** Site to browse. `SYSTEM_HOST` is not browsable and suppresses the search. */
    site: DotSite;

    /**
     * Folder to open on. Omit to browse the site root.
     * AssetPicker 6/7 feeds this from the globally remembered last-used path.
     */
    path?: string;

    /** Locale pre-selected from the contentlet's language. */
    languageId?: string;

    /**
     * Base types the editor may pick from, by name (`DOTASSET`, `FILEASSET`, …).
     * Seeds the base-type filter and narrows the content-type selector.
     */
    baseTypes?: string[];

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

export interface DotAssetPickerState {
    /** `null` until the host calls `initPicker`. No search is issued before that. */
    config: DotAssetPickerConfig | null;
    /** Folder currently being browsed. `undefined` means the site root. */
    path: string | undefined;
    filters: DotAssetPickerFilters;
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
    folders: TreeNodeItem[];
    selectedNode: TreeNodeItem;
    foldersStatus: ComponentStatus;
}

export interface DotAssetPickerSelectionState {
    /** The single asset the editor has picked, or `null`. */
    selectedAsset: DotCMSContentlet | null;
}
