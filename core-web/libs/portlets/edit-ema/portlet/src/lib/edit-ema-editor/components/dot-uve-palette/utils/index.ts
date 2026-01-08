import { MenuItem } from 'primeng/api';

import {
    DEFAULT_VARIANT_ID,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    ESContent
} from '@dotcms/dotcms-models';

import {
    DEFAULT_PER_PAGE,
    DotCMSContentTypePalette,
    DotPaletteSortOption,
    DotPaletteListStatus,
    DotPaletteViewMode,
    DotUVEPaletteListTypes
} from '../models';

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
 * Empty pagination object used for error states.
 * Sets all pagination values to 0 when data cannot be fetched.
 */
export const EMPTY_PAGINATION = {
    currentPage: 1,
    perPage: DEFAULT_PER_PAGE,
    totalEntries: 0
};

/**
 * Empty response object for content types.
 * Used when an error occurs during content types fetch.
 */
export const EMPTY_CONTENTTYPE_RESPONSE = {
    contenttypes: [] as DotCMSContentTypePalette[],
    pagination: EMPTY_PAGINATION
};

/**
 * Empty response object for contentlets.
 * Used when an error occurs during contentlets fetch.
 */
export const EMPTY_CONTENTLET_RESPONSE = {
    contentlets: [] as DotCMSContentlet[],
    pagination: EMPTY_PAGINATION,
    status: DotPaletteListStatus.EMPTY
};

export const DEFAULT_SORT_OPTIONS: DotPaletteSortOption = {
    orderby: 'name',
    direction: 'ASC'
};

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

