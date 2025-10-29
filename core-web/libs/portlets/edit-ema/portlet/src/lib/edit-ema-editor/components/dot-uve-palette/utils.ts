import { DEFAULT_VARIANT_ID, DotCMSContentlet, DotCMSContentType, ESContent } from '@dotcms/dotcms-models';

import {
    DEFAULT_PER_PAGE,
    DotPaletteSortOption,
    DotPaletteListStatus
} from './models';

/**
 * Mock array for loading skeleton rows.
 * Used to display placeholder content while data is loading.
 */
export const LOADING_ROWS_MOCK = Array.from({ length: DEFAULT_PER_PAGE }, (_, index) => index);

/**
 * Determines the CSS class for sort menu items based on current sort state.
 * Returns 'active-menu-item' if the item matches the current sort configuration.
 *
 * @param itemSort - Sort option to check
 * @param currentSort - Current sort state
 * @returns CSS class string for the menu item
 */
export function isSortActive(itemSort: DotPaletteSortOption, currentSort: DotPaletteSortOption): string {
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
