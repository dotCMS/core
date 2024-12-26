import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

export enum INPUT_TYPES {
    File = 'File',
    Image = 'Image',
    Binary = 'Binary'
}

export type INPUT_TYPE = `${INPUT_TYPES}`;

export type FILE_STATUS = 'init' | 'uploading' | 'preview';

export type UPLOAD_TYPE = 'temp' | 'dotasset';

export interface UIMessage {
    message: string;
    severity: 'info' | 'error' | 'warning' | 'success';
    icon: string;
    args?: string[];
}

export type MESSAGES_TYPES =
    | 'DEFAULT'
    | 'SERVER_ERROR'
    | 'FILE_TYPE_MISMATCH'
    | 'MAX_FILE_SIZE_EXCEEDED'
    | 'MULTIPLE_FILES_DROPPED';

export type UIMessagesMap = Record<MESSAGES_TYPES, UIMessage>;

export type UploadedFile =
    | {
          source: 'temp';
          file: DotCMSTempFile;
      }
    | {
          source: 'contentlet';
          file: DotCMSContentlet;
      };

export interface DotPreviewResourceLink {
    key: string;
    value: string;
}
