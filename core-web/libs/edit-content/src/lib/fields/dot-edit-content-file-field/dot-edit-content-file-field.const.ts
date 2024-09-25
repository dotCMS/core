import { INPUT_TYPES } from './models';

type Actions = {
    allowExistingFile: boolean;
    allowURLImport: boolean;
    allowCreateFile: boolean;
    allowGenerateImg: boolean;
};

type ConfigActions = Record<INPUT_TYPES, Actions>;

export const INPUT_CONFIG_ACTIONS: ConfigActions = {
    File: {
        allowExistingFile: true,
        allowURLImport: true,
        allowCreateFile: true,
        allowGenerateImg: false
    },
    Image: {
        allowExistingFile: true,
        allowURLImport: true,
        allowCreateFile: false,
        allowGenerateImg: true
    },
    Binary: {
        allowExistingFile: false,
        allowURLImport: true,
        allowCreateFile: true,
        allowGenerateImg: true
    }
};
