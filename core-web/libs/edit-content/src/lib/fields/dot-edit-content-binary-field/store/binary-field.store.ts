import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { from, Observable, of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotLicenseService, DotUploadService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile, DotFileMetadata } from '@dotcms/dotcms-models';

import {
    BinaryFieldMode,
    BinaryFieldStatus,
    UI_MESSAGE_KEYS,
    UiMessageI
} from '../interfaces/index';
import { getUiMessage } from '../utils/binary-field-utils';

export interface DotFilePreview extends DotFileMetadata {
    id: string;
    contentType: string;
    name: string;
    titleImage: string;
    inode?: string;
    url?: string;
    content?: string;
}

export interface BinaryFieldState {
    file: DotFilePreview;
    value: string;
    mode: BinaryFieldMode;
    status: BinaryFieldStatus;
    uiMessage: UiMessageI;
    dropZoneActive: boolean;
    isEnterprise: boolean;
}

const initialState: BinaryFieldState = {
    file: null,
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

    // Selectors
    readonly vm$ = this.select((state) => ({
        ...state,
        isLoading: state.status === BinaryFieldStatus.UPLOADING
    }));

    readonly value$ = this.select((state) => state.value);

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

    readonly setFile = this.updater<DotFilePreview>((state, file) => ({
        ...state,
        status: BinaryFieldStatus.PREVIEW,
        value: file.id,
        file
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
        file: null,
        value: '',
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
                        (tempFile) => {
                            const { referenceUrl, thumbnailUrl, metadata, id } = tempFile;
                            const { name, contentType } = metadata;

                            this.setFile({
                                id,
                                name,
                                contentType,
                                titleImage: '',
                                url: thumbnailUrl || referenceUrl,
                                ...metadata
                            });
                        },
                        () => this.setError(getUiMessage(UI_MESSAGE_KEYS.SERVER_ERROR))
                    )
                )
            )
        )
    );

    readonly setFileFromTemp = this.effect<DotCMSTempFile>((file$: Observable<DotCMSTempFile>) => {
        return file$.pipe(
            tap(() => this.setUploading()),
            switchMap((tempFile) => {
                const { referenceUrl, thumbnailUrl, metadata, id } = tempFile;
                const { name, contentType, editableAsText = false } = metadata;

                const obs$ = editableAsText ? this.getFileContent(referenceUrl) : of('');

                return obs$.pipe(
                    tapResponse(
                        (content = '') => {
                            this.setFile({
                                id,
                                name,
                                contentType,
                                titleImage: '',
                                content,
                                url: thumbnailUrl || referenceUrl,
                                ...metadata
                            });
                        },
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
                    const { titleImage, inode } = contentlet;
                    const metadataKey = `${titleImage}MetaData`;
                    const metaData = contentlet['metaData'] || contentlet[metadataKey];
                    const sourceURL = contentlet[titleImage];

                    const { name, contentType, editableAsText = false } = metaData;

                    const obs$ = editableAsText ? this.getFileContent(sourceURL) : of('');

                    return obs$.pipe(
                        tapResponse(
                            (content = '') => {
                                this.setFile({
                                    id: inode,
                                    inode,
                                    name,
                                    contentType,
                                    titleImage,
                                    content,
                                    ...metaData
                                });
                            },
                            () => this.setError(getUiMessage(UI_MESSAGE_KEYS.SERVER_ERROR))
                        )
                    );
                })
            );
        }
    );

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
}
