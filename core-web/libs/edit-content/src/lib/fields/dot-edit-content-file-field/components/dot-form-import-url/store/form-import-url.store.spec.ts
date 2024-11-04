import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { NEW_FILE_MOCK } from './../../../../../utils/mocks';
import { FormImportUrlStore, FormImportUrlState } from './form-import-url.store';

import { DotFileFieldUploadService } from '../../../services/upload-file/upload-file.service';

describe('FormImportUrlStore', () => {
    let store: InstanceType<typeof FormImportUrlStore>;
    let uploadService: SpyObject<DotFileFieldUploadService>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [FormImportUrlStore, mockProvider(DotFileFieldUploadService)]
        });

        store = TestBed.inject(FormImportUrlStore);
        uploadService = TestBed.inject(
            DotFileFieldUploadService
        ) as SpyObject<DotFileFieldUploadService>;
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    it('should initialize with the correct state', () => {
        expect(store.file()).toBeNull();
        expect(store.status()).toBe(ComponentStatus.INIT);
        expect(store.error()).toBeNull();
        expect(store.uploadType()).toBe('temp');
        expect(store.acceptedFiles()).toEqual([]);
    });

    describe('Method: initSetup', () => {
        it('should set the upload type and accepted files', () => {
            const data: Partial<FormImportUrlState> = {
                uploadType: 'dotasset',
                acceptedFiles: ['image/png']
            };
            store.initSetup(data);
            expect(store.uploadType()).toBe('dotasset');
            expect(store.acceptedFiles()).toEqual(['image/png']);
        });
    });

    describe('Method: uploadFileByUrl', () => {
        it('should upload file by URL successfully', () => {
            const fileUrl = 'http://example.com/file.png';

            const mockContentlet = NEW_FILE_MOCK.entity;

            uploadService.uploadFile.mockReturnValue(
                of({ source: 'contentlet', file: mockContentlet })
            );

            const abortController = new AbortController();

            store.uploadFileByUrl({ fileUrl, abortSignal: abortController.signal });

            expect(store.file()?.file).toEqual(mockContentlet);
            expect(store.file()?.source).toEqual('contentlet');
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should handle upload file by URL error', () => {
            const fileUrl = 'http://example.com/file.png';
            uploadService.uploadFile.mockReturnValue(throwError(new Error('Invalid file type')));

            const abortController = new AbortController();

            store.uploadFileByUrl({ fileUrl, abortSignal: abortController.signal });

            expect(store.error()).toBe(
                'dot.file.field.import.from.url.error.file.not.supported.message'
            );
            expect(store.status()).toBe(ComponentStatus.ERROR);
        });
    });
});
