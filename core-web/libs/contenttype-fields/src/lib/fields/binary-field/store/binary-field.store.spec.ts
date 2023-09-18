import { expect, describe } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    BINARY_FIELD_MODE,
    BINARY_FIELD_STATUS,
    BinaryFieldState,
    DotBinaryFieldStore
} from './binary-field.store';

import { UI_MESSAGE_KEYS, getUiMessage } from '../../../utils/binary-field-utils';

const INITIAL_STATE: BinaryFieldState = {
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

const TEMP_FILE_MOCK: DotCMSTempFile = {
    fileName: 'image.png',
    folder: '/images',
    id: '12345',
    image: true,
    length: 1000,
    referenceUrl: '/reference/url',
    thumbnailUrl: 'image.png',
    mimeType: 'mimeType'
};
describe('DotBinaryFieldStore', () => {
    let spectator: SpectatorService<DotBinaryFieldStore>;
    let store: DotBinaryFieldStore;

    let initialState;

    const createStoreService = createServiceFactory({
        service: DotBinaryFieldStore,
        providers: [
            {
                provide: DotUploadService,
                useValue: {
                    uploadFile: ({ file }) => {
                        return new Promise((resolve) => {
                            if (file) {
                                resolve(TEMP_FILE_MOCK);
                            }
                        });
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createStoreService();
        store = spectator.inject(DotBinaryFieldStore);

        store.state$.subscribe((state) => {
            initialState = state;
        });
    });

    it('should set initial state', () => {
        expect(initialState).toEqual(INITIAL_STATE);
    });

    describe('Updaters', () => {
        it('should set File', () => {
            const file = new File([''], 'filename');
            store.setFile(file);

            store.vm$.subscribe((state) => expect(state.file).toEqual(file));
        });
        it('should set TempFile', () => {
            store.setTempFile(TEMP_FILE_MOCK);

            store.vm$.subscribe((state) => expect(state.tempFile).toEqual(TEMP_FILE_MOCK));
        });
        it('should set UiMessage', () => {
            const uiMessage = getUiMessage(UI_MESSAGE_KEYS.FILE_TYPE_MISMATCH);
            store.setUiMessage(uiMessage);

            store.vm$.subscribe((state) => expect(state.UiMessage).toEqual(uiMessage));
        });
        it('should set Mode', () => {
            store.setMode(BINARY_FIELD_MODE.EDITOR);

            store.vm$.subscribe((state) => expect(state.mode).toBe(BINARY_FIELD_MODE.EDITOR));
        });
        it('should set Status', () => {
            store.setStatus(BINARY_FIELD_STATUS.PREVIEW);

            store.vm$.subscribe((state) => expect(state.status).toBe(BINARY_FIELD_STATUS.PREVIEW));
        });
    });

    describe('Actions', () => {
        describe('handleFileDrop', () => {
            it('should set tempFile and status to PREVIEW when dropping a valid', () => {
                const file = new File([''], 'filename');
                const validity = {
                    valid: true,
                    fileTypeMismatch: false,
                    maxFileSizeExceeded: false,
                    multipleFilesDropped: false
                };

                const spyStatus = jest.spyOn(store, 'setStatus');
                const message = getUiMessage(UI_MESSAGE_KEYS.DEFAULT);

                store.vm$.subscribe((state) => {
                    expect(state.status).toBe(BINARY_FIELD_STATUS.PREVIEW);
                    expect(state.tempFile).toBe(TEMP_FILE_MOCK);
                    expect(state.UiMessage).toEqual(message);
                });

                store.handleFileDrop({ file, validity });
                // Should be called twice, one for PREVIEW and one for UPLOADING
                expect(spyStatus).toHaveBeenCalledWith(BINARY_FIELD_STATUS.UPLOADING);
            });

            it('should not set tempFile, set status to ERROR, and set MAX_FILE_SIZE_EXCEEDED messsage when drop a file that exceed the max size', () => {
                const file = new File([''], 'filename');
                const validity = {
                    valid: false,
                    fileTypeMismatch: false,
                    maxFileSizeExceeded: true,
                    multipleFilesDropped: false
                };

                const spyStatus = jest.spyOn(store, 'setStatus');
                const spyTemp = jest.spyOn(store, 'setTempFile');
                const message = getUiMessage(UI_MESSAGE_KEYS.MAX_FILE_SIZE_EXCEEDED);

                store.vm$.subscribe((state) => {
                    expect(state.status).toBe(BINARY_FIELD_STATUS.ERROR);
                    expect(state.tempFile).toBe(null);
                    expect(state.UiMessage).toEqual(message);
                });

                store.handleFileDrop({ file, validity });
                expect(spyStatus).toHaveBeenCalledWith(BINARY_FIELD_STATUS.ERROR);
                expect(spyTemp).not.toHaveBeenCalled();
            });

            it('should not set tempFile, set status to ERROR, and set FILE_TYPE_MISMATCH messsage when drop a file which extension is not allowed', () => {
                const file = new File([''], 'filename');
                const validity = {
                    valid: false,
                    fileTypeMismatch: false,
                    maxFileSizeExceeded: true,
                    multipleFilesDropped: false
                };

                const spyStatus = jest.spyOn(store, 'setStatus');
                const spyTemp = jest.spyOn(store, 'setTempFile');
                const message = getUiMessage(UI_MESSAGE_KEYS.FILE_TYPE_MISMATCH);

                store.vm$.subscribe((state) => {
                    expect(state.status).toBe(BINARY_FIELD_STATUS.ERROR);
                    expect(state.tempFile).toBe(null);
                    expect(state.UiMessage).toEqual(message);
                });

                store.handleFileDrop({ file, validity });
                expect(spyStatus).toHaveBeenCalledWith(BINARY_FIELD_STATUS.ERROR);
                expect(spyTemp).not.toHaveBeenCalled();
            });
        });

        describe('handleFileSelection', () => {
            it('should set tempFile and status to PREVIEW when select valid file', () => {
                const file = new File([''], 'filename');

                const spyStatus = jest.spyOn(store, 'setStatus');
                const message = getUiMessage(UI_MESSAGE_KEYS.DEFAULT);

                store.vm$.subscribe((state) => {
                    expect(state.status).toBe(BINARY_FIELD_STATUS.PREVIEW);
                    expect(state.tempFile).toBe(TEMP_FILE_MOCK);
                    expect(state.UiMessage).toEqual(message);
                });

                store.handleFileSelection(file);
                // Should be called twice, one for PREVIEW and one for UPLOADING
                expect(spyStatus).toHaveBeenCalledWith(BINARY_FIELD_STATUS.UPLOADING);
            });
        });
    });
});
