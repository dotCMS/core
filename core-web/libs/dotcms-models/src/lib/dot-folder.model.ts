/**
 * Represents a folder in the DotCMS system
 *
 * @interface DotFolder
 * @property {string} id - Unique identifier for the folder
 * @property {string} hostName - The hostname where the folder resides.  For a regular folder this
 *   is the parent site's hostname; for a nested-host entry this is the nested host's own hostname.
 * @property {string} path - The path to the folder in the system.  For a nested-host entry this
 *   is always {@code "/"} (the root of the nested host).
 * @property {boolean} addChildrenAllowed - Whether new child folders can be added to this folder
 * @property {boolean} [isHost] - When {@code true} this entry represents a nested host (sub-site)
 *   rather than a regular folder.  The tree UI renders a host/globe icon for these nodes.
 */
export interface DotFolder {
    id: string;
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
    isHost?: boolean;
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
