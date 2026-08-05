import { DotCMSContentlet } from './dot-contentlet.model';

/**
 * Pagination/sort/filter event shape compatible with PrimeNG's LazyLoadEvent.
 * Defined locally so dotcms-models does not depend on primeng; consumers can pass
 * a real LazyLoadEvent from p-table/p-dataView.
 */
export interface DotContentDriveLazyLoadEvent {
    first?: number;
    last?: number;
    rows?: number;
    sortField?: string;
    sortOrder?: number;
    multiSortMeta?: Array<{ field: string; order: number }>;
    filters?: Record<string, { value?: unknown; matchMode?: string; operator?: string }>;
    globalFilter?: unknown;
    forceUpdate?: () => void;
}

export interface DotContentDriveFolder {
    __icon__: 'folderIcon';
    defaultFileType: string;
    /**
     * Folder upload preference: `DOTASSET`/`FILEASSET` forces every upload to that base type,
     * `null`/`undefined` means "ask each time" (no preference). Backed by #35577.
     */
    defaultBaseType?: string | null;
    description: string;
    extension: 'folder';
    filesMasks: string;
    hasTitleImage: boolean;
    hostId: string;
    iDate: number;
    identifier: string;
    inode: string;
    mimeType: string;
    modDate: number;
    name: string;
    owner: string | null;
    parent: string;
    path: string;
    permissions: PermissionType[];
    showOnMenu: boolean;
    sortOrder: number;
    title: string;
    type: 'folder';
}

export const PERMISSIONS_TYPE = {
    READ: 'READ',
    EDIT: 'EDIT',
    PUBLISH: 'PUBLISH',
    EDIT_PERMISSIONS: 'EDIT_PERMISSIONS',
    CAN_ADD_CHILDREN: 'CAN_ADD_CHILDREN'
} as const;

export type PermissionType = (typeof PERMISSIONS_TYPE)[keyof typeof PERMISSIONS_TYPE];

// This will extend the DotCMSContentlet with more properties,
// but for now we will just use the DotCMSContentlet until we have folders on the request response
export type DotContentDriveItem = DotCMSContentlet | DotContentDriveFolder;

/**
 * Pagination event emitted by the folder list view,
 * extending the lazy-load event shape with a resolved 1-indexed page number.
 */
export type DotContentDrivePaginateEvent = DotContentDriveLazyLoadEvent & { page: number };

/**
 * Interface representing data needed for context menu interactions
 * @interface ContextMenuData
 * @property {Event} event - The DOM event that triggered the context menu
 * @property {DotContentDriveItem} contentlet - The content item associated with the context menu
 */
export interface ContextMenuData {
    event: Event;
    contentlet: DotContentDriveItem;
}

/**
 * Query filters for text-based content filtering.
 * Provides Elasticsearch-powered text search capabilities.
 */
export interface DotContentDriveQueryFilters {
    /**
     * By default, we filter folders. When text is provided but we can always override this.
     * @default true
     */
    filterFolders?: boolean;

    /**
     * Text to search for.
     */
    text: string;
}

/**
 * Request body for the /api/v1/drive/search endpoint.
 *
 * @example
 * // Basic folder browsing
 * {
 *   assetPath: "//demo.dotcms.com/documents/"
 * }
 *
 * @example
 * // Search with filtering and pagination
 * {
 *   assetPath: "//demo.dotcms.com/",
 *   filters: { text: "product review" },
 *   contentTypes: ["Blog", "News"],
 *   sortBy: "title:asc",
 *   offset: 0,
 *   maxResults: 20
 * }
 */
export interface DotContentDriveSearchRequest {
    /**
     * The path to the asset/folder to browse.
     * Supports dotCMS path format: //sitename/folder/subfolder/
     *
     * @example "//demo.dotcms.com/"
     * @example "//demo.dotcms.com/documents/"
     */
    assetPath: string;

    /**
     * Whether to include system host content in the results.
     * @default true
     */
    includeSystemHost?: boolean;

