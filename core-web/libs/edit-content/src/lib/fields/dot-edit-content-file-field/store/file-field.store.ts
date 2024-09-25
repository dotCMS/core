import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import { INPUT_CONFIG } from '../dot-edit-content-file-field.const';
import { INPUT_TYPES, FILE_STATUS, UIMessage, PreviewFile } from '../models';
import { getUiMessage } from '../utils/messages';

export interface FileFieldState {
    contentlet: DotCMSContentlet | null;
    tempFile: DotCMSTempFile | null;
    value: string | File;
    inputType: INPUT_TYPES | null;
    fileStatus: FILE_STATUS;
    dropZoneActive: boolean;
    isEnterprise: boolean;
    isAIPluginInstalled: boolean;
    allowURLImport: boolean;
    allowGenerateImg: boolean;
    allowExistingFile: boolean;
    allowCreateFile: boolean;
    uiMessage: UIMessage;
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
    uiMessage: getUiMessage('DEFAULT'),
    acceptedFiles: [],
    maxFileSize: 0,
    fieldVariable: '',
    previewFile: null
};

export const FileFieldStore = signalStore(
    withState(initialState),
    withComputed(({ fileStatus }) => ({
        isInitOrPreview: computed(() => {
            const currentStatus = fileStatus();

            return currentStatus === 'init' || currentStatus === 'preview';
        }),
        isUploading: computed(() => {
            const currentStatus = fileStatus();

            return currentStatus === 'uploading';
        })
    })),
    withMethods((store, fileService = inject(DotUploadFileService)) => ({
        initLoad: (initState: {
            inputType: FileFieldState['inputType'];
            fieldVariable: FileFieldState['fieldVariable'];
        }) => {
            const { inputType, fieldVariable } = initState;

            const actions = INPUT_CONFIG[inputType] || {};

            patchState(store, {
                inputType,
                fieldVariable,
                ...actions
            });
        },
        setValue: (value: string) => {
            patchState(store, { value });
        },
        setUIMessage: (uiMessage: UIMessage) => {
            const acceptedFiles = store.acceptedFiles();
            const maxFileSize = store.maxFileSize();

            patchState(store, {
                uiMessage: {
                    ...uiMessage,
                    args: [acceptedFiles.join(', '), `${maxFileSize}`]
                }
            });
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
        handleUploadFile: rxMethod<File>(
            pipe(
                tap(() =>
                    patchState(store, {
                        dropZoneActive: false,
                        fileStatus: 'uploading'
                    })
                ),
                switchMap((file) => {
                    return fileService.uploadDotAsset(file).pipe(
                        tapResponse({
                            next: (file) => {
                                patchState(store, {
                                    tempFile: null,
                                    contentlet: file,
                                    fileStatus: 'preview',
                                    value: file.identifier,
                                    previewFile: { source: 'contentlet', file: file }
                                });
                            },
                            error: () => {
                                patchState(store, {
                                    fileStatus: 'init',
                                    uiMessage: getUiMessage('SERVER_ERROR')
                                });
                            }
                        })
                    );
                })
            )
        )
    }))
);
