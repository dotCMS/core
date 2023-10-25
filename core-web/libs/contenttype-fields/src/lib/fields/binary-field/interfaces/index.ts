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
    SERVER_ERROR = 'SERVER_ERROR',
    INVALID_FILE = 'INVALID_FILE',
    MAX_FILE_SIZE_EXCEEDED = 'MAX_FILE_SIZE_EXCEEDED'
}

export type UiMessageMap = {
    [key in UI_MESSAGE_KEYS]: UiMessageI;
};

export interface UiMessageI {
    message: string;
    severity: string;
    icon: string;
    args?: string[];
}
