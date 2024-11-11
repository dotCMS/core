import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { FileFieldStore } from './file-field.store';

import { UIMessage } from '../../../models/dot-edit-content-file.model';
import { NEW_FILE_MOCK } from '../../../utils/mocks';
import { DotFileFieldUploadService } from '../services/upload-file/upload-file.service';
import { getUiMessage } from '../utils/messages';

describe('FileFieldStore', () => {
    let store: InstanceType<typeof FileFieldStore>;
    let service: SpyObject<DotFileFieldUploadService>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [FileFieldStore, mockProvider(DotFileFieldUploadService)]
        });

        store = TestBed.inject(FileFieldStore);
        service = TestBed.inject(DotFileFieldUploadService) as SpyObject<DotFileFieldUploadService>;
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    describe('Method: initLoad', () => {
        it('should init the state properly for File input', () => {
            store.initLoad({
                inputType: 'File',
                fieldVariable: 'myVar'
            });

            expect(store.inputType()).toEqual('File');
            expect(store.fieldVariable()).toEqual('myVar');
            expect(store.allowExistingFile()).toBe(true);
            expect(store.allowURLImport()).toBe(true);
            expect(store.allowCreateFile()).toBe(true);
            expect(store.allowGenerateImg()).toBe(false);
            expect(store.acceptedFiles().length).toBe(0);
            expect(store.maxFileSize()).toBeNull();
        });

        it('should init the state properly for Image input', () => {
            store.initLoad({
                inputType: 'Image',
                fieldVariable: 'myVar'
            });

            expect(store.inputType()).toEqual('Image');
            expect(store.fieldVariable()).toEqual('myVar');
            expect(store.allowExistingFile()).toBe(true);
            expect(store.allowURLImport()).toBe(true);
            expect(store.allowCreateFile()).toBe(false);
            expect(store.allowGenerateImg()).toBe(true);
            expect(store.acceptedFiles()).toContain('image/*');
            expect(store.maxFileSize()).toBeNull();
        });

        it('should init the state properly for Binary input', () => {
            store.initLoad({
                inputType: 'Binary',
                fieldVariable: 'myVar'
            });

            expect(store.inputType()).toEqual('Binary');
            expect(store.fieldVariable()).toEqual('myVar');
            expect(store.allowExistingFile()).toBe(false);
            expect(store.allowURLImport()).toBe(true);
            expect(store.allowCreateFile()).toBe(true);
            expect(store.allowGenerateImg()).toBe(true);
            expect(store.acceptedFiles().length).toBe(0);
            expect(store.maxFileSize()).toBeNull();
        });
    });

    describe('Method: setUIMessage', () => {
        it('should set uiMessage with valid UIMessage object', () => {
            const uiMessage: UIMessage = {
                message: 'test message',
                severity: 'info',
                icon: 'pi pi-upload'
            };

            store.setUIMessage(uiMessage);

            expect(store.uiMessage()).toEqual({
                message: 'test message',
                severity: 'info',
                icon: 'pi pi-upload',
                args: [`${store.maxFileSize()}`, store.acceptedFiles().join(', ')]
            });
        });
    });

    describe('Method: removeFile', () => {
        it('should set the state properly when removeFile is called', fakeAsync(() => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            service.uploadFile.mockReturnValue(of({ source: 'contentlet', file: mockContentlet }));

            const file = new File([''], 'filename', { type: 'text/plain' });
            Object.defineProperty(file, 'size', { value: 5000 });

            store.handleUploadFile(file);

            tick(50);

            store.removeFile();
            expect(store.value()).toBe('');
            expect(store.fileStatus()).toBe('init');
            expect(store.uiMessage()).toBe(getUiMessage('DEFAULT'));
            expect(store.uploadedFile()).toBeNull();
        }));
    });

    describe('Method: setDropZoneState', () => {
        it('should set dropZoneActive to true', () => {
            store.setDropZoneState(true);
            expect(store.dropZoneActive()).toBe(true);
        });

        it('should set dropZoneActive to false', () => {
            store.setDropZoneState(false);
            expect(store.dropZoneActive()).toBe(false);
        });
    });

    describe('Method: handleUploadFile', () => {
        it('should does not call uploadService with maxFileSize exceeded', fakeAsync(() => {
            store.setMaxSizeFile(10000);

            tick(50);

            const file = new File([''], 'filename', { type: 'text/plain' });
            Object.defineProperty(file, 'size', { value: 20000 });

            store.handleUploadFile(file);
            expect(service.uploadFile).not.toHaveBeenCalled();
        }));

        it('should set state properly with maxFileSize exceeded', fakeAsync(() => {
            store.setMaxSizeFile(10000);

            tick(50);

            const file = new File([''], 'filename', { type: 'text/plain' });
            Object.defineProperty(file, 'size', { value: 20000 });

            store.handleUploadFile(file);
            expect(store.dropZoneActive()).toBe(true);
            expect(store.fileStatus()).toBe('init');
            expect(store.uiMessage()).toEqual({
                ...getUiMessage('MAX_FILE_SIZE_EXCEEDED'),
                args: ['10000']
            });
        }));

        it('should call uploadService with maxFileSize not exceeded', fakeAsync(() => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            service.uploadFile.mockReturnValue(of({ source: 'contentlet', file: mockContentlet }));

            store.setMaxSizeFile(10000);

            tick(50);

            const file = new File([''], 'filename', { type: 'text/plain' });
            Object.defineProperty(file, 'size', { value: 5000 });

            store.handleUploadFile(file);
            expect(service.uploadFile).toHaveBeenCalledWith({
                file,
                acceptedFiles: [],
                maxSize: '10000',
                uploadType: 'dotasset'
            });
        }));

        it('should set state properly with maxFileSize not exceeded', fakeAsync(() => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            service.uploadFile.mockReturnValue(of({ source: 'contentlet', file: mockContentlet }));

            store.setMaxSizeFile(10000);

            tick(50);

            const file = new File([''], 'filename', { type: 'text/plain' });
            Object.defineProperty(file, 'size', { value: 5000 });

            store.handleUploadFile(file);
            expect(store.value()).toBe(mockContentlet.identifier);
            expect(store.fileStatus()).toBe('preview');
            expect(store.uploadedFile()).toEqual({
                source: 'contentlet',
                file: mockContentlet
            });
        }));

        it('should set state properly with an error calling uploadFile', () => {
            service.uploadFile.mockReturnValue(throwError('error'));

            const file = new File([''], 'filename', { type: 'text/plain' });
            store.handleUploadFile(file);

            expect(service.uploadFile).toHaveBeenCalledWith({
                file,
                uploadType: 'dotasset',
                acceptedFiles: [],
                maxSize: null
            });
            expect(store.fileStatus()).toBe('init');
            expect(store.uiMessage()).toEqual(getUiMessage('SERVER_ERROR'));
        });
    });

    describe('Method: getAssetData', () => {
        it('should call getContentById', () => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            service.getContentById.mockReturnValue(of(mockContentlet));

            store.getAssetData(mockContentlet.identifier);
            expect(service.getContentById).toHaveBeenCalledWith(mockContentlet.identifier);
        });

        it('should set state properly', () => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            service.getContentById.mockReturnValue(of(mockContentlet));

            store.getAssetData(mockContentlet.identifier);
            expect(store.value()).toBe(mockContentlet.identifier);
            expect(store.fileStatus()).toBe('preview');
            expect(store.uploadedFile()).toEqual({
                source: 'contentlet',
                file: mockContentlet
            });
        });

        it('should set state properly with an error calling getAssetData', () => {
            service.getContentById.mockReturnValue(throwError('error'));

            store.getAssetData('id');

            expect(service.getContentById).toHaveBeenCalledWith('id');
            expect(store.fileStatus()).toBe('init');
            expect(store.uiMessage()).toEqual(getUiMessage('SERVER_ERROR'));
        });
    });
});
