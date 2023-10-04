import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, from } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile, DotHttpErrorResponse } from '@dotcms/dotcms-models';

export interface DotBinaryFieldUrlModeState {
    tempFile: DotCMSTempFile;
    isLoading: boolean;
    error: string;
}

@Injectable()
export class DotBinaryFieldUrlModeStore extends ComponentStore<DotBinaryFieldUrlModeState> {
    private _maxFileSize: number;

    readonly vm$ = this.select((state) => state);

    readonly tempFile$ = this.select(({ tempFile }) => tempFile);

    readonly isLoading$ = this.select(({ isLoading }) => isLoading);

    constructor(private readonly dotUploadService: DotUploadService) {
        super({
            tempFile: null,
            isLoading: false,
            error: ''
        });
    }

    // Update state
    readonly setTempFile = this.updater((state, tempFile: DotCMSTempFile) => {
        return {
            ...state,
            tempFile,
            isLoading: false,
            error: ''
        };
    });

    readonly setIsLoading = this.updater((state, isLoading: boolean) => {
        return {
            ...state,
            isLoading
        };
    });

    readonly setError = this.updater((state, error: string) => {
        return {
            ...state,
            isLoading: false,
            error
        };
    });

    // Actions
    readonly uploadFileByUrl = this.effect(
        (
            data$: Observable<{
                url: string;
                signal: AbortSignal;
            }>
        ) => {
            return data$.pipe(
                tap(() => this.setIsLoading(true)),
                switchMap(({ url, signal }) => this.uploadTempFile(url, signal))
            );
        }
    );

    setMaxFileSize(bytes: number) {
        this._maxFileSize = bytes / (1024 * 1024);
    }

    private uploadTempFile(file: File | string, signal: AbortSignal): Observable<DotCMSTempFile> {
        return from(
            this.dotUploadService.uploadFile({
                file,
                maxSize: this._maxFileSize ? `${this._maxFileSize}MB` : '',
                signal: signal
            })
        ).pipe(
            tapResponse(
                (tempFile: DotCMSTempFile) => this.setTempFile(tempFile),
                (error: DotHttpErrorResponse) => {
                    if (!signal?.aborted) {
                        this.setError(error.message);

                        return;
                    }

                    this.setIsLoading(false);
                }
            )
        );
    }
}
