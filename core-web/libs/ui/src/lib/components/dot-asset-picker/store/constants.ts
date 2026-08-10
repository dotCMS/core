import { DotAssetPickerPage, DotAssetPickerPagination, DotAssetPickerSort } from './models';

export const DEFAULT_ASSET_PICKER_PAGINATION: DotAssetPickerPagination = {
    limit: 20,
    page: 1
};

export const DEFAULT_ASSET_PICKER_SORT: DotAssetPickerSort = {
    field: 'modDate',
    order: 'desc'
};

/** Page 1 always starts at cursor 0. */
export const DEFAULT_ASSET_PICKER_PAGE: DotAssetPickerPage = {
    contentCursor: 0,
    hasMoreContent: true
};

/**
 * Shortest sidebar search term that reaches the folder-name search.
 *
 * Not a UX choice: `GET /api/v1/folder/search` rejects a `name` shorter than two characters, so a
 * single letter is treated as "no search" instead of being sent and failing.
 */
export const MIN_TREE_SEARCH_LENGTH = 2;
