import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, from } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { UI_MESSAGE_KEYS, UiMessageI, getUiMessage } from '../../../utils/binary-field-utils';

export interface BinaryFieldState {
    file: File;
    tempFile: DotCMSTempFile;
    mode: BINARY_FIELD_MODE;
    status: BINARY_FIELD_STATUS;
    UiMessage: UiMessageI;
    dialogOpen: boolean;
    dropZoneActive: boolean;
}

export enum BINARY_FIELD_MODE {
    DROPZONE = 'DROPZONE',
    URL = 'URL',
    EDITOR = 'EDITOR'
}

export enum BINARY_FIELD_STATUS {
    INIT = 'INIT',
    UPLOADING = 'UPLOADING',
    PREVIEW = 'PREVIEW',
    ERROR = 'ERROR'
}

@Injectable()
export class DotBinaryFieldStore extends ComponentStore<BinaryFieldState> {
    private _maxFileSizeInMB: number;

    // Selectors
    readonly vm$ = this.select((state) => state);

    // File state
    readonly file$ = this.select((state) => state.file);

    // Temp file state
    readonly tempFile$ = this.select((state) => state.tempFile);

    // Mode state
    readonly mode$ = this.select((state) => state.mode);

    constructor(private readonly dotUploadService: DotUploadService) {
        super();
    }

    // Updaters
    readonly setFile = this.updater<File>((state, file) => ({
        ...state,
        file
    }));

    readonly setDialogOpen = this.updater<boolean>((state, dialogOpen) => ({
        ...state,
        dialogOpen
    }));

    readonly setDropZoneActive = this.updater<boolean>((state, dropZoneActive) => ({
        ...state,
        dropZoneActive
    }));

    readonly setTempFile = this.updater<DotCMSTempFile>((state, tempFile) => ({
        ...state,
        status: BINARY_FIELD_STATUS.PREVIEW,
        tempFile
    }));

    readonly setUiMessage = this.updater<UiMessageI>((state, UiMessage) => ({
        ...state,
        UiMessage
    }));

    readonly setMode = this.updater<BINARY_FIELD_MODE>((state, mode) => ({
        ...state,
        mode
    }));

    readonly setStatus = this.updater<BINARY_FIELD_STATUS>((state, status) => ({
        ...state,
        status
    }));

    readonly setUploading = this.updater((state) => ({
        ...state,
        dialogOpen: false,
        dropZoneActive: false,
        uiMessage: getUiMessage(UI_MESSAGE_KEYS.DEFAULT),
        status: BINARY_FIELD_STATUS.UPLOADING
    }));

    readonly setError = this.updater<UiMessageI>((state, UiMessage) => ({
        ...state,
        UiMessage,
        status: BINARY_FIELD_STATUS.ERROR,
        tempFile: null
    }));

    readonly invalidFile = this.updater<UiMessageI>((state, UiMessage) => ({
        ...state,
        dropZoneActive: false,
        UiMessage,
        status: BINARY_FIELD_STATUS.ERROR
    }));

    readonly openDialog = this.updater<BINARY_FIELD_MODE>((state, mode) => ({
        ...state,
        dialogOpen: true,
        mode
    }));

    readonly closeDialog = this.updater((state) => ({
        ...state,
        dialogOpen: false
    }));

    readonly removeFile = this.updater((state) => ({
        ...state,
        file: null,
        tempFile: null,
        status: BINARY_FIELD_STATUS.INIT
    }));

    //  Effects
    readonly handleUploadFile = this.effect<File | string>((event$) => {
        return event$.pipe(
            tap(() => this.setUploading()),
            switchMap((file) => this.uploadTempFile(file))
        );
    });

    readonly handleCreateFile = this.effect<{ name: string; code: string }>((fileDetails$) => {
        /* To be implemented */
        return fileDetails$.pipe();
    });

    /**
     * Set the max file size in Bytes
     *
     * @param {number} bytes
     * @memberof DotBinaryFieldStore
     */
    setMaxFileSize(bytes: number) {
        this._maxFileSizeInMB = bytes / (1024 * 1024);
    }

    private uploadTempFile(file: File | string): Observable<DotCMSTempFile> {
        return from(
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
        );
    }
}
