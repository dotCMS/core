import { INPUT_TYPES } from './models';

type Actions = {
    allowExistingFile: boolean;
    allowURLImport: boolean;
    allowCreateFile: boolean;
    allowGenerateImg: boolean;
    acceptedFiles: string[];
    maxFileSize: number;
};

type ConfigActions = Record<INPUT_TYPES, Actions>;

export const INPUT_CONFIG: ConfigActions = {
    File: {
        allowExistingFile: true,
        allowURLImport: true,
        allowCreateFile: true,
        allowGenerateImg: false,
        acceptedFiles: [],
        maxFileSize: 1024
    },
    Image: {
        allowExistingFile: true,
        allowURLImport: true,
        allowCreateFile: false,
        allowGenerateImg: true,
        acceptedFiles: ['image/*'],
        maxFileSize: 0
    },
    Binary: {
        allowExistingFile: false,
        allowURLImport: true,
        allowCreateFile: true,
        allowGenerateImg: true,
        acceptedFiles: [],
        maxFileSize: 0
    }
};
