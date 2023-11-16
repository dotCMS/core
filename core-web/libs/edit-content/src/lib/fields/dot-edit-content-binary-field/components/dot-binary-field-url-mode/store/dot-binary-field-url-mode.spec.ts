import { expect, describe, jest } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { skip } from 'rxjs/operators';

import { DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    DotBinaryFieldUrlModeState,
    DotBinaryFieldUrlModeStore
} from './dot-binary-field-url-mode.store';

import { DotBinaryFieldValidatorService } from '../../../service/dot-binary-field-validator/dot-binary-field-validator.service';

const INITIAL_STATE: DotBinaryFieldUrlModeState = {
    tempFile: null,
    isLoading: false,
    error: ''
};

export const TEMP_FILE_MOCK: DotCMSTempFile = {
    fileName: 'image.png',
    folder: '/images',
    id: '12345',
    image: true,
    length: 1000,
    referenceUrl: '/reference/url',
    thumbnailUrl: 'image.png',
    mimeType: 'mimeType'
};

describe('DotBinaryFieldUrlModeStore', () => {
    let spectator: SpectatorService<DotBinaryFieldUrlModeStore>;
    let store: DotBinaryFieldUrlModeStore;
    let dotBinaryFieldValidatorService: DotBinaryFieldValidatorService;

    let dotUploadService: DotUploadService;
    let initialState;

    const createStoreService = createServiceFactory({
        service: DotBinaryFieldUrlModeStore,
        providers: [
            DotBinaryFieldValidatorService,
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
        dotUploadService = spectator.inject(DotUploadService);
        store = spectator.inject(DotBinaryFieldUrlModeStore);
        dotBinaryFieldValidatorService = spectator.inject(DotBinaryFieldValidatorService);
        dotBinaryFieldValidatorService.setMaxFileSize(1048576);

        store.setState(INITIAL_STATE);
        store.state$.subscribe((state) => {
            initialState = state;
        });
    });

    it('should set initial state', () => {
        expect(initialState).toEqual(INITIAL_STATE);
    });

    describe('Updaters', () => {
        it('should set TempFile', (done) => {
            store.setTempFile(TEMP_FILE_MOCK);

            store.tempFile$.subscribe((tempFile) => {
                expect(tempFile).toEqual(TEMP_FILE_MOCK);
                done();
            });
        });

        it('should set isLoading', (done) => {
            store.setIsLoading(true);

            store.vm$.subscribe((state) => {
                expect(state.isLoading).toBeTruthy();
                done();
            });
        });

        it('should set error and isLoading to false', (done) => {
            store.setIsLoading(true); // Set isLoading to true
            store.setError('Request Error'); // Set error and isLoading to false

            // Skip setIsLoading
            store.vm$.subscribe((state) => {
                expect(state.error).toBe('Request Error');
                expect(state.isLoading).toBeFalsy();
                done();
            });
        });
    });

    describe('Actions', () => {
        describe('handleUploadFile', () => {
            it('should set tempFile and loading to false', (done) => {
                const spySetIsLoading = jest.spyOn(store, 'setIsLoading');
                const abortController = new AbortController();

                store.uploadFileByUrl({
                    url: 'url',
                    signal: abortController.signal
                });

                // Skip initial state
                store.tempFile$.pipe(skip(1)).subscribe((tempFile) => {
                    expect(tempFile).toEqual(TEMP_FILE_MOCK);
                    done();
                });

                expect(spySetIsLoading).toHaveBeenCalledWith(true);
            });

            it('should called tempFile API with 1MB', (done) => {
                const spyOnUploadService = jest.spyOn(dotUploadService, 'uploadFile');

                // 1MB
                store.setMaxFileSize(1048576);
                const abortController = new AbortController();

                store.uploadFileByUrl({
                    url: 'url',
                    signal: abortController.signal
                });

                // Skip initial state
                store.tempFile$.pipe(skip(1)).subscribe(() => {
                    expect(spyOnUploadService).toHaveBeenCalledWith({
                        file: 'url',
                        maxSize: '1MB',
                        signal: abortController.signal
                    });
                    done();
                });
            });
        });
    });
});
