import {
    DEFAULT_VARIANT_ID,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    ESContent
} from '@dotcms/dotcms-models';

import { SortOption } from './components/dot-uve-palette-list/model';
import {
    DEFAULT_PER_PAGE,
    DotPaletteListStatus
} from './components/dot-uve-palette-list/store/store';

export const BASETYPES_FOR_CONTENT = [
    DotCMSBaseTypesContentTypes.CONTENT,
    DotCMSBaseTypesContentTypes.FILEASSET,
    DotCMSBaseTypesContentTypes.DOTASSET
];
export const BASETYPES_FOR_WIDGET = [DotCMSBaseTypesContentTypes.WIDGET];
export const BASE_TYPES_FOR_FAVORITES = [...BASETYPES_FOR_CONTENT, ...BASETYPES_FOR_WIDGET];
export enum UVE_PALETTE_LIST_TYPES {
    CONTENT = DotCMSBaseTypesContentTypes.CONTENT,
    WIDGET = DotCMSBaseTypesContentTypes.WIDGET,
    FAVORITES = 'FAVORITES'
}

/**
 * Determines the CSS class for sort menu items based on current sort state.
 * Returns 'active-menu-item' if the item matches the current sort configuration.
 *
 * @param orderby - Sort field to check
 * @param direction - Sort direction to check
 * @param currentSort - Current sort state
 * @returns CSS class string for the menu item
 */
export function isSortActive(itemSort: SortOption, currentSort: SortOption): string {
    const sameOrderby = currentSort.orderby === itemSort.orderby;
    const sameDirection = currentSort.direction === itemSort.direction;
    const isActive = sameOrderby && sameDirection;

    return isActive ? 'active-menu-item' : '';
}

export function getPaletteState(elements: DotCMSContentType[] | DotCMSContentlet[]) {
    return elements.length > 0 ? DotPaletteListStatus.LOADED : DotPaletteListStatus.EMPTY;
}

export function buildFavoriteResponse(contentTypes: DotCMSContentType[], filter = '') {
    const contenttypes = contentTypes.filter(
        (ct) => !filter || ct.name.toLowerCase().includes(filter.toLowerCase())
    );
    contenttypes.sort((a, b) => a.name.localeCompare(b.name));

    const pagination = {
        currentPage: 1,
        perPage: contenttypes.length,
        totalEntries: contenttypes.length
    };

    return { contenttypes, pagination };
}

export function buildContentletsResponse(response: ESContent, offset: number) {
    const contentlets = response.jsonObjectView.contentlets;
    const totalEntries = response.resultsSize;
    const currentPage = Math.floor(Number(offset) / DEFAULT_PER_PAGE) + 1;
    return {
        contentlets,
        pagination: { currentPage, perPage: contentlets.length, totalEntries }
    };
}

export function buildContentletsQuery(contentTypeName: string, variantId: string) {
    return `+contentType:${contentTypeName} +deleted:false ${variantId ? `+variant:(${DEFAULT_VARIANT_ID} OR ${variantId})` : `+variant:${DEFAULT_VARIANT_ID}`}`;
}
