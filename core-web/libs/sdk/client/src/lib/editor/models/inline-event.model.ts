export type INLINE_EDITING_EVENT_KEY = 'BLOCK_EDITOR' | 'WYSIWYG';

export interface InlineEditorData {
    inode: string;
    language: number;
    contentType: string;
    fieldName: string;
    content: Record<string, unknown>;
}

export type InlineEditEventData = InlineEditorData;
