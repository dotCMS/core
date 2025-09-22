/**
 * Represents a folder in the DotCMS system
 *
 * @interface DotFolder
 * @property {string} id - Unique identifier for the folder
 * @property {string} hostName - The hostname where the folder resides
 * @property {string} path - The path to the folder in the system
 * @property {boolean} addChildrenAllowed - Whether new child folders can be added to this folder
 */
export interface DotFolder {
    id: string;
    hostName: string;
    path: string;
    addChildrenAllowed: boolean;
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
 */
export interface DotFolderEntity {
    assetPath: string;
    data: {
        title: string;
        showOnMenu?: boolean;
        sortOrder?: number;
        fileMasks?: string[];
        defaultAssetType?: string;
    };
}
