import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    DotPagination
} from '@dotcms/dotcms-models';

/**
 * Available tab types in the UVE palette.
 * Each type determines which content types are displayed and how they're filtered.
 */
export enum DotUVEPaletteListTypes {
    /** Content types: CONTENT, FILEASSET, DOTASSET */
    CONTENT = DotCMSBaseTypesContentTypes.CONTENT,
    /** Widget content types only */
    WIDGET = DotCMSBaseTypesContentTypes.WIDGET,
    /** User's favorite content types from all categories */
    FAVORITES = 'FAVORITES'
}

/** @deprecated Use DotUVEPaletteListTypes instead */
export const UVE_PALETTE_LIST_TYPES = DotUVEPaletteListTypes;

/**
 * View states for the palette list component.
 * Determines whether showing content types or contentlets (drill-down view).
 */
export enum DotUVEPaletteListView {
    /** Displaying content types in grid/list view */
    CONTENT_TYPES = 'contenttypes',
    /** Displaying contentlets for a selected content type */
    CONTENTLETS = 'contentlets'
}

/**
 * Loading/data status for the palette list.
 */
export enum DotPaletteListStatus {
    /** Data is being fetched from the server */
    LOADING = 'loading',
    /** Data has been successfully loaded */
    LOADED = 'loaded',
    /** No data available (empty result) */
    EMPTY = 'empty'
}

/**
 * Display mode for content types in the palette.
 * - grid: Card-based grid layout
 * - list: Compact list layout
 */
export type DotPaletteViewMode = 'grid' | 'list';

/**
 * Sort field options for content types.
 */
export type DotPaletteSortField = 'name' | 'usage';

/**
 * Sort direction options.
 */
export type DotPaletteSortDirection = 'ASC' | 'DESC';

/**
 * Base query parameters for fetching content types.
 * Used by the content type service for API requests.
 */
export interface DotContentTypeQueryParams {
    /** Language ID for content type filtering (default: 1) */
    language?: number;
    /** Filter content types by name or description */
    filter?: string;
    /** Page number for pagination (default: 1) */
    page?: number;
    /** Items per page - max: 100 (default: 30) */
    per_page?: number;
    /** Sort field - "name" or "usage" (default: "usage") */
    orderby?: DotPaletteSortField;
    /** Sort direction - ASC or DESC (default: "ASC") */
    direction?: DotPaletteSortDirection;
    /** Content type base types to filter by */
    types?: DotCMSBaseTypesContentTypes[];
}

/** @deprecated Use DotContentTypeQueryParams instead */
export type DotContentTypeParams = DotContentTypeQueryParams;

/**
 * Extended query parameters for fetching page-specific content types.
 * Adds page context to the base content type parameters.
 * Used when filtering content types based on page context.
 */
export interface DotPageContentTypeQueryParams extends DotContentTypeQueryParams {
    /** The URL path or identifier of the page to filter content types */
    pagePathOrId: string;
}

/** @deprecated Use DotPageContentTypeQueryParams instead */
export type DotPageContentTypeParams = DotPageContentTypeQueryParams;

/**
 * Parameters for Elasticsearch content queries.
 * Used when fetching contentlets for a specific content type.
 */
export interface DotESContentParams {
    /** Number of items per page */
    itemsPerPage: number;
    /** Language code for the query */
    lang: string;
    /** Search filter text */
    filter: string;
    /** Offset for pagination */
    offset: string;
    /** Lucene query string */
    query: string;
}

/**
 * Generic API response structure for dotCMS endpoints.
 * @template T - Type of the entity being returned
 */
export interface DotCMSAPIResponse<T = unknown> {
    /** The main data payload */
    entity: T;
    /** Array of error messages, if any */
    errors: string[];
    /** Array of informational messages */
    messages: string[];
    /** User permissions for the entity */
    permissions: string[];
    /** Internationalization message map */
    i18nMessagesMap: { [key: string]: string };
    /** Pagination information, if applicable */
    pagination?: DotPagination;
}

/**
 * Sort configuration for content types in the palette.
 */
export interface DotPaletteSortOption {
    /** Field to sort by */
    orderby: DotPaletteSortField;
    /** Sort direction */
    direction: DotPaletteSortDirection;
}

/**
 * Search and filter parameters for palette queries.
 * Used by the store to manage search state.
 */
export interface DotPaletteSearchParams {
    /** Page path or ID for context-aware filtering */
    pagePathOrId: string;
    /** Language ID for content */
    language: number;
    /** Variant ID for personalization */
    variantId: string;
    /** Current palette list type (CONTENT, WIDGET, FAVORITES) */
    listType: DotUVEPaletteListTypes;
    /** Selected content type for drill-down view (empty = content types view) */
    selectedContentType: string;
    /** Field to sort results by */
    orderby: DotPaletteSortField;
    /** Direction to sort results */
    direction: DotPaletteSortDirection;
    /** Current page number for pagination */
    page: number;
    /** Search filter text for content types/contentlets */
    filter: string;
}

/**
 * State interface for the palette list component store.
 * Manages all data and UI state for the palette list.
 */
export interface DotPaletteListState {
    /** Current search and filter parameters */
    searchParams: DotPaletteSearchParams;
    /** List of content types to display */
    contenttypes: DotCMSContentType[];
    /** List of contentlets (when drilling into a content type) */
    contentlets: DotCMSContentlet[];
    /** Pagination information */
    pagination: DotPagination;
    /** Current view mode (content types vs contentlets) */
    currentView: DotUVEPaletteListView;
    /** Loading/data status */
    status: DotPaletteListStatus;
}

/**
 * Response structure when fetching content types.
 */
export interface DotContentTypeResponse {
    /** Array of content types */
    contenttypes: DotCMSContentType[];
    /** Pagination metadata */
    pagination: DotPagination;
}

/**
 * Response structure when fetching contentlets.
 */
export interface DotContentletsResponse {
    /** Array of contentlets */
    contentlets: DotCMSContentlet[];
    /** Pagination metadata */
    pagination: DotPagination;
}

/**
 * Default number of items to display per page in the palette.
 */
export const DEFAULT_PER_PAGE = 30;

/**
 * Base content types included in the Content tab.
 */
export const BASETYPES_FOR_CONTENT = [
    DotCMSBaseTypesContentTypes.CONTENT,
    DotCMSBaseTypesContentTypes.FILEASSET,
    DotCMSBaseTypesContentTypes.DOTASSET
];

/**
 * Base content types included in the Widget tab.
 */
export const BASETYPES_FOR_WIDGET = [DotCMSBaseTypesContentTypes.WIDGET];

/**
 * All base content types that can be added to favorites.
 */
export const BASE_TYPES_FOR_FAVORITES = [...BASETYPES_FOR_CONTENT, ...BASETYPES_FOR_WIDGET];
