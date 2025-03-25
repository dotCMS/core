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
