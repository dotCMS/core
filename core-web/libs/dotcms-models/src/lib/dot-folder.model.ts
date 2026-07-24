/**
 * Represents a folder in the DotCMS system
 *
 * @interface DotFolder
 * @property {string} id - Unique identifier for the folder
 * @property {string} [inode] - The inode of the folder (used by the legacy content editor to pre-select the target folder)
 * @property {string} hostName - The hostname where the folder resides
 * @property {string} path - The path to the folder in the system
 * @property {boolean} addChildrenAllowed - Whether new child folders can be added to this folder
 * @property {boolean} [hasChildren] - Whether the folder has at least one child folder visible to
 * the current user. Populated by the folder-search endpoint; `undefined` when the source does not
 * report it (e.g. legacy callers), in which case the folder is treated as potentially expandable.
 */
export interface DotFolder {
    id: string;
    inode?: string;
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
    hasChildren?: boolean;
}

/**
 * Represents a folder entity in the DotCMS system with additional metadata
 *
 * @interface DotFolderEntity
 * @property {string} assetPath - The path to the folder asset in the system
 * @property {object} data - Additional folder metadata and configuration
 * @property {string} data.title - The display title of the folder
 * @property {boolean} [data.showOnMenu] - Whether the folder should be shown in navigation menus
 * @property {number} [data.sortOrder] - The sort order position of the folder
 * @property {string[]} [data.fileMasks] - Array of file patterns/masks allowed in this folder
 * @property {string} [data.defaultAssetType] - The default type for new assets created in this folder
 * @property {string} [data.url] - The URL of the folder
 */
export interface DotFolderEntity {
    assetPath: string;
    data: {
        title: string;
        showOnMenu?: boolean;
        sortOrder?: number;
        fileMasks?: string[];
        defaultAssetType?: string;
        name?: string;
    };
}

/**
 * Result item returned by the unified folder search endpoint (`GET /api/v1/folder/search`).
 * Unlike {@link DotFolder}, the folder's own `name` and its parent `path` are exposed
 * separately, and `siteId`/hostname are not included (the search is scoped by `siteId`).
 *
 * @interface FolderSearchView
 * @property {string} id - Unique identifier for the folder
 * @property {string} inode - Inode of the folder
 * @property {string} name - The folder's own name (not the full path)
 * @property {string} path - The path of the parent folder that contains this folder
 * @property {boolean} addChildrenAllowed - Whether new child folders can be added to this folder
 * @property {boolean} hasChildren - Whether the folder has at least one visible child folder for the requesting user
 */
export interface FolderSearchView {
    id: string;
    inode: string;
    name: string;
    path: string;
    addChildrenAllowed: boolean;
    hasChildren: boolean;
}

/**
 * Query parameters accepted by `GET /api/v1/folder/search`.
 *
 * @interface FolderSearchParams
 * @property {string} siteId - Site identifier to scope the search (required)
 * @property {string} [path] - Path scope for the search. Defaults to '/' (site root)
 * @property {boolean} [recursive] - false = direct children of `path` only (default); true = search all descendants
 * @property {string} [name] - Optional case-insensitive partial match on folder name (minimum 2 characters when provided)
 * @property {'name' | 'mod_date'} [orderby] - Column to sort by
 * @property {'ASC' | 'DESC'} [direction] - Sort direction
 * @property {number} [page] - Page number (1-based, default 1)
 * @property {number} [per_page] - Number of results per page (default 40)
 */
export interface FolderSearchParams {
    siteId: string;
    path?: string;
    recursive?: boolean;
    name?: string;
    orderby?: 'name' | 'mod_date';
    direction?: 'ASC' | 'DESC';
    page?: number;
    per_page?: number;
}
