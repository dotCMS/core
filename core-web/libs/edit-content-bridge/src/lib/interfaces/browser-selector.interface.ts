import { ContentByFolderParams } from '@dotcms/dotcms-models';

/**
 * Result returned when content is selected from the browser selector.
 * This is a unified result that can represent pages, files, or other content types.
 */
export interface BrowserSelectorResult {
    /**
     * The unique identifier of the selected content.
     */
    identifier: string;

    /**
     * The inode of the selected content.
     */
    inode: string;

    /**
     * The title of the selected content.
     */
    title: string;

    /**
     * The name of the selected content (for files).
     */
    name?: string;

    /**
     * The URL of the selected content.
     */
    url: string;

    /**
     * The MIME type of the selected content (for files).
     */
    mimeType?: string;

    /**
     * The base type of the content (e.g., 'CONTENT', 'FILEASSET', 'HTMLPAGE').
     */
    baseType?: string;

    /**
     * The content type variable name.
     */
    contentType?: string;
}

/**
 * Options for configuring the browser selector dialog.
 */
export interface BrowserSelectorOptions {
    /**
     * The title/header of the dialog.
     * @default 'Select Content'
     */
    header: string;

    /**
     * The parameters for the browser selector.
     */
    params: ContentByFolderParams;

    /**
     * Callback function executed when the browser selector is closed.
     * @param result - The selected content result, or null if canceled
     */
    onClose: (result: BrowserSelectorResult | null) => void;
}

/**
 * Controller interface for managing an open browser selector dialog.
 */
export interface BrowserSelectorController {
    /**
     * Closes the browser selector dialog programmatically.
     */
    close(): void;
}
