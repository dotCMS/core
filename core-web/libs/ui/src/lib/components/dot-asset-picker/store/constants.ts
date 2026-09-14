import { DotAssetPickerPage, DotAssetPickerPagination, DotAssetPickerSort } from './models';

export const DEFAULT_ASSET_PICKER_PAGINATION: DotAssetPickerPagination = {
    limit: 20,
    page: 1
};

export const DEFAULT_ASSET_PICKER_SORT: DotAssetPickerSort = {
    field: 'modDate',
    order: 'desc'
};

/** Page 1 always starts every stream at cursor 0. */
export const DEFAULT_ASSET_PICKER_PAGE: DotAssetPickerPage = {
    contentCursor: 0,
    hasMoreContent: true,
    linkCursor: 0,
    hasMoreLinks: true
};

/**
 * Toast summary keys for the two things that can fail to load.
 *
 * The store records one of these rather than calling `DotHttpErrorManagerService`: that service
 * transitively needs `Router` and `DotEventsSocket`, neither of which exists in the legacy Dojo
 * binary-field host, so injecting it stopped the picker from constructing there at all. The store
 * says *what* failed; the host component decides how to show it.
 */
export const ASSET_PICKER_ERROR_KEYS = {
    assets: 'dot.asset.picker.error.assets',
    folders: 'dot.asset.picker.error.folders'
} as const;

/**
 * Shortest sidebar search term that reaches the folder-name search.
 *
 * Not a UX choice: `GET /api/v1/folder/search` rejects a `name` shorter than two characters, so a
 * single letter is treated as "no search" instead of being sent and failing.
 */
export const MIN_TREE_SEARCH_LENGTH = 2;
