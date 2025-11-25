import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { filter, switchMap, tap } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import { INPUT_CONFIG } from '../dot-edit-content-file-field.const';
import { INPUT_TYPES, FILE_STATUS, UIMessage, PreviewFile } from '../models';
import { DotFileFieldUploadService } from '../services/upload-file/upload-file.service';
import { getUiMessage } from '../utils/messages';

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
    uiMessage: UIMessage;
    acceptedFiles: string[];
    maxFileSize: number | null;
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
    maxFileSize: null,
    fieldVariable: '',
    previewFile: null
};

export const FileFieldStore = signalStore(
    withState(initialState),
    withComputed(({ fileStatus }) => ({
        isInit: computed(() => {
            const currentStatus = fileStatus();

            return currentStatus === 'init';
        }),
        isPreview: computed(() => {
            const currentStatus = fileStatus();

            return currentStatus === 'preview';
        }),
        isUploading: computed(() => {
            const currentStatus = fileStatus();

            return currentStatus === 'uploading';
        })
    })),
    withMethods((store) => {
        const uploadService = inject(DotFileFieldUploadService);

        return {
            /**
             * initLoad is used to init load
             * @param initState
             */
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
            /**
             * setUIMessage is used to set uiMessage
             * @param uiMessage
             */
            setUIMessage: (uiMessage: UIMessage) => {
                const acceptedFiles = store.acceptedFiles();
                const maxFileSize = store.maxFileSize();

                patchState(store, {
                    uiMessage: {
                        ...uiMessage,
                        args: [`${maxFileSize}`, acceptedFiles.join(', ')]
                    }
                });
            },
            /**
             * removeFile is used to remove file
             * @param
             */
            removeFile: () => {
                patchState(store, {
                    contentlet: null,
                    tempFile: null,
                    value: '',
                    fileStatus: 'init',
                    uiMessage: getUiMessage('DEFAULT')
                });
            },
            /**
             * setDropZoneState is used to set dropZoneActive
             * @param state
             */
            setDropZoneState: (state: boolean) => {
                patchState(store, {
                    dropZoneActive: state
                });
            },
            /**
             * handleUploadFile is used to upload file
             * @param File
             */
            handleUploadFile: rxMethod<File>(
                pipe(
                    tap(() => {
                        patchState(store, {
                            dropZoneActive: false,
                            fileStatus: 'uploading'
                        });
                    }),
                    filter((file) => {
                        const maxFileSize = store.maxFileSize();

                        if (maxFileSize && file.size > maxFileSize) {
                            patchState(store, {
                                fileStatus: 'init',
                                dropZoneActive: true,
                                uiMessage: {
                                    ...getUiMessage('MAX_FILE_SIZE_EXCEEDED'),
                                    args: [`${maxFileSize}`]
                                }
                            });

                            return false;
                        }

                        return true;
                    }),
                    switchMap((file) => {
                        return uploadService.uploadDotAsset(file).pipe(
                            tapResponse({
                                next: (file) => {
                                    patchState(store, {
                                        tempFile: null,
                                        contentlet: file,
                                        fileStatus: 'preview',
                                        value: file.identifier,
                                        previewFile: { source: 'contentlet', file }
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
            ),
            /**
             * getAssetData is used to get asset data
             * @param File
             */
            getAssetData: rxMethod<string>(
                pipe(
                    switchMap((id) => {
                        return uploadService.getContentById(id).pipe(
                            tapResponse({
                                next: (file) => {
                                    patchState(store, {
                                        tempFile: null,
                                        contentlet: file,
                                        fileStatus: 'preview',
                                        value: file.identifier,
                                        previewFile: { source: 'contentlet', file }
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
        };
    })
);
