import { DropZoneErrorType } from '@dotcms/ui';

export enum BinaryFieldMode {
    DROPZONE = 'DROPZONE',
    URL = 'URL',
    EDITOR = 'EDITOR'
}

export enum BinaryFieldStatus {
    INIT = 'INIT',
    UPLOADING = 'UPLOADING',
    PREVIEW = 'PREVIEW'
}

export interface BinaryFile {
    mimeType: string;
    name: string;
    fileSize: number;
    url?: string;
    inode?: string;
    content?: string;
    width?: string;
    height?: string;
    titleImage?: string;
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
