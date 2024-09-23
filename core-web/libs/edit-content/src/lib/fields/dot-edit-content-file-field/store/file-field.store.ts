import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

import { computed, inject } from '@angular/core';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import { INPUT_CONFIG } from '../dot-edit-content-file-field.const';
import { INPUT_TYPES, FILE_STATUS, UIMessage, PreviewFile } from '../models';

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
    acceptedFiles: string[];
    maxFileSize: number;
    fieldVariable: string;
    previewFile: PreviewFile | null;
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
    uiMessage: null,
    acceptedFiles: [],
    maxFileSize: 0,
    fieldVariable: '',
    previewFile: null
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
    withMethods((store, uploadService = inject(DotUploadService)) => ({
        initLoad: (initState: {
            inputType: FileFieldState['inputType'];
            uiMessage: FileFieldState['uiMessage'];
        }) => {
            const { inputType, uiMessage } = initState;

            const actions = INPUT_CONFIG[inputType] || {};

            patchState(store, {
                inputType,
                uiMessage,
                ...actions
            });
        },
        setValue: (value: string) => {
            patchState(store, { value });
        },
        removeFile: () => {
            patchState(store, {
                contentlet: null,
                tempFile: null,
                value: '',
                fileStatus: 'init'
            });
        },
        setDropZoneState: (state: boolean) => {
            patchState(store, {
                dropZoneActive: state
            });
        },
        setUploading: () => {
            patchState(store, {
                dropZoneActive: false,
                fileStatus: 'uploading'
            });
        },
        handleUploadFile: async (files: FileList) => {
            const file = files[0];

            if (file) {
                patchState(store, {
                    dropZoneActive: false,
                    fileStatus: 'uploading'
                });

                const tempFile = await uploadService.uploadFile({
                    file,
                    maxSize: `${store.maxFileSize()}`
                });
                patchState(store, {
                    tempFile,
                    contentlet: null,
                    fileStatus: 'preview',
                    value: tempFile?.id,
                    previewFile: { source: 'temp', file: tempFile }
                });
            }
        }
    }))
);
