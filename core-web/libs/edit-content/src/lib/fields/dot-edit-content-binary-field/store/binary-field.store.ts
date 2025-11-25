import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { from, Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap, tap, map, catchError, distinctUntilChanged } from 'rxjs/operators';

import { DotLicenseService, DotUploadService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    BinaryFieldMode,
    BinaryFieldStatus,
    UI_MESSAGE_KEYS,
    UiMessageI
} from '../interfaces/index';
import { getFieldVersion, getFileMetadata, getUiMessage } from '../utils/binary-field-utils';

export interface BinaryFieldState {
    contentlet: DotCMSContentlet;
    tempFile: DotCMSTempFile;
    value: string;
    mode: BinaryFieldMode;
    status: BinaryFieldStatus;
    uiMessage: UiMessageI;
    dropZoneActive: boolean;
    isEnterprise: boolean;
}

const initialState: BinaryFieldState = {
    contentlet: null,
    tempFile: null,
    value: null,
    mode: BinaryFieldMode.DROPZONE,
    status: BinaryFieldStatus.INIT,
    dropZoneActive: false,
    uiMessage: getUiMessage(UI_MESSAGE_KEYS.DEFAULT),
    isEnterprise: false
};

@Injectable()
export class DotBinaryFieldStore extends ComponentStore<BinaryFieldState> {
    private _maxFileSizeInMB = 0;

    get maxFile() {
        return this._maxFileSizeInMB ? `${this._maxFileSizeInMB}MB` : '';
    }

    // Selectors
    readonly vm$ = this.select((state) => ({
        ...state,
        isLoading: state.status === BinaryFieldStatus.UPLOADING
    }));

    readonly value$ = this.select(({ value, tempFile }) => ({
        value,
        fileName: tempFile?.fileName
    })).pipe(distinctUntilChanged((previous, current) => previous.value === current.value));

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

    readonly setContentlet = this.updater<DotCMSContentlet>((state, contentlet) => ({
        ...state,
        contentlet,
        status: BinaryFieldStatus.PREVIEW,
        value: contentlet?.value || ''
    }));

    readonly setTempFile = this.updater<DotCMSTempFile>((state, tempFile) => ({
        ...state,
        tempFile,
        contentlet: null,
        status: BinaryFieldStatus.PREVIEW,
        value: tempFile?.id
    }));

    readonly setValue = this.updater<string>((state, value) => ({
        ...state,
        value
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
        contentlet: null,
        tempFile: null,
        value: '',
        status: BinaryFieldStatus.INIT
    }));

    // Effects
    readonly handleUploadFile = this.effect<File>((file$: Observable<File>) =>
        file$.pipe(
            tap(() => this.setUploading()),
            switchMap((file) =>
                this.uploadFile(file).pipe(
                    switchMap((tempFile) => this.handleTempFile(tempFile)),
                    tap((file) => this.setTempFile(file)),
                    catchError(() => {
                        this.setError(getUiMessage(UI_MESSAGE_KEYS.SERVER_ERROR));

                        return of(null);
                    })
                )
            )
        )
    );

    readonly setFileFromTemp = this.effect<DotCMSTempFile>((file$: Observable<DotCMSTempFile>) => {
        return file$.pipe(
            tap(() => this.setUploading()),

            switchMap((tempFile) => {
                return this.handleTempFile(tempFile).pipe(
                    tapResponse(
                        (file) => this.setTempFile(file),
                        () => this.setError(getUiMessage(UI_MESSAGE_KEYS.SERVER_ERROR))
                    )
                );
            })
        );
    });

    readonly setFileFromContentlet = this.effect<DotCMSContentlet>(
        (contentlet$: Observable<DotCMSContentlet>) => {
            return contentlet$.pipe(
                tap(() => this.setUploading()),
                switchMap((contentlet) => {
                    const { contentType, editableAsText, name } = getFileMetadata(contentlet);
                    const contentURL = getFieldVersion(contentlet);
                    const obs$ = editableAsText ? this.getFileContent(contentURL) : of('');

                    return obs$.pipe(
                        tapResponse(
                            (content = '') => {
                                this.setContentlet({
                                    ...contentlet,
                                    mimeType: contentType,
                                    name,
                                    content
                                });
                            },
                            () => {
                                this.setContentlet({
                                    ...contentlet,
                                    mimeType: contentType,
                                    name
                                });
                            }
                        )
                    );
                })
            );
        }
    );

    private handleTempFile(tempFile: DotCMSTempFile): Observable<DotCMSTempFile> {
        const { referenceUrl, metadata } = tempFile;
        const { editableAsText = false } = metadata;

        const obs$ = editableAsText ? this.getFileContent(referenceUrl) : of('');

        return obs$.pipe(
            map((content) => {
                return {
                    ...tempFile,
                    content
                };
            })
        );
    }

    /**
     * Set the max file size in bytes
     *
     * @param bytes The max file size in bytes
     */
    setMaxFileSize(bytes: number): void {
        // Convert bytes to MB
        this._maxFileSizeInMB = bytes / (1024 * 1024);
    }

    private uploadFile(file: File): Observable<DotCMSTempFile> {
        return from(
            this.dotUploadService.uploadFile({
                file,
                maxSize: this.maxFile
            })
        );
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
}
