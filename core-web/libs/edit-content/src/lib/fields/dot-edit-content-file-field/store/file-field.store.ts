import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { Observable, of, pipe } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { filter, map, switchMap, tap } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSTempFile, DotFileMetadata } from '@dotcms/dotcms-models';

import {
    INPUT_TYPE,
    INPUT_TYPES,
    UPLOAD_TYPE,
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
    uploadType: UPLOAD_TYPE;
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
    uploadType: 'dotasset',
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

/**
 * Resolves the file metadata from a contentlet, supporting FileAsset (`metaData`),
 * dotAsset (`assetMetaData`) and per-field (`{variable}MetaData`) shapes.
 */
const resolveContentletMetadata = (
    contentlet: DotCMSContentlet,
    fieldVariable: string
): DotFileMetadata => {
    const metadata =
        contentlet?.metaData ||
        contentlet?.['assetMetaData'] ||
        contentlet?.[`${fieldVariable}MetaData`];

    return (metadata || {}) as DotFileMetadata;
};

/**
 * Resolves the versioned file URL of a contentlet field, used to fetch the text
 * content of files flagged as `editableAsText`.
 */
const resolveContentletVersion = (
    contentlet: DotCMSContentlet,
    fieldVariable: string
): string | null => {
    return (
        contentlet?.['assetVersion'] ||
        contentlet?.fileAssetVersion ||
        contentlet?.[`${fieldVariable}Version`] ||
        null
    );
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
        const http = inject(HttpClient);

        /**
         * Fetches the text content of an `editableAsText` file so the preview can
         * render it inline. Returns the same file when no content needs loading.
         */
        const hydrateTempContent = (file: DotCMSTempFile): Observable<DotCMSTempFile> => {
            const editableAsText = file?.metadata?.editableAsText ?? false;

            if (!editableAsText || !file?.referenceUrl) {
                return of(file);
            }

            return http
                .get(file.referenceUrl, { responseType: 'text' })
                .pipe(map((content) => ({ ...file, content })));
        };

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
                    // Binary fields upload to the temp endpoint (legacy contract);
                    // File/Image fields create a dotAsset contentlet directly.
                    uploadType: inputType === INPUT_TYPES.Binary ? 'temp' : 'dotasset',
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
             * Syncs the store value with the reactive form without altering preview state.
             * Mirrors the legacy binary field {@link DotBinaryFieldStore.setValue} contract
             * so mount-time store ticks stay aligned with `writeValue`.
             */
            setValue: (value: string) => {
                patchState(store, { value });
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
                                uploadType: store.uploadType(),
                                acceptedFiles: store.acceptedFiles(),
                                maxSize: store.maxFileSize() ? `${store.maxFileSize()}` : null
                            })
                            .pipe(
                                switchMap((uploadedFile) =>
                                    uploadedFile.source === 'temp'
                                        ? hydrateTempContent(uploadedFile.file).pipe(
                                              map(
                                                  (hydrated) =>
                                                      ({
                                                          source: 'temp',
                                                          file: hydrated
                                                      }) as UploadedFile
                                              )
                                          )
                                        : of(uploadedFile)
                                ),
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
             * applyTempFile applies an edited/generated temp file to the preview.
             * Used by the image editor round-trip to swap the current asset.
             * @param tempFile temp file returned by the image editor
             */
            applyTempFile: rxMethod<DotCMSTempFile>(
                pipe(
                    filter((tempFile) => !!tempFile),
                    tap(() => {
                        patchState(store, { fileStatus: 'uploading' });
                    }),
                    switchMap((tempFile) =>
                        hydrateTempContent(tempFile).pipe(
                            tapResponse({
                                next: (file) => {
                                    patchState(store, {
                                        fileStatus: 'preview',
                                        value: file.id,
                                        uploadedFile: { source: 'temp', file }
                                    });
                                },
                                error: () => {
                                    patchState(store, {
                                        fileStatus: 'init',
                                        uiMessage: getUiMessage('SERVER_ERROR')
                                    });
                                }
                            })
                        )
                    )
                )
            ),
            /**
             * setFileFromContentlet hydrates the preview from a saved contentlet.
             * Used by the binary web component, which receives the contentlet
             * imperatively (no reactive form value to drive {@link getAssetData}).
             * @param params contentlet, field variable and the stored value
             */
            setFileFromContentlet: rxMethod<{
                contentlet: DotCMSContentlet;
                fieldVariable: string;
                value: string;
            }>(
                pipe(
                    tap(() => {
                        patchState(store, { fileStatus: 'uploading' });
                    }),
                    switchMap(({ contentlet, fieldVariable, value }) => {
                        const metadata = resolveContentletMetadata(contentlet, fieldVariable);
                        const versionUrl = resolveContentletVersion(contentlet, fieldVariable);
                        const content$ =
                            metadata.editableAsText && versionUrl
                                ? http.get(versionUrl, { responseType: 'text' })
                                : of('');

                        return content$.pipe(
                            tapResponse({
                                next: (content = '') => {
                                    patchState(store, {
                                        fileStatus: 'preview',
                                        value,
                                        uploadedFile: {
                                            source: 'contentlet',
                                            file: {
                                                ...contentlet,
                                                metaData: metadata,
                                                content,
                                                fieldVariable
                                            }
                                        }
                                    });
                                },
                                error: () => {
                                    patchState(store, {
                                        fileStatus: 'preview',
                                        value,
                                        uploadedFile: {
                                            source: 'contentlet',
                                            file: {
                                                ...contentlet,
                                                metaData: metadata,
                                                fieldVariable
                                            }
                                        }
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
