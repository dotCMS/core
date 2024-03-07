import { DotCMSPageEditorConfig } from './models/editor.model';
/**
 *
 * Updates the navigation in the editor.
 * @param {string} pathname - The pathname to update the navigation with.
 * @memberof DotCMSPageEditor
 */
export declare function updateNavigation(pathname: string): void;
/**
 * Sends a ping message to the editor.
 *
 */
export declare function pingEditor(): void;
/**
 * Checks if the code is running inside an editor.
 * @returns {boolean} Returns true if the code is running inside an editor, otherwise false.
 */
export declare function isInsideEditor(): boolean;
/**
 * Initializes the DotCMS page editor.
 *
 * @param conf - Optional configuration for the editor.
 */
export declare function initEditor(config?: DotCMSPageEditorConfig): void;
/**
 * Destroys the editor by removing event listeners and disconnecting observers.
 */
export declare function destroyEditor(): void;
