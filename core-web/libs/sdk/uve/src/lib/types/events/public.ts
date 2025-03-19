export type DotCMSInlineEditingType = 'BLOCK_EDITOR' | 'WYSIWYG';

/**
 * Interface representing the data needed for inline editing in DotCMS
 *
 * @interface DotCMSInlineEditorData
 * @property {string} inode - The inode identifier of the content being edited
 * @property {number} language - The language ID of the content
 * @property {string} contentType - The content type identifier
 * @property {string} fieldName - The name of the field being edited
 * @property {Record<string, unknown>} content - The content data as key-value pairs
 */
export interface DotCMSInlineEditingPayload {
    inode: string;
    language: number;
    contentType: string;
    fieldName: string;
    content: Record<string, unknown>;
}

/**
 * Actions received from the dotcms editor
 *
 * @export
 * @enum {number}
 */
export enum DotCMSUveEvent {
    /**
     * Request to page to reload
     */
    UVE_RELOAD_PAGE = 'uve-reload-page',
    /**
     * Request the bounds for the elements
     */
    UVE_REQUEST_BOUNDS = 'uve-request-bounds',
    /**
     * Received pong from the editor
     */
    UVE_EDITOR_PONG = 'uve-editor-pong',
    /**
     * Received scroll event trigger from the editor
     */
    UVE_SCROLL_INSIDE_IFRAME = 'uve-scroll-inside-iframe',
    /**
     * Set the page data
     */
    UVE_SET_PAGE_DATA = 'uve-set-page-data',
    /**
     * Copy contentlet inline editing success
     */
    UVE_COPY_CONTENTLET_INLINE_EDITING_SUCCESS = 'uve-copy-contentlet-inline-editing-success'
}
