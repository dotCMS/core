import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { from, Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { UI_MESSAGE_KEYS, UiMessageI, getUiMessage } from '../../../utils/binary-field-utils';
import { BinaryFile } from '../components/dot-binary-field-preview/dot-binary-field-preview.component';

export enum BinaryFieldMode {
    DROPZONE = 'DROPZONE',
    URL = 'URL',
    EDITOR = 'EDITOR'
}

export enum BinaryFieldStatus {
    INIT = 'INIT',
    UPLOADING = 'UPLOADING',
    PREVIEW = 'PREVIEW',
    ERROR = 'ERROR'
}

export interface BinaryFieldState {
    file?: BinaryFile;
    tempFile: DotCMSTempFile;
    mode: BinaryFieldMode;
    status: BinaryFieldStatus;
    uiMessage: UiMessageI;
    dropZoneActive: boolean;
}

@Injectable()
export class DotBinaryFieldStore extends ComponentStore<BinaryFieldState> {
    private _maxFileSizeInMB = 0;

    // Selectors
    readonly vm$ = this.select((state) => ({
        ...state,
        isLoading: state.status === BinaryFieldStatus.UPLOADING
    }));

    // Temp file state
    readonly tempFile$ = this.select((state) => state.tempFile);

    // Mode state
    readonly mode$ = this.select((state) => state.mode);

    constructor(
        private readonly dotUploadService: DotUploadService,
        private readonly http: HttpClient
    ) {
        super();
    }

    readonly setDropZoneActive = this.updater<boolean>((state, dropZoneActive) => ({
        ...state,
        dropZoneActive
    }));

    readonly setTempFile = this.updater<DotCMSTempFile>((state, tempFile) => ({
        ...state,
        status: BinaryFieldStatus.PREVIEW,
        file: this.fileFromTempFile(tempFile),
        tempFile
    }));

    readonly setFile = this.updater<BinaryFile>((state, file) => ({
        ...state,
        status: BinaryFieldStatus.PREVIEW,
        file
    }));

    readonly setUiMessage = this.updater<UiMessageI>((state, uiMessage) => ({
        ...state,
        uiMessage
    }));

    readonly setMode = this.updater<BinaryFieldMode>((state, mode) => ({
        ...state,
        mode
    }));

    readonly setStatus = this.updater<BinaryFieldStatus>((state, status) => ({
        ...state,
        status
    }));

    readonly setUploading = this.updater((state) => ({
        ...state,
        dropZoneActive: false,
        uiMessage: getUiMessage(UI_MESSAGE_KEYS.DEFAULT),
        status: BinaryFieldStatus.UPLOADING
    }));

    readonly setError = this.updater<UiMessageI>((state, uiMessage) => ({
        ...state,
        uiMessage,
        status: BinaryFieldStatus.ERROR,
        tempFile: null
    }));

    readonly invalidFile = this.updater<UiMessageI>((state, uiMessage) => ({
        ...state,
        dropZoneActive: false,
        uiMessage,
        status: BinaryFieldStatus.ERROR
    }));

    readonly removeFile = this.updater((state) => ({
        ...state,
        file: null,
        tempFile: null,
        status: BinaryFieldStatus.INIT
    }));

    // Effects
    readonly handleUploadFile = this.effect<File>((file$: Observable<File>) =>
        file$.pipe(
            tap(() => this.setUploading()),
            switchMap((file) =>
                from(
                    this.dotUploadService.uploadFile({
                        file,
                        maxSize: this._maxFileSizeInMB ? `${this._maxFileSizeInMB}MB` : '',
                        signal: null
                    })
                ).pipe(
                    tapResponse(
                        (tempFile) => this.setTempFile(tempFile),
                        () => this.setError(getUiMessage(UI_MESSAGE_KEYS.SERVER_ERROR))
                    )
                )
            )
        )
    );

    readonly setFileAndContent = this.effect<BinaryFile>((file$: Observable<BinaryFile>) => {
        return file$.pipe(
            tap(() => this.setUploading()),
            switchMap((file) => {
                const type = file.mimeType?.split('/')[0];
                if (type === 'text') {
                    return this.http.get(file.url, { responseType: 'text' }).pipe(
                        tap((content) => {
                            this.setFile({ ...file, content });
                        })
                    );
                }

                return of(null).pipe(tap(() => this.setFile(file)));
            })
        );
    });

    /**
     * Set the max file size in bytes
     *
     * @param bytes The max file size in bytes
     */
    setMaxFileSize(bytes: number): void {
        this._maxFileSizeInMB = bytes / (1024 * 1024);
    }

    private fileFromTempFile({
        length,
        thumbnailUrl,
        referenceUrl,
        fileName,
        mimeType,
        content = ''
    }: DotCMSTempFile): BinaryFile {
        return {
            url: thumbnailUrl || referenceUrl,
            fileSize: length,
            mimeType,
            name: fileName,
            content
        };
    }
}