export function buildPaletteMenuItems({
    viewMode,
    currentSort,
    onSortSelect,
    onViewSelect
}: {
    viewMode: DotPaletteViewMode;
    currentSort: DotPaletteSortOption;
    onSortSelect: (sortOption: DotPaletteSortOption) => void;
    onViewSelect: (viewMode: DotPaletteViewMode) => void;
}): MenuItem[] {
    return [
        {
            label: 'uve.palette.menu.sort.title',
            items: [
                {
                    label: 'uve.palette.menu.sort.option.popular',
                    command: () => onSortSelect({ orderby: 'usage', direction: 'ASC' }),
                    styleClass: getSortActiveClass(
                        { orderby: 'usage', direction: 'ASC' },
                        currentSort
                    )
                },
                {
                    label: 'uve.palette.menu.sort.option.a-to-z',
                    command: () => onSortSelect({ orderby: 'name', direction: 'ASC' }),
                    styleClass: getSortActiveClass(
                        { orderby: 'name', direction: 'ASC' },
                        currentSort
                    )
                },
                {
                    label: 'uve.palette.menu.sort.option.z-to-a',
                    command: () => onSortSelect({ orderby: 'name', direction: 'DESC' }),
                    styleClass: getSortActiveClass(
                        { orderby: 'name', direction: 'DESC' },
                        currentSort
                    )
                }
            ]
        },
        {
            label: 'uve.palette.menu.view.title',
            items: [
                {
                    label: 'uve.palette.menu.view.option.grid',
                    command: () => onViewSelect('grid'),
                    styleClass: viewMode === 'grid' ? 'active-menu-item' : ''
                },
                {
                    label: 'uve.palette.menu.view.option.list',
                    command: () => onViewSelect('list'),
                    styleClass: viewMode === 'list' ? 'active-menu-item' : ''
                }
            ]
        }
    ];
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

function sortContentTypesByName<T extends Pick<DotCMSContentType, 'name'>>(contentTypes: T[]): T[] {
    return contentTypes.sort((a, b) => a.name.localeCompare(b.name));
}

function isAllowedFavoriteContentType({
    contentType,
    allowedContentTypes
}: {
    contentType: DotCMSContentType;
    allowedContentTypes?: Record<string, true>;
}): boolean {
    if (!allowedContentTypes || Object.keys(allowedContentTypes).length === 0) {
        // By UX rule: if we don't have an allowed map, everything is disabled
        return false;
    }

    return (
        allowedContentTypes[contentType.variable] === true ||
        contentType.baseType === DotCMSBaseTypesContentTypes.WIDGET
    );
}

function markDisabledFavorites({
    contentTypes,
    allowedContentTypes
}: {
    contentTypes: DotCMSContentTypePalette[];
    allowedContentTypes?: Record<string, true>;
}): DotCMSContentTypePalette[] {
    return contentTypes.map((ct) =>
        isAllowedFavoriteContentType({ contentType: ct, allowedContentTypes })
            ? ct
            : { ...ct, disabled: true }
    );
}

/**
 * Filters content types by search term, sorts them alphabetically by name,
 * applies pagination, and builds a response object with pagination metadata.
 *
 * This function performs four operations:
 * 1. Filters content types by name (case-insensitive) if a filter is provided
 * 2. Sorts the filtered results alphabetically by name
 * 3. Slices the array based on the current page and DEFAULT_PER_PAGE
 * 4. Builds pagination metadata for the results
 *
 * @param params - Parameters object containing:
 *   - contentTypes: Array of content types to filter and sort
 *   - filter: Optional search term to filter content types by name (case-insensitive)
 *   - page: Current page number (1-indexed, defaults to 1)
 * @returns Object containing filtered/sorted/paginated content types and pagination metadata
 *
 * @example
 * ```typescript
 * const result = buildPaletteFavorite({
 *   contentTypes: allContentTypes,
 *   filter: 'blog',
 *   page: 2
 * });
 * // Returns: {
 * //   contenttypes: [paginated array for page 2],
 * //   pagination: { currentPage: 2, perPage: 30, totalEntries: 45 }
 * // }
 * ```
 */
export function buildPaletteFavorite({
    contentTypes,
    filter = '',
    page = 1,
    allowedContentTypes
}: {
    contentTypes: DotCMSContentTypePalette[];
    filter?: string;
    page?: number;
    allowedContentTypes?: Record<string, true>;
}): {
    contenttypes: DotCMSContentTypePalette[];
    pagination: { currentPage: number; perPage: number; totalEntries: number };
    status: DotPaletteListStatus;
} {
    // Filter + sort (A→Z) to keep pagination stable
    const filteredContentTypes = sortContentTypesByName(
        contentTypes.filter((ct) => !filter || ct.name.toLowerCase().includes(filter.toLowerCase()))
    );

    const totalEntries = filteredContentTypes.length;
    const startIndex = (page - 1) * DEFAULT_PER_PAGE;
    const endIndex = page * DEFAULT_PER_PAGE;
    const pageContentTypes = filteredContentTypes.slice(startIndex, endIndex);

    // Favorites are stored locally, so we need to mark which are not allowed on this page.
    // `allowedContentTypes` comes from containerStructures[*].contentTypeVar and matches contentType.variable.
    const contenttypes = markDisabledFavorites({
        contentTypes: pageContentTypes,
        allowedContentTypes
    });

    const pagination = {
        currentPage: page,
        perPage: DEFAULT_PER_PAGE,
        totalEntries
    };

    return { contenttypes, pagination, status: getPaletteState(contenttypes) };
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
export function buildPaletteContent(
    response: ESContent,
    offset: number
): {
    contentlets: DotCMSContentlet[];
    pagination: { currentPage: number; perPage: number; totalEntries: number };
    status: DotPaletteListStatus;
} {
    const contentlets = response.jsonObjectView.contentlets;
    const totalEntries = response.resultsSize;
    const currentPage = Math.floor(Number(offset) / DEFAULT_PER_PAGE) + 1;

    return {
        contentlets,
        pagination: { currentPage, perPage: contentlets.length, totalEntries },
        status: getPaletteState(contentlets)
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

/**
 * Key for storing the layout mode in the local storage.
 */
export const DOT_PALETTE_LAYOUT_MODE_STORAGE_KEY = 'dot-uve-palette-layout-mode';

/**
 * Key for storing the orderby in the local storage.
 */
export const DOT_PALETTE_SORT_OPTIONS_STORAGE_KEY = 'dot-uve-palette-sort-options';

/**
 * Object containing the empty message for the search state.
 * @type {Object}
 */
export const EMPTY_MESSAGE_SEARCH = {
    icon: 'pi pi-search',
    title: 'uve.palette.empty.search.state.title',
    message: 'uve.palette.empty.search.state.message'
};

export const EMPTY_MESSAGE_CONTENTLETS = {
    icon: 'pi pi-folder-open',
    title: 'uve.palette.empty.state.contentlets.title',
    message: 'uve.palette.empty.state.contentlets.message'
};

/**
 * Object containing empty messages for different list types.
 * Each key corresponds to a DotUVEPaletteListTypes enum value.
 * @type {Record<DotUVEPaletteListTypes, { icon: string; title: string; message: string }>}
 */
export const EMPTY_MESSAGES = {
    [DotUVEPaletteListTypes.CONTENT]: {
        icon: 'pi pi-folder-open',
        title: 'uve.palette.empty.state.contenttypes.title',
        message: 'uve.palette.empty.state.contenttypes.message'
    },
    [DotUVEPaletteListTypes.FAVORITES]: {
        icon: 'pi pi-plus',
        title: 'uve.palette.empty.state.favorites.title',
        message: 'uve.palette.empty.state.favorites.message'
    },
    [DotUVEPaletteListTypes.WIDGET]: {
        icon: 'pi pi-folder-open',
        title: 'uve.palette.empty.state.widgets.title',
        message: 'uve.palette.empty.state.widgets.message'
    }
};
