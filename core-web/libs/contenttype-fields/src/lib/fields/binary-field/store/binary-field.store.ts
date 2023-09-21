import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, from } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DropZoneFileEvent } from '@dotcms/ui';

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

const initialState: BinaryFieldState = {
    file: null,
    tempFile: null,
    mode: BINARY_FIELD_MODE.DROPZONE,
    status: BINARY_FIELD_STATUS.INIT,
    dialogOpen: false,
    dropZoneActive: false,
    UiMessage: getUiMessage(UI_MESSAGE_KEYS.DEFAULT)
};

@Injectable()
export class DotBinaryFieldStore extends ComponentStore<BinaryFieldState> {
    private _maxFileSize: number;

    // Selectors
    readonly vm$ = this.select((state) => state);

    // File state
    readonly file$ = this.select((state) => state.file);

    // Temp file state
    readonly tempFile$ = this.select((state) => state.tempFile);

    // Mode state
    readonly mode$ = this.select((state) => state.mode);

    constructor(private readonly dotUploadService: DotUploadService) {
        super(initialState);
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

    readonly invalidFile = this.updater<UiMessageI>((state, UiMessage) => ({
        ...state,
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
        dialogOpen: false,
        mode: BINARY_FIELD_MODE.DROPZONE
    }));

    readonly removeFile = this.updater((state) => ({
        ...state,
        file: null,
        tempFile: null,
        status: BINARY_FIELD_STATUS.INIT
    }));

    //  Effects
    readonly handleFileDrop = this.effect<DropZoneFileEvent>((event$) => {
        return event$.pipe(
            tap(({ file }) => {
                this.setFile(file);
                this.setStatus(BINARY_FIELD_STATUS.UPLOADING);
                this.setUiMessage(getUiMessage(UI_MESSAGE_KEYS.DEFAULT));
            }),
            switchMap(({ file }) => this.uploadTempFile(file))
        );
    });

    readonly handleFileSelection = this.effect<File>((file$) => {
        return file$.pipe(
            tap((file: File) => {
                this.setUiMessage(getUiMessage(UI_MESSAGE_KEYS.DEFAULT));
                this.setFile(file);
                this.setStatus(BINARY_FIELD_STATUS.UPLOADING);
            }),
            switchMap((file) => this.uploadTempFile(file))
        );
    });

    readonly handleCreateFile = this.effect<{ name: string; code: string }>((fileDetails$) => {
        /* To be implemented */
        return fileDetails$.pipe();
    });

    readonly handleExternalSourceFile = this.effect<string>((url$) => {
        /* To be implemented */
        return url$.pipe();
    });

    setMaxFileSize(maxFileSize: number) {
        this._maxFileSize = maxFileSize;
    }

    private uploadTempFile(file: File): Observable<DotCMSTempFile> {
        /* To be implemented */
        return from(
            this.dotUploadService.uploadFile({
                file,
                maxSize: `${this._maxFileSize}`,
                signal: null
            })
        ).pipe(
            tapResponse(
                (tempFile) => {
                    this.setStatus(BINARY_FIELD_STATUS.PREVIEW);
                    this.setTempFile(tempFile);
                },
                () => {
                    this.setUiMessage(getUiMessage(UI_MESSAGE_KEYS.SERVER_ERROR));
                    this.setStatus(BINARY_FIELD_STATUS.ERROR);
                    this.setTempFile(null);
                }
            )
        );
    }
}
