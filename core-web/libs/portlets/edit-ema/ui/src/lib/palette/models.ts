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
    // CONTENT/WIDGET intentionally reuse the base-type string values and are intentionally
    // ABSENT from LIST_TYPE_BASE_TYPES: they take the page-scoped (UVE) fetch branch in the
    // store. Do NOT add them to that map — that would reroute UVE to the global endpoint.
    /** Content types: CONTENT, FILEASSET, DOTASSET — page-scoped (UVE) */
    CONTENT = DotCMSBaseTypesContentTypes.CONTENT,
    /** Widget content types only — page-scoped (UVE) */
    WIDGET = DotCMSBaseTypesContentTypes.WIDGET,
    /** User's favorite content types from all categories */
    FAVORITES = 'FAVORITES',
    // Page-agnostic list types (global fetch). One per Content Drive "New" menu option.
    /** Every base type except FORM */
    ALL_CONTENT_TYPES = 'ALL_CONTENT_TYPES',
    ALL_CONTENT = 'ALL_CONTENT',
    ALL_WIDGET = 'ALL_WIDGET',
    ALL_FILEASSET = 'ALL_FILEASSET',
    ALL_DOTASSET = 'ALL_DOTASSET',
    ALL_PERSONA = 'ALL_PERSONA',
    ALL_VANITY_URL = 'ALL_VANITY_URL',
    ALL_KEY_VALUE = 'ALL_KEY_VALUE',
    ALL_HTMLPAGE = 'ALL_HTMLPAGE'
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
    /**
     * Map of content-type variables allowed on the current page (favorites filtering).
     * Passed in as data by the consumer (UVE) instead of injecting UVEStore, so the
     * palette is reusable outside the editor. Undefined → no favorites filtering.
     */
    allowedContentTypes?: Record<string, true>;
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
 * Filter excludes DotCMSBaseTypesContentTypes FORMs and HTMLPAGEs to keep the Palette UI focused on standard content.
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
 * Every base type except FORM (forms are deprecated). Used by the Content Drive
 * "New" menu's "All Content Types" option.
 */
export const BASE_TYPES_FOR_CONTENT_DRIVE = [
    DotCMSBaseTypesContentTypes.CONTENT,
    DotCMSBaseTypesContentTypes.WIDGET,
    DotCMSBaseTypesContentTypes.FILEASSET,
    DotCMSBaseTypesContentTypes.DOTASSET,
    DotCMSBaseTypesContentTypes.PERSONA,
    DotCMSBaseTypesContentTypes.VANITY_URL,
    DotCMSBaseTypesContentTypes.KEY_VALUE,
    DotCMSBaseTypesContentTypes.HTMLPAGE
];

/**
 * Base-type filter for each page-agnostic list type (Content Drive "New" menu).
 * The store routes any list type present here to the global content-type endpoint.
 */
export const LIST_TYPE_BASE_TYPES: Partial<
    Record<DotUVEPaletteListTypes, DotCMSBaseTypesContentTypes[]>
> = {
    [DotUVEPaletteListTypes.ALL_CONTENT_TYPES]: BASE_TYPES_FOR_CONTENT_DRIVE,
    [DotUVEPaletteListTypes.ALL_CONTENT]: [DotCMSBaseTypesContentTypes.CONTENT],
    [DotUVEPaletteListTypes.ALL_WIDGET]: [DotCMSBaseTypesContentTypes.WIDGET],
    [DotUVEPaletteListTypes.ALL_FILEASSET]: [DotCMSBaseTypesContentTypes.FILEASSET],
    [DotUVEPaletteListTypes.ALL_DOTASSET]: [DotCMSBaseTypesContentTypes.DOTASSET],
    [DotUVEPaletteListTypes.ALL_PERSONA]: [DotCMSBaseTypesContentTypes.PERSONA],
    [DotUVEPaletteListTypes.ALL_VANITY_URL]: [DotCMSBaseTypesContentTypes.VANITY_URL],
    [DotUVEPaletteListTypes.ALL_KEY_VALUE]: [DotCMSBaseTypesContentTypes.KEY_VALUE],
    [DotUVEPaletteListTypes.ALL_HTMLPAGE]: [DotCMSBaseTypesContentTypes.HTMLPAGE]
};

/**
 * Content type for the palette.
 */
export interface DotCMSPaletteContentType extends DotCMSContentType {
    disabled?: boolean;
}
