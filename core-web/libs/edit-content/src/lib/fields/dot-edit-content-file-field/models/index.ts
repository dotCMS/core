export type INPUT_TYPES = 'File' | 'Image' | 'Binary';

export type FILE_STATUS = 'init' | 'uploading' | 'preview';

export interface UIMessage {
    message: string;
    severity: 'info' | 'error' | 'warning' | 'success';
    icon: string;
}
