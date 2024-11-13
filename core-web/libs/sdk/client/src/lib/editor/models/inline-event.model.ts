export type INLINE_EDITING_EVENT_KEY = 'block-editor' | 'WYSIWYG';

export interface InlineEditingBlockEditorData {
    inode: string;
    languageId: string | number;
    contentType: string;
    fieldName: string;
    content: Record<string, unknown>;
}

export type InlineEditingEventData = InlineEditingBlockEditorData;
