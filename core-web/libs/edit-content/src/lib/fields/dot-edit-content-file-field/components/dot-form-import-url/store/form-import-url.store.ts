import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { ComponentStatus, DotHttpErrorResponse } from '@dotcms/dotcms-models';

import { UPLOAD_TYPE, UploadedFile } from '../../../../../models/dot-edit-content-file.model';
import { DotFileFieldUploadService } from '../../../services/upload-file/upload-file.service';

export interface FormImportUrlState {
    file: UploadedFile | null;
    status: ComponentStatus;
    error: string | null;
    uploadType: UPLOAD_TYPE;
    acceptedFiles: string[];
}

const initialState: FormImportUrlState = {
    file: null,
    status: ComponentStatus.INIT,
    error: null,
    uploadType: 'temp',
    acceptedFiles: []
};

export const FormImportUrlStore = signalStore(
    { protectedState: false }, // TODO: remove when the unit tests are fixed
    withState(initialState),
    withComputed((state) => ({
        isLoading: computed(() => state.status() === ComponentStatus.LOADING),
        isDone: computed(() => state.status() === ComponentStatus.LOADED && state.file),
        allowFiles: computed(() => state.acceptedFiles().join(', '))
    })),
    withMethods((store, uploadService = inject(DotFileFieldUploadService)) => ({
        /**
         * uploadFileByUrl - Uploads a file using its URL.
         * @param {string} fileUrl - The URL of the file to be uploaded.
         */
        uploadFileByUrl: rxMethod<{
            fileUrl: string;
            abortSignal: AbortSignal;
        }>(
            pipe(
                tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                switchMap(({ fileUrl, abortSignal }) => {
                    return uploadService
                        .uploadFile({
                            file: fileUrl,
                            uploadType: store.uploadType(),
                            acceptedFiles: store.acceptedFiles(),
                            maxSize: null,
                            abortSignal: abortSignal
                        })
                        .pipe(
                            tapResponse({
                                next: (file) => {
                                    patchState(store, { file, status: ComponentStatus.LOADED });
                                },
                                error: (error: DotHttpErrorResponse) => {
                                    let errorMessage = error?.message || '';

                                    if (error instanceof Error) {
                                        if (errorMessage === 'Invalid file type') {
                                            errorMessage =
                                                'dot.file.field.import.from.url.error.file.not.supported.message';
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
        ),
        /**
         * Set the upload type (contentlet or temp) for the file.
         * @param uploadType the type of upload to perform
         */
        initSetup: (data: Partial<FormImportUrlState>) => {
            patchState(store, data);
        }
    }))
);
