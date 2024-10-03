import { tapResponse } from '@ngrx/operators';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { UploadedFile, UPLOAD_TYPE } from '../../../models';
import { DotFileFieldUploadService } from '../../../services/upload-file/upload-file.service';

export interface FormImportUrlState {
    file: UploadedFile | null;
    status: 'init' | 'uploading' | 'done' | 'error';
    error: string | null;
    uploadType: UPLOAD_TYPE;
}

const initialState: FormImportUrlState = {
    file: null,
    status: 'init',
    error: null,
    uploadType: 'temp'
};

export const FormImportUrlStore = signalStore(
    withState(initialState),
    withComputed((state) => ({
        isLoading: computed(() => state.status() === 'uploading'),
        isDone: computed(() => state.status() === 'done')
    })),
    withMethods((store, uploadService = inject(DotFileFieldUploadService)) => ({
        uploadFileByUrl: rxMethod<string>(
            pipe(
                tap(() => patchState(store, { status: 'uploading' })),
                switchMap((fileUrl) => {
                    return uploadService
                        .uploadFile({
                            file: fileUrl,
                            uploadType: store.uploadType()
                        })
                        .pipe(
                            tapResponse({
                                next: (file) => patchState(store, { file, status: 'done' }),
                                error: console.error
                            })
                        );
                })
            )
        ),
        setUploadType: (uploadType: FormImportUrlState['uploadType']) => {
            patchState(store, { uploadType });
        }
    }))
);