    /**
     * List of language identifiers to include in the search.
     * Supports both language codes (e.g., "en", "es") and language IDs.
     * @default system default language
     */
    language?: string[];

    /**
     * List of specific content type identifiers or variable names to include.
     * Can use either content type IDs or variable names (e.g., "Blog", "News", "webPageContent").
     */
    contentTypes?: string[];

    /**
     * List of base content types to include in the search.
     * Available values: "CONTENT", "FILEASSET", "DOTASSET", "HTMLPAGE", "PERSONA", "FORM"
     */
    baseTypes?: string[];

    /**
     * List of MIME types to filter file assets.
     * @example ["image/jpeg", "image/png", "image/gif"]
     * @example ["application/pdf"]
     */
    mimeTypes?: string[];

    /**
     * Search filters for text-based content filtering.
     * Provides Elasticsearch-powered text search capabilities.
     */
    filters?: DotContentDriveQueryFilters;

    /**
     * Number of content items to skip for pagination.
     * @default 0
     */
    contentCursor?: number;

    /**
     * Number of folder items to skip for pagination.
     * @default 0
     */
    folderCursor?: number;

    /**
     * Maximum number of results to return.
     * @default 2000
     */
    maxResults?: number;

    /**
     * Field and direction for sorting results.
     * Format: "fieldName:direction" where direction is "asc" or "desc".
     * Supported fields: "title", "modDate", "modUser", "sortOrder", "name"
     * @default "modDate"
     * @example "title:asc"
     * @example "modDate:desc"
     */
    sortBy?: string;

    /**
     * Whether to include only live (published) content.
     * @default false
     */
    live?: boolean;

    /**
     * Whether to include archived content in results.
     * @default false
     */
    archived?: boolean;

    /**
     * Whether to show folders in results.
     * @default true
     */
    showFolders?: boolean;

    /**
     * Workflow filter entries. Each entry is one workflow scheme, optionally pinned to a
     * single step (omit `step` to match the whole scheme). Entries combine with OR.
     * @example [{ scheme: "d61a59e1-…", step: "dc3c9cd0-…" }, { scheme: "2a4e1d2e-…" }]
     */
    workflow?: { scheme: string; step?: string }[];

    /**
     * Field-based search criteria, keyed by the content-type field variable. Only offered when a
     * single content type is selected and only for fields flagged User Searchable + System Indexed.
     * The value shape depends on the field type:
     * - text/select/radio → a single string (contains / equals)
     * - multi-select/checkbox → a list of values
     * - date/time/date-and-time → an inclusive `{ from, to }` ISO range
     *
     * @example
     * {
     *   title: "product review",
     *   category: ["news", "press"],
     *   publishDate: { from: "2024-01-01T00:00:00Z", to: "2024-12-31T23:59:59Z" }
     * }
     */
    userSearchable?: Record<string, DotContentDriveUserSearchableValue>;
}

/**
 * Inclusive date range used by date/time field-based search criteria.
 */
export interface DotContentDriveDateRange {
    from: string;
    to: string;
}

/**
 * Value shape for a single {@link DotContentDriveSearchRequest.userSearchable} entry.
 * String for text/select, string[] for multi-select and multi-option checkbox, a boolean for a
 * binary checkbox, and a range for date/time fields.
 */
export type DotContentDriveUserSearchableValue =
    | string
    | string[]
    | boolean
    | DotContentDriveDateRange;

/**
 * Response from the /api/v1/drive/search endpoint.
 * @interface DotContentDriveSearchResponse
 * @property {number} contentTotalCount - The total number of content items
 * @property {number} folderCount - The total number of folder items
 * @property {number} contentCount - The total number of content items
 * @property {DotContentDriveItem[]} list - The list of content items
 */
export interface DotContentDriveSearchResponse {
    folderCount: number;
    contentCount: number;
    list: DotContentDriveItem[];
    hasMoreContent: boolean;
    hasMoreFolders: boolean;
    nextContentCursor: number;
    nextFolderCursor: number;
}
