import {
    DEFAULT_VARIANT_ID,
    DotCMSContentlet,
    DotCMSContentType,
    ESContent
} from '@dotcms/dotcms-models';

import { DEFAULT_PER_PAGE, DotPaletteSortOption, DotPaletteListStatus } from './models';

/**
 * Mock array for loading skeleton rows.
 * Used to display placeholder content while data is loading.
 * Generates an array of indices based on the default items per page.
 *
 * @example
 * ```typescript
 * LOADING_ROWS_MOCK.forEach(() => {
 *   // Render skeleton component
 * });
 * ```
 */
export const LOADING_ROWS_MOCK = Array.from({ length: DEFAULT_PER_PAGE }, (_, index) => index);

/**
 * Determines the appropriate CSS class for sort menu items based on current sort state.
 * Returns 'active-menu-item' if the menu item matches the current sort configuration,
 * otherwise returns an empty string.
 *
 * @param itemSort - The sort option of the menu item to check
 * @param currentSort - The current active sort state
 * @returns CSS class string: 'active-menu-item' if active, empty string otherwise
 *
 * @example
 * ```typescript
 * const className = getSortActiveClass(
 *   { orderby: 'name', direction: 'ASC' },
 *   currentSort
 * );
 * // Returns: 'active-menu-item' or ''
 * ```
 */
export function getSortActiveClass(
    itemSort: DotPaletteSortOption,
    currentSort: DotPaletteSortOption
): string {
    const sameOrderby = currentSort.orderby === itemSort.orderby;
    const sameDirection = currentSort.direction === itemSort.direction;
    const isActive = sameOrderby && sameDirection;

    return isActive ? 'active-menu-item' : '';
}

/**
 * Determines the loading state for the palette based on data availability.
 * Returns LOADED if data exists, EMPTY if no data is present.
 *
 * @param elements - Array of content types or contentlets to evaluate
 * @returns The appropriate palette list status based on data presence
 *
 * @example
 * ```typescript
 * const status = getPaletteState(contenttypes);
 * // Returns: DotPaletteListStatus.LOADED or DotPaletteListStatus.EMPTY
 * ```
 */
export function getPaletteState(
    elements: DotCMSContentType[] | DotCMSContentlet[]
): DotPaletteListStatus {
    return elements.length > 0 ? DotPaletteListStatus.LOADED : DotPaletteListStatus.EMPTY;
}

/**
 * Filters content types by search term, sorts them alphabetically by name,
 * and builds a response object with pagination metadata.
 *
 * This function performs three operations:
 * 1. Filters content types by name (case-insensitive) if a filter is provided
 * 2. Sorts the filtered results alphabetically by name
 * 3. Builds pagination metadata for the results
 *
 * @param contentTypes - Array of content types to filter and sort
 * @param filter - Optional search term to filter content types by name (case-insensitive)
 * @returns Object containing filtered/sorted content types and pagination metadata
 *
 * @example
 * ```typescript
 * const result = filterAndBuildFavoriteResponse(allContentTypes, 'blog');
 * // Returns: {
 * //   contenttypes: [filtered and sorted array],
 * //   pagination: { currentPage: 1, perPage: 5, totalEntries: 5 }
 * // }
 * ```
 */
export function filterAndBuildFavoriteResponse(
    contentTypes: DotCMSContentType[],
    filter = ''
): {
    contenttypes: DotCMSContentType[];
    pagination: { currentPage: number; perPage: number; totalEntries: number };
} {
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

/**
 * Transforms an Elasticsearch content response into a normalized format
 * with pagination metadata.
 *
 * Extracts contentlets from the ES response and calculates pagination
 * information based on the provided offset and default page size.
 *
 * @param response - Elasticsearch content response object
 * @param offset - Current offset position in the result set
 * @returns Object containing contentlets array and calculated pagination metadata
 *
 * @example
 * ```typescript
 * const result = buildContentletsResponse(esResponse, 30);
 * // Returns: {
 * //   contentlets: [...],
 * //   pagination: { currentPage: 2, perPage: 10, totalEntries: 100 }
 * // }
 * ```
 */
export function buildContentletsResponse(
    response: ESContent,
    offset: number
): {
    contentlets: DotCMSContentlet[];
    pagination: { currentPage: number; perPage: number; totalEntries: number };
} {
    const contentlets = response.jsonObjectView.contentlets;
    const totalEntries = response.resultsSize;
    const currentPage = Math.floor(Number(offset) / DEFAULT_PER_PAGE) + 1;

    return {
        contentlets,
        pagination: { currentPage, perPage: contentlets.length, totalEntries }
    };
}

/**
 * Builds a Lucene query string for fetching contentlets by content type and variant.
 *
 * Constructs a query that:
 * - Filters by content type name
 * - Excludes deleted content
 * - Includes specified variant OR DEFAULT variant (for personalization support)
 *
 * @param contentTypeName - The name of the content type to query
 * @param variantId - The variant ID for personalization (defaults to DEFAULT if not provided)
 * @returns Formatted Lucene query string for Elasticsearch
 *
 * @example
 * ```typescript
 * const query = buildContentletsQuery('Blog', 'christmas-variant');
 * // Returns: '+contentType:Blog +deleted:false +variant:(DEFAULT OR christmas-variant)'
 * ```
 */
export function buildContentletsQuery(contentTypeName: string, variantId: string): string {
    return `+contentType:${contentTypeName} +deleted:false ${variantId ? `+variant:(${DEFAULT_VARIANT_ID} OR ${variantId})` : `+variant:${DEFAULT_VARIANT_ID}`}`;
}

/**
 * Converts search parameters to Elasticsearch content parameters.
 * Calculates offset based on page number and builds the query string.
 *
 * @param searchParams - The search parameters from the store
 * @returns DotESContentParams formatted for the ES content service
 *
 * @example
 * ```typescript
 * const esParams = buildESContentParams({
 *   selectedContentType: 'Blog',
 *   variantId: 'default',
 *   language: 1,
 *   page: 2,
 *   filter: 'news'
 * });
 * // Returns: {
 * //   query: '+contentType:Blog +deleted:false +variant:DEFAULT',
 * //   offset: '30',
 * //   itemsPerPage: 30,
 * //   lang: '1',
 * //   filter: 'news'
 * // }
 * ```
 */
export function buildESContentParams(searchParams: {
    selectedContentType: string;
    variantId: string;
    language: number;
    page: number;
    filter: string;
}): {
    query: string;
    offset: string;
    itemsPerPage: number;
    lang: string;
    filter: string;
} {
    const offset = (searchParams.page - 1) * DEFAULT_PER_PAGE;
    const query = buildContentletsQuery(searchParams.selectedContentType, searchParams.variantId);

    return {
        query,
        offset: String(offset),
        itemsPerPage: DEFAULT_PER_PAGE,
        lang: String(searchParams.language),
        filter: searchParams.filter
    };
}
