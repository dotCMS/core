import { DotFileMetadata } from '@dotcms/dotcms-models';
import { DropZoneErrorType } from '@dotcms/ui';

export enum BinaryFieldMode {
    DROPZONE = 'DROPZONE',
    URL = 'URL',
    EDITOR = 'EDITOR',
    AI = 'AI'
}

export enum BinaryFieldStatus {
    INIT = 'INIT',
    UPLOADING = 'UPLOADING',
    PREVIEW = 'PREVIEW'
}

export interface DotFilePreview extends DotFileMetadata {
    id: string;
    titleImage: string;
    inode?: string;
    url?: string;
    content?: string;
}

export enum UI_MESSAGE_KEYS {
    DEFAULT = 'DEFAULT',
    SERVER_ERROR = 'SERVER_ERROR'
}

type BINARY_FIELD_MESSAGE_KEY = UI_MESSAGE_KEYS | DropZoneErrorType;

export type UiMessageMap = {
    [key in BINARY_FIELD_MESSAGE_KEY]: UiMessageI;
};

export interface UiMessageI {
    message: string;
    severity: string;
    icon: string;
    args?: string[];
}
