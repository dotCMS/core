import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { from, Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotLicenseService, DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    BinaryFieldMode,
    BinaryFieldStatus,
    BinaryFile,
    UI_MESSAGE_KEYS,
    UiMessageI
} from '../interfaces/index';
import { getUiMessage } from '../utils/binary-field-utils';

export interface BinaryFieldState {
    file?: BinaryFile;
    tempFile: DotCMSTempFile | null;
    mode: BinaryFieldMode;
    status: BinaryFieldStatus;
    uiMessage: UiMessageI;
    dropZoneActive: boolean;
    isEnterprise: boolean;
}

const initialState: BinaryFieldState = {
    file: null,
    tempFile: null,
    mode: BinaryFieldMode.DROPZONE,
    status: BinaryFieldStatus.INIT,
    dropZoneActive: false,
    uiMessage: getUiMessage(UI_MESSAGE_KEYS.DEFAULT),
    isEnterprise: false
};

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
        private readonly dotLicenseService: DotLicenseService,
        private readonly http: HttpClient
    ) {
        super(initialState);
        this.dotLicenseService.isEnterprise().subscribe((isEnterprise) => {
            this.setIsEnterprise(isEnterprise);
        });
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

    readonly setIsEnterprise = this.updater<boolean>((state, isEnterprise) => ({
        ...state,
        isEnterprise
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
        status: BinaryFieldStatus.INIT,
        tempFile: null
    }));

    readonly invalidFile = this.updater<UiMessageI>((state, uiMessage) => ({
        ...state,
        dropZoneActive: false,
        uiMessage,
        status: BinaryFieldStatus.INIT
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

    /**
     * Set the file and the content
     *
     * @memberof DotBinaryFieldStore
     */
    readonly setFileAndContent = this.effect<BinaryFile>((file$: Observable<BinaryFile>) => {
        return file$.pipe(
            tap(() => this.setUploading()),
            switchMap((file) => {
                const { url, mimeType } = file;
                // TODO: This should be done in the serverside
                const obs$ = mimeType.includes('text') ? this.getFileContent(url) : of('');

                return obs$.pipe(
                    tap((content) => {
                        this.setFile({
                            ...file,
                            content
                        });
                    })
                );
            })
        );
    });

    /**
     * Set the max file size in bytes
     *
     * @param bytes The max file size in bytes
     */
    setMaxFileSize(bytes: number): void {
        // Convert bytes to MB
        this._maxFileSizeInMB = bytes / (1024 * 1024);
    }

    /**
     * Get the file content
     *
     * @private
     * @param {string} url
     * @return {*}  {Observable<string>}
     * @memberof DotBinaryFieldStore
     */
    private getFileContent(url: string): Observable<string> {
        return this.http.get(url, { responseType: 'text' });
    }

    /**
     * Create a BinaryFile from a DotCMSTempFile
     *
     * @private
     * @param {DotCMSTempFile} tempFile
     * @return {*}  {BinaryFile}
     * @memberof DotBinaryFieldStore
     */
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
