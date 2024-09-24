import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import { INPUT_CONFIG_ACTIONS } from '../dot-edit-content-file-field.const';
import { INPUT_TYPES, FILE_STATUS, UIMessage } from '../models';

export interface FileFieldState {
    contentlet: DotCMSContentlet | null;
    tempFile: DotCMSTempFile | null;
    value: string;
    inputType: INPUT_TYPES | null;
    fileStatus: FILE_STATUS;
    dropZoneActive: boolean;
    isEnterprise: boolean;
    isAIPluginInstalled: boolean;
    allowURLImport: boolean;
    allowGenerateImg: boolean;
    allowExistingFile: boolean;
    allowCreateFile: boolean;
    uiMessage: UIMessage | null;
}

const initialState: FileFieldState = {
    contentlet: null,
    tempFile: null,
    value: '',
    inputType: null,
    fileStatus: 'init',
    dropZoneActive: false,
    isEnterprise: false,
    isAIPluginInstalled: false,
    allowURLImport: false,
    allowGenerateImg: false,
    allowExistingFile: false,
    allowCreateFile: false,
    uiMessage: null
};

export const FileFieldStore = signalStore(
    withState(initialState),
    withComputed(({ fileStatus }) => ({
        isEmpty: computed(() => {
            const currentStatus = fileStatus();

            return currentStatus === 'init' || currentStatus === 'preview';
        }),
        isUploading: computed(() => {
            const currentStatus = fileStatus();

            return currentStatus === 'uploading';
        })
    })),
    withMethods((store) => ({
        initLoad: (initState: {
            inputType: FileFieldState['inputType'];
            uiMessage: FileFieldState['uiMessage'];
        }) => {
            const { inputType, uiMessage } = initState;

            const actions = INPUT_CONFIG_ACTIONS[inputType] || {};

            patchState(store, {
                inputType,
                uiMessage,
                ...actions
            });
        }
    }))
);
