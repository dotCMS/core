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
