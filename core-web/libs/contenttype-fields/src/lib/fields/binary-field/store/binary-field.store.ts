import { ComponentStore, tapResponse } from '@ngrx/component-store';
import { Observable, from } from 'rxjs';

import { Injectable } from '@angular/core';

import { filter, switchMap, tap } from 'rxjs/operators';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DropZoneFileEvent, DropZoneFileValidity } from '@dotcms/ui';

import { UI_MESSAGE_KEYS, UiMessageI, getUiMessage } from '../../../utils/binary-field-utils';

export interface BinaryFieldState {
    file: File;
    tempFile: DotCMSTempFile;
    mode: BINARY_FIELD_MODE;
    status: BINARY_FIELD_STATUS;
    UiMessage: UiMessageI;
    rules: {
        accept: string[];
        maxFileSize: number;
    };
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
    rules: {
        accept: [],
        maxFileSize: 0
    },
    UiMessage: getUiMessage(UI_MESSAGE_KEYS.DEFAULT)
};

@Injectable()
export class DotBinaryFieldStore extends ComponentStore<BinaryFieldState> {
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

    readonly setRules = this.updater<{ accept: string[]; maxFileSize: number }>((state, rules) => ({
        ...state,
        rules
    }));

    //  Effects
    readonly handleFileDrop = this.effect<DropZoneFileEvent>((event$) => {
        return event$.pipe(
            filter(({ validity }) => {
                if (!validity.valid) {
                    this.handleFileDropError(validity);
                    this.setStatus(BINARY_FIELD_STATUS.ERROR);
                }

                return validity.valid;
            }),
            tap(({ file }) => {
                this.setUiMessage(getUiMessage(UI_MESSAGE_KEYS.DEFAULT));
                this.setFile(file);
                this.setStatus(BINARY_FIELD_STATUS.UPLOADING);
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

    private uploadTempFile(file: File): Observable<DotCMSTempFile> {
        const { maxFileSize } = this.get().rules;

        /* To be implemented */
        return from(
            this.dotUploadService.uploadFile({
                file,
                maxSize: `${maxFileSize}`,
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

    /**
     *  Handle file drop error
     *
     * @private
     * @param {DropZoneFileValidity} { fileTypeMismatch, maxFileSizeExceeded }
     * @memberof DotBinaryFieldStore
     */
    private handleFileDropError({
        fileTypeMismatch,
        maxFileSizeExceeded
    }: DropZoneFileValidity): void {
        const { accept, maxFileSize } = this.get().rules;
        const acceptedTypes = accept.join(', ');
        const maxSize = `${maxFileSize} bytes`;
        let uiMessage: UiMessageI;

        if (fileTypeMismatch) {
            uiMessage = getUiMessage(UI_MESSAGE_KEYS.FILE_TYPE_MISMATCH, acceptedTypes);
        } else if (maxFileSizeExceeded) {
            uiMessage = getUiMessage(UI_MESSAGE_KEYS.MAX_FILE_SIZE_EXCEEDED, maxSize);
        }

        this.setUiMessage(uiMessage);
    }
}
