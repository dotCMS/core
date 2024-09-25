import { tapResponse } from '@ngrx/component-store';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { filter, switchMap, tap } from 'rxjs/operators';

import { DotUploadFileService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import { DotEditContentService } from '../../../services/dot-edit-content.service';
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
        isInitOrPreview: computed(() => {
            const currentStatus = fileStatus();

            return currentStatus === 'init' || currentStatus === 'preview';
        }),
        isUploading: computed(() => {
            const currentStatus = fileStatus();

            return currentStatus === 'uploading';
        })
    })),
    withMethods(
        (
            store,
            fileService = inject(DotUploadFileService),
            contentService = inject(DotEditContentService)
        ) => ({
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
            removeFile: () => {
                patchState(store, {
                    contentlet: null,
                    tempFile: null,
                    value: '',
                    fileStatus: 'init',
                    uiMessage: getUiMessage('DEFAULT')
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
                        return fileService.uploadDotAsset(file).pipe(
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
            getAssetData: rxMethod<string>(
                pipe(
                    switchMap((identifier) => {
                        return contentService.getContentById(identifier).pipe(
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
        })
    )
);
