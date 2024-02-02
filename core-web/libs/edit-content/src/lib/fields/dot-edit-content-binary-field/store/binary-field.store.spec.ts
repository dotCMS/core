import { expect, describe } from '@jest/globals';
import { HttpMethod, SpectatorService, createServiceFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { skip } from 'rxjs/operators';

import { DotLicenseService, DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DropZoneErrorType } from '@dotcms/ui';

import { BinaryFieldState, DotBinaryFieldStore } from './binary-field.store';

import { BINARY_FIELD_CONTENTLET } from '../../../utils/mocks';
import { BinaryFieldMode, BinaryFieldStatus, UI_MESSAGE_KEYS } from '../interfaces';
import { getUiMessage } from '../utils/binary-field-utils';
import { fileMetaData } from '../utils/mock';

const INITIAL_STATE: BinaryFieldState = {
    contentlet: null,
    tempFile: null,
    value: null,
    mode: BinaryFieldMode.DROPZONE,
    status: BinaryFieldStatus.INIT,
    uiMessage: getUiMessage(UI_MESSAGE_KEYS.DEFAULT),
    dropZoneActive: false,
    isEnterprise: false
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
    mimeType: 'mimeType',
    metadata: fileMetaData
};

describe('DotBinaryFieldStore', () => {
    let spectator: SpectatorService<DotBinaryFieldStore>;
    let store: DotBinaryFieldStore;
    let httpMock: HttpTestingController;

    let dotUploadService: DotUploadService;
    let initialState;

    const createStoreService = createServiceFactory({
        service: DotBinaryFieldStore,
        imports: [HttpClientTestingModule],
        providers: [
            {
                provide: DotLicenseService,
                useValue: {
                    isEnterprise: () => of(true)
                }
            },
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
        httpMock = spectator.inject(HttpTestingController);

        store.setState(INITIAL_STATE);
        store.state$.subscribe((state) => {
            initialState = state;
        });
    });

    it('should set initial state', () => {
        expect(initialState).toEqual(INITIAL_STATE);
    });

    describe('Updaters', () => {
        it('should set Contentlet', (done) => {
            store.setContentlet(BINARY_FIELD_CONTENTLET);

            store.vm$.subscribe((state) => {
                expect(state.contentlet).toEqual(BINARY_FIELD_CONTENTLET);
                expect(state.value).toEqual(BINARY_FIELD_CONTENTLET.value);
                done();
            });
        });

        it('should set TempFile', (done) => {
            store.setTempFile(TEMP_FILE_MOCK);

            store.vm$.subscribe((state) => {
                expect(state.tempFile).toEqual(TEMP_FILE_MOCK);
                expect(state.value).toEqual(TEMP_FILE_MOCK.id);
                done();
            });
        });

        it('should set uiMessage', (done) => {
            const uiMessage = getUiMessage(DropZoneErrorType.FILE_TYPE_MISMATCH);
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
            it('should set value from tempFile and status to PREVIEW when dropping a valid', (done) => {
                const file = new File([''], 'filename');
                const spyUploading = jest.spyOn(store, 'setUploading');

                store.handleUploadFile(file);

                // Skip initial state
                store.value$.pipe(skip(1)).subscribe((value) => {
                    expect(value).toEqual({
                        value: TEMP_FILE_MOCK.id,
                        fileName: TEMP_FILE_MOCK.fileName
                    });
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
                store.value$.pipe(skip(1)).subscribe(() => {
                    expect(spyOnUploadService).toHaveBeenCalledWith({
                        file,
                        maxSize: '1MB'
                    });
                    done();
                });
            });
        });
    });

    describe('Effects', () => {
        describe('setFileFromContentlet', () => {
            it(`should get content if the file is editableAsText`, (done) => {
                const { BinaryMetaData } = BINARY_FIELD_CONTENTLET;
                const metaData = {
                    ...BinaryMetaData,
                    editableAsText: true
                };

                const NEW_BINARY_FIELD_CONTENTLET = {
                    ...BINARY_FIELD_CONTENTLET,
                    fileAssetVersion: '12345',
                    metaData
                };

                store.setFileFromContentlet(NEW_BINARY_FIELD_CONTENTLET);

                const req = httpMock.expectOne('12345', HttpMethod.GET); // Need to check here
                req.flush('DATA'); // Need to flush here

                store.state$.subscribe((state) => {
                    expect(state.contentlet).toEqual({
                        ...NEW_BINARY_FIELD_CONTENTLET,
                        mimeType: metaData.contentType,
                        name: metaData.name,
                        content: 'DATA'
                    });
                    done();
                });
            });

            it('should not get content if the file is not editableAsText', (done) => {
                store.setFileFromContentlet({
                    ...BINARY_FIELD_CONTENTLET,
                    metaData: {
                        ...fileMetaData,
                        editableAsText: false
                    }
                });

                store.state$.subscribe(() => {
                    httpMock.expectNone('test-url', HttpMethod.GET);
                    done();
                });
            });
        });

        describe('setFileFromTemp', () => {
            it(`should get content if the file is editableAsText`, (done) => {
                const NEW_TEMP_FILE_MOCK = {
                    ...TEMP_FILE_MOCK,
                    metadata: {
                        ...fileMetaData,
                        editableAsText: true
                    }
                };
                store.setFileFromTemp(NEW_TEMP_FILE_MOCK);

                const req = httpMock.expectOne(TEMP_FILE_MOCK.referenceUrl, HttpMethod.GET); // Need to check here
                req.flush('DATA'); // Need to flush here

                store.state$.subscribe((state) => {
                    expect(state.tempFile).toEqual({
                        ...NEW_TEMP_FILE_MOCK,
                        content: 'DATA'
                    });
                    done();
                });
            });

            it('should not get content if the file is not editableAsText', (done) => {
                store.setFileFromTemp({
                    ...TEMP_FILE_MOCK,
                    metadata: fileMetaData
                });

                store.state$.subscribe(() => {
                    httpMock.expectNone('test-url', HttpMethod.GET);
                    done();
                });
            });
        });
    });
});
