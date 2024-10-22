import { MonacoEditorConstructionOptions } from '@materia-ui/ngx-monaco-editor';
import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { ComponentStatus, DotHttpErrorResponse } from '@dotcms/dotcms-models';

import {
    extractFileExtension,
    getInfoByLang
} from '../../../../dot-edit-content-binary-field/utils/editor';
import { UPLOAD_TYPE, UploadedFile } from '../../../models';
import { DotFileFieldUploadService } from '../../../services/upload-file/upload-file.service';
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
