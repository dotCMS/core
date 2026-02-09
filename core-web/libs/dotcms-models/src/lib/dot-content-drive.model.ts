import { DotCMSContentlet } from './dot-contentlet.model';

export interface DotContentDriveFolder {
    __icon__: 'folderIcon';
    defaultFileType: string;
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
    permissions: number[];
    showOnMenu: boolean;
    sortOrder: number;
    title: string;
    type: 'folder';
}

// This will extend the DotCMSContentlet with more properties,
// but for now we will just use the DotCMSContentlet until we have folders on the request response
export type DotContentDriveItem = DotCMSContentlet | DotContentDriveFolder;

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
     * Number of results to skip for pagination.
     * @default 0
     */
    offset?: number;

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
}

/**
 * Response from the /api/v1/drive/search endpoint.
 * @interface DotContentDriveSearchResponse
 * @property {number} contentTotalCount - The total number of content items
 * @property {number} folderCount - The total number of folder items
 * @property {number} contentCount - The total number of content items
 * @property {DotContentDriveItem[]} list - The list of content items
 */
export interface DotContentDriveSearchResponse {
    contentTotalCount: number;
    folderCount: number;
    contentCount: number;
    list: DotContentDriveItem[];
}
