import { CLIENT_ACTIONS } from './client.model';

export type INLINE_EDITING_EVENT_KEY = 'block-editor';

export const INLINE_EDITING_EVENT: Record<INLINE_EDITING_EVENT_KEY, CLIENT_ACTIONS> = {
    'block-editor': CLIENT_ACTIONS.INIT_BLOCK_EDITOR_INLINE_EDITING
};

export interface InlineEditingData {
    inode: string;
    languageId: string | number;
    contentType: string;
    fieldName: string;
    content: string | Record<string, unknown>;
}
