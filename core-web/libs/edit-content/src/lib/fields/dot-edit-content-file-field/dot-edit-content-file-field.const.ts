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
