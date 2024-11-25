import { INPUT_TYPE } from '../../models/dot-edit-content-file.model';

type Actions = {
    allowExistingFile: boolean;
    allowURLImport: boolean;
    allowCreateFile: boolean;
    allowGenerateImg: boolean;
    acceptedFiles: string[];
    maxFileSize: number | null;
};

type ConfigActions = Record<INPUT_TYPE, Actions>;

export const INPUT_CONFIG: ConfigActions = {
    File: {
        allowExistingFile: true,
        allowURLImport: true,
        allowCreateFile: true,
        allowGenerateImg: false,
        acceptedFiles: [],
        maxFileSize: null
    },
    Image: {
        allowExistingFile: true,
        allowURLImport: true,
        allowCreateFile: false,
        allowGenerateImg: true,
        acceptedFiles: ['image/*'],
        maxFileSize: null
    },
    Binary: {
        allowExistingFile: false,
        allowURLImport: true,
        allowCreateFile: true,
        allowGenerateImg: true,
        acceptedFiles: [],
        maxFileSize: null
    }
};

export const DEFAULT_CONTENT_TYPE = 'asset';

export const CONTENT_TYPES = {
    DotAsset: 'asset',
    FileAsset: 'fileAsset'
};
