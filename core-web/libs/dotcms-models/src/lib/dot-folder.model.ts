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
