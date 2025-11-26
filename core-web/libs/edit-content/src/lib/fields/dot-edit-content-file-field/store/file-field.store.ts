import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { filter, switchMap, tap } from 'rxjs/operators';

import {
    INPUT_TYPE,
    FILE_STATUS,
    UIMessage,
    UploadedFile
} from '../../../models/dot-edit-content-file.model';
import { INPUT_CONFIG } from '../dot-edit-content-file-field.const';
import { DotFileFieldUploadService } from '../services/upload-file/upload-file.service';
import { getUiMessage } from '../utils/messages';

export interface FileFieldState {
    value: string;
    inputType: INPUT_TYPE | null;
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
    uploadedFile: UploadedFile | null;
}

const initialState: FileFieldState = {
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
    uploadedFile: null
};

export const FileFieldStore = signalStore(
    { protectedState: false }, // TODO: remove when the unit tests are fixed
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
                inputType: INPUT_TYPE;
                fieldVariable: FileFieldState['fieldVariable'];
                isAIPluginInstalled?: boolean;
            }) => {
                const { inputType, fieldVariable, isAIPluginInstalled } = initState;

                const actions = INPUT_CONFIG[inputType] || {};

                patchState(store, {
                    inputType,
                    fieldVariable,
                    isAIPluginInstalled,
                    ...actions
                });
            },
            /**
             * Sets the maximum file size allowed for uploads.
             *
             * @param {number} maxFileSize - The maximum file size.
             */
            setMaxSizeFile: (maxFileSize: number) => {
                patchState(store, {
                    maxFileSize
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
                    uploadedFile: null,
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
             * setPreviewFile is used to set previewFile
             * @param file uploaded file
             */
            setPreviewFile: (file: UploadedFile) => {
                patchState(store, {
                    fileStatus: 'preview',
                    uploadedFile: file,
                    value: file.source === 'temp' ? file.file.id : file.file.identifier
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
                        return uploadService
                            .uploadFile({
                                file,
                                uploadType: 'dotasset',
                                acceptedFiles: store.acceptedFiles(),
                                maxSize: store.maxFileSize() ? `${store.maxFileSize()}` : null
                            })
                            .pipe(
                                tapResponse({
                                    next: (uploadedFile) => {
                                        patchState(store, {
                                            fileStatus: 'preview',
                                            value:
                                                uploadedFile.source === 'temp'
                                                    ? uploadedFile.file.id
                                                    : uploadedFile.file.identifier,
                                            uploadedFile
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
                                        fileStatus: 'preview',
                                        value: file.identifier,
                                        uploadedFile: { source: 'contentlet', file }
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
