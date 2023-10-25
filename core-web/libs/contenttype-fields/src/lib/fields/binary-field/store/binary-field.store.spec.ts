import { expect, describe } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { skip } from 'rxjs/operators';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { BinaryFieldState, DotBinaryFieldStore } from './binary-field.store';

import { UI_MESSAGE_KEYS, getUiMessage } from '../../../utils/binary-field-utils';
import { BinaryFieldMode, BinaryFieldStatus } from '../interfaces';

const INITIAL_STATE: BinaryFieldState = {
    file: null,
    tempFile: null,
    mode: BinaryFieldMode.DROPZONE,
    status: BinaryFieldStatus.INIT,
    uiMessage: getUiMessage(UI_MESSAGE_KEYS.DEFAULT),
    dropZoneActive: false
};

export const TEMP_FILE_MOCK: DotCMSTempFile = {
    content: 'test',
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

    let dotUploadService: DotUploadService;
    let initialState;

    const createStoreService = createServiceFactory({
        service: DotBinaryFieldStore,
        imports: [HttpClientTestingModule],
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
        dotUploadService = spectator.inject(DotUploadService);

        store.setState(INITIAL_STATE);
        store.state$.subscribe((state) => {
            initialState = state;
        });
    });

    it('should set initial state', () => {
        expect(initialState).toEqual(INITIAL_STATE);
    });

    describe('Updaters', () => {
        it('should set File', (done) => {
            const mockFile = {
                mimeType: 'mimeType',
                name: 'name',
                fileSize: 100000,
                content: 'content',
                url: 'url',
                inode: 'inode',
                titleImage: 'titleImage',
                width: '20',
                height: '20'
            };
            store.setFile(mockFile);

            store.vm$.subscribe((state) => {
                expect(state.file).toEqual(mockFile);
                done();
            });
        });
        it('should set TempFile', (done) => {
            store.setTempFile(TEMP_FILE_MOCK);

            store.tempFile$.subscribe((tempFile) => {
                expect(tempFile).toEqual(TEMP_FILE_MOCK);
                done();
            });
        });
        it('should set uiMessage', (done) => {
            const uiMessage = getUiMessage(UI_MESSAGE_KEYS.FILE_TYPE_MISMATCH);
            store.setUiMessage(uiMessage);

            store.vm$.subscribe((state) => {
                expect(state.uiMessage).toEqual(uiMessage);
                done();
            });
        });
        it('should set Mode', (done) => {
            store.setMode(BinaryFieldMode.EDITOR);

            store.vm$.subscribe((state) => {
                expect(state.mode).toBe(BinaryFieldMode.EDITOR);
                done();
            });
        });
        it('should set Status', (done) => {
            store.setStatus(BinaryFieldStatus.PREVIEW);

            store.vm$.subscribe((state) => {
                expect(state.status).toBe(BinaryFieldStatus.PREVIEW);
                done();
            });
        });

        it('should set DropZoneActive', (done) => {
            store.setDropZoneActive(true);

            store.vm$.subscribe((state) => {
                expect(state.dropZoneActive).toBe(true);
                done();
            });
        });
    });

    describe('Actions', () => {
        describe('handleUploadFile', () => {
            it('should set tempFile and status to PREVIEW when dropping a valid', (done) => {
                const file = new File([''], 'filename');
                const spyUploading = jest.spyOn(store, 'setUploading');

                store.handleUploadFile(file);

                // Skip initial state
                store.tempFile$.pipe(skip(1)).subscribe((tempFile) => {
                    expect(tempFile).toBe(TEMP_FILE_MOCK);
                    done();
                });

                expect(spyUploading).toHaveBeenCalled();
            });

            it('should called tempFile API with 1MB', (done) => {
                const file = new File([''], 'filename');
                const spyOnUploadService = jest.spyOn(dotUploadService, 'uploadFile');

                // 1MB
                store.setMaxFileSize(1048576);
                store.handleUploadFile(file);

                // Skip initial state
                store.tempFile$.pipe(skip(1)).subscribe(() => {
                    expect(spyOnUploadService).toHaveBeenCalledWith({
                        file,
                        maxSize: '1MB',
                        signal: null
                    });
                    done();
                });
            });
        });
    });
});
