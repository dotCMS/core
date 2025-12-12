import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { Observable, from } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile, DotHttpErrorResponse } from '@dotcms/dotcms-models';

import { DotBinaryFieldValidatorService } from '../../../service/dot-binary-field-validator/dot-binary-field-validator.service';

export interface DotBinaryFieldUrlModeState {
    tempFile: DotCMSTempFile;
    isLoading: boolean;
    error: string;
}

@Injectable()
export class DotBinaryFieldUrlModeStore extends ComponentStore<DotBinaryFieldUrlModeState> {
    private readonly dotUploadService = inject(DotUploadService);
    private readonly dotBinaryFieldValidatorService = inject(DotBinaryFieldValidatorService);

    private _maxFileSize: number;
    private _accept: string[];

    readonly vm$ = this.select((state) => state);

    readonly tempFile$ = this.select(({ tempFile }) => tempFile);

    readonly error$ = this.select(({ error }) => error);

    constructor() {
        super({
            tempFile: null,
            isLoading: false,
            error: ''
        });

        this.setMaxFileSize(this.dotBinaryFieldValidatorService.maxFileSize);
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

    private uploadTempFile(file: File | string, signal: AbortSignal): Observable<DotCMSTempFile> {
        return from(
            this.dotUploadService.uploadFile({
                file,
                maxSize: this._maxFileSize ? `${this._maxFileSize}MB` : '',
                signal: signal
            })
        ).pipe(
            tapResponse({
                next: (tempFile: DotCMSTempFile) => {
                    if (!this.isValidType(tempFile)) {
                        this.setError(
                            'dot.binary.field.import.from.url.error.file.not.supported.message'
                        );
                        return;
                    }
                    this.setTempFile(tempFile);
                },
                error: (error: DotHttpErrorResponse) => {
                    if (signal.aborted) {
                        this.setIsLoading(false);
                        return;
                    }
                    this.setError(error.message);
                }
            })
        );
    }

    setMaxFileSize(bytes: number) {
        this._maxFileSize = this._maxFileSize = bytes / (1024 * 1024);
    }

    /**
     * Validate file type
     *
     * @private
     * @return {*}  {boolean}
     * @memberof DotBinaryFieldUrlModeStore
     */
    private isValidType(tempFile: DotCMSTempFile): boolean {
        const { fileName, mimeType } = tempFile;
        const extension = fileName.split('.').pop();

        return this.dotBinaryFieldValidatorService.isValidType({
            extension,
            mimeType
        });
    }
}
