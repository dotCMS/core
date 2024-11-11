import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';
import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { ComponentStatus, DotHttpErrorResponse } from '@dotcms/dotcms-models';

import { UPLOAD_TYPE, UploadedFile } from '../../../../../models/dot-edit-content-file.model';
import { DotFileFieldUploadService } from '../../../services/upload-file/upload-file.service';
import { extractFileExtension, getInfoByLang } from '../../../utils/editor';
import { DEFAULT_MONACO_CONFIG } from '../dot-form-file-editor.conts';

type FileInfo = {
    name: string;
    content: string;
    mimeType: string;
    extension: string;
    language: string;
};

export interface FormFileEditorState {
    file: FileInfo;
    allowFileNameEdit: boolean;
    status: ComponentStatus;
    error: string | null;
    monacoOptions: MonacoEditorConstructionOptions;
    uploadedFile: UploadedFile | null;
    uploadType: UPLOAD_TYPE;
    acceptedFiles: string[];
}

const initialState: FormFileEditorState = {
    file: {
        name: '',
        content: '',
        mimeType: 'plain/text',
        extension: '.txt',
        language: 'text'
    },
    allowFileNameEdit: false,
    status: ComponentStatus.INIT,
    error: null,
    monacoOptions: DEFAULT_MONACO_CONFIG,
    uploadedFile: null,
    uploadType: 'dotasset',
    acceptedFiles: []
};

export const FormFileEditorStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        isUploading: computed(() => state.status() === ComponentStatus.LOADING),
        isDone: computed(() => state.status() === ComponentStatus.LOADED && state.uploadedFile),
        allowFiles: computed(() => state.acceptedFiles().join(', ')),
        monacoConfig: computed<MonacoEditorConstructionOptions>(() => {
            const monacoOptions = state.monacoOptions();
            const { language } = state.file();

            return {
                ...monacoOptions,
                language: language
            };
        })
    })),
    withMethods((store) => {
        const uploadService = inject(DotFileFieldUploadService);

        return {
            /**
             * Sets the file name and updates the file's metadata in the store.
             *
             * @param name - The new name of the file.
             *
             * This method performs the following actions:
             * 1. Extracts the file extension from the provided name.
             * 2. Retrieves file information based on the extracted extension.
             * 3. Updates the store with the new file name and its associated metadata, including MIME type, extension, and language.
             */
            setFileName(name: string) {
                const file = store.file();

                const extension = extractFileExtension(name);
                const info = getInfoByLang(extension);

                patchState(store, {
                    file: {
                        ...file,
                        name,
                        mimeType: info.mimeType,
                        extension: info.extension,
                        language: info.lang
                    }
                });
            },
            /**
             * Initializes the file editor state with the provided options.
             *
             * @param params - The parameters for initializing the file editor.
             * @param params.monacoOptions - Partial options for configuring the Monaco editor.
             * @param params.allowFileNameEdit - Flag indicating if the file name can be edited.
             * @param params.uploadedFile - The uploaded file information, or null if no file is uploaded.
             * @param params.acceptedFiles - Array of accepted file types.
             * @param params.uploadType - The type of upload being performed.
             */
            initLoad({
                monacoOptions,
                allowFileNameEdit,
                uploadedFile,
                acceptedFiles,
                uploadType
            }: {
                monacoOptions: Partial<MonacoEditorConstructionOptions>;
                allowFileNameEdit: boolean;
                uploadedFile: UploadedFile | null;
                acceptedFiles: string[];
                uploadType: UPLOAD_TYPE;
            }) {
                const prevState = store.monacoOptions();

                const state: Partial<FormFileEditorState> = {
                    monacoOptions: {
                        ...prevState,
                        ...monacoOptions
                    },
                    allowFileNameEdit,
                    acceptedFiles,
                    uploadType
                };

                if (uploadedFile) {
                    const { file, source } = uploadedFile;

                    const name = source === 'contentlet' ? file.title : file.fileName;
                    const extension = extractFileExtension(name);
                    const info = getInfoByLang(extension);

                    state.file = {
                        name,
                        content: file.content || '',
                        mimeType: info.mimeType,
                        extension: info.extension,
                        language: info.lang
                    };
                }

                patchState(store, state);
            },
            /**
             * Uploads the file content to the server.
             *
             */
            uploadFile: rxMethod<{
                name: string;
                content: string;
            }>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap(({ name, content }) => {
                        const { mimeType: type } = store.file();
                        const uploadType = store.uploadType();
                        const acceptedFiles = store.acceptedFiles();

                        const file = new File([content], name, { type });

                        return uploadService
                            .uploadFile({
                                file,
                                uploadType,
                                acceptedFiles,
                                maxSize: null
                            })
                            .pipe(
                                tapResponse({
                                    next: (uploadedFile) => {
                                        patchState(store, {
                                            uploadedFile,
                                            status: ComponentStatus.LOADED
                                        });
                                    },
                                    error: (error: DotHttpErrorResponse) => {
                                        let errorMessage = error?.message || '';

                                        if (error instanceof Error) {
                                            if (errorMessage === 'Invalid file type') {
                                                errorMessage =
                                                    'dot.file.field.error.type.file.not.supported.message';
                                            }
                                        }

                                        patchState(store, {
                                            error: errorMessage,
                                            status: ComponentStatus.ERROR
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
