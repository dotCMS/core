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
export type DotPaletteViewMode = 'grid grid-cols-12 gap-4' | 'list';

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
 * Sort configuration for content types in the palette.
 */
export interface DotPaletteSortOption {
    /** Field to sort by */
    orderby: 'name' | 'usage';
    /** Sort direction */
    direction: 'ASC' | 'DESC';
}

/**
 * Search and filter parameters for palette queries.
 * Used by the store to manage search state.
 */
export interface DotPaletteSearchParams {
    /** Site identifier for context-aware filtering */
    host: string;
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
    orderby: 'name' | 'usage';
    /** Direction to sort results */
    direction: 'ASC' | 'DESC';
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
    contenttypes: DotCMSPaletteContentType[];
    /** List of contentlets (when drilling into a content type) */
    contentlets: DotCMSContentlet[];
    /** Pagination information */
    pagination: DotPagination;
    /** Current view mode (content types vs contentlets) */
    currentView: DotUVEPaletteListView;
    /** Loading/data status */
    status: DotPaletteListStatus;
    /** Layout mode (grid or list) */
    layoutMode: DotPaletteViewMode;
}

/**
 * Response structure when fetching content types.
 */
export interface DotContentTypeResponse {
    /** Array of content types */
    contenttypes: DotCMSPaletteContentType[];
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

/**
 * Content type for the palette.
 */
export interface DotCMSPaletteContentType extends DotCMSContentType {
    disabled?: boolean;
}
