import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { ControlContainer } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import { DotDropZoneComponent, DropZoneErrorType, DropZoneFileEvent } from '@dotcms/ui';

import { DotFileFieldPreviewComponent } from './components/dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from './components/dot-file-field-ui-message/dot-file-field-ui-message.component';
import { DotEditContentFileFieldComponent } from './dot-edit-content-file-field.component';
import { DotFileFieldUploadService } from './services/upload-file/upload-file.service';
import { FileFieldStore } from './store/file-field.store';

import {
    BINARY_FIELD_MOCK,
    createFormGroupDirectiveMock,
    FILE_FIELD_MOCK,
    IMAGE_FIELD_MOCK,
    NEW_FILE_MOCK
} from '../../utils/mocks';

describe('DotEditContentFileFieldComponent', () => {
    let spectator: Spectator<DotEditContentFileFieldComponent>;
    let store: InstanceType<typeof FileFieldStore>;
    let uploadService: SpyObject<DotFileFieldUploadService>;

    const createComponent = createComponentFactory({
        component: DotEditContentFileFieldComponent,
        detectChanges: false,
        componentProviders: [FileFieldStore, mockProvider(DotFileFieldUploadService)],
        providers: [provideHttpClient(), mockProvider(DotMessageService)],
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ]
    });

    describe('FileField', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    field: FILE_FIELD_MOCK
                } as unknown
            });
            store = spectator.inject(FileFieldStore, true);
            uploadService = spectator.inject(DotFileFieldUploadService, true);
        });

        it('should be created', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have a DotDropZoneComponent and DotFileFieldUiMessageComponent', () => {
            spectator.detectChanges();

            expect(spectator.query(DotDropZoneComponent)).toBeTruthy();
            expect(spectator.query(DotFileFieldUiMessageComponent)).toBeTruthy();
        });

        it('should show the proper actions', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('action-import-from-url'))).toBeTruthy();
            expect(spectator.query(byTestId('action-existing-file'))).toBeTruthy();
            expect(spectator.query(byTestId('action-new-file'))).toBeTruthy();
            expect(spectator.query(byTestId('action-generate-with-ai'))).toBeFalsy();
        });

        it('should call initLoad with proper params', () => {
            const spyInitLoad = jest.spyOn(store, 'initLoad');

            spectator.detectChanges();

            expect(spyInitLoad).toHaveBeenCalledTimes(1);
            expect(spyInitLoad).toHaveBeenCalledWith({
                fieldVariable: FILE_FIELD_MOCK.variable,
                inputType: FILE_FIELD_MOCK.fieldType
            });
        });

        it('should call getAssetData when an value is set', () => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            uploadService.getContentById.mockReturnValue(of(mockContentlet));

            const spyGetAssetData = jest.spyOn(store, 'getAssetData');

            spectator.component.writeValue(mockContentlet.identifier);

            expect(spyGetAssetData).toHaveBeenCalledTimes(1);
            expect(spyGetAssetData).toHaveBeenCalledWith(mockContentlet.identifier);
        });

        it('should does not call getAssetData when an null value', () => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            uploadService.getContentById.mockReturnValue(of(mockContentlet));

            const spyGetAssetData = jest.spyOn(store, 'getAssetData');

            spectator.component.writeValue(null);

            expect(spyGetAssetData).not.toHaveBeenCalled();
        });

        it('should have a preview with a proper content', () => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            uploadService.getContentById.mockReturnValue(of(mockContentlet));

            spectator.component.writeValue(mockContentlet.identifier);

            spectator.detectChanges();

            expect(spectator.query(DotFileFieldPreviewComponent)).toBeTruthy();
        });

        describe('fileDropped event', () => {
            it('should call to handleUploadFile when and proper file', () => {
                const mockContentlet = NEW_FILE_MOCK.entity;
                uploadService.uploadDotAsset.mockReturnValue(of(mockContentlet));

                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

                const mockEvent: DropZoneFileEvent = {
                    file: new File([''], 'filename', { type: 'text/html' }),
                    validity: {
                        fileTypeMismatch: false,
                        maxFileSizeExceeded: false,
                        multipleFilesDropped: false,
                        errorsType: [],
                        valid: true
                    }
                };
                spectator.detectChanges();

                spectator.triggerEventHandler(DotDropZoneComponent, 'fileDropped', mockEvent);

                expect(spyHandleUploadFile).toHaveBeenCalledTimes(1);
                expect(spyHandleUploadFile).toHaveBeenCalledWith(mockEvent.file);
            });

            it('should not call to handleUploadFile when a null file', () => {
                const mockContentlet = NEW_FILE_MOCK.entity;
                uploadService.uploadDotAsset.mockReturnValue(of(mockContentlet));

                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

                const mockEvent: DropZoneFileEvent = {
                    file: null,
                    validity: {
                        fileTypeMismatch: false,
                        maxFileSizeExceeded: false,
                        multipleFilesDropped: false,
                        errorsType: [],
                        valid: true
                    }
                };
                spectator.detectChanges();

                spectator.triggerEventHandler(DotDropZoneComponent, 'fileDropped', mockEvent);

                expect(spyHandleUploadFile).not.toHaveBeenCalled();
            });

            it('should set a proper error message with a invalid file', () => {
                const mockContentlet = NEW_FILE_MOCK.entity;
                uploadService.uploadDotAsset.mockReturnValue(of(mockContentlet));

                const spySetUIMessage = jest.spyOn(store, 'setUIMessage');

                const mockEvent: DropZoneFileEvent = {
                    file: new File([''], 'filename', { type: 'text/html' }),
                    validity: {
                        fileTypeMismatch: true,
                        maxFileSizeExceeded: false,
                        multipleFilesDropped: false,
                        errorsType: [DropZoneErrorType.MAX_FILE_SIZE_EXCEEDED],
                        valid: false
                    }
                };
                spectator.detectChanges();

                spectator.triggerEventHandler(DotDropZoneComponent, 'fileDropped', mockEvent);

                expect(spySetUIMessage).toHaveBeenCalled();
            });
        });

        describe('fileSelected event', () => {
            it('should call to fileSelected with proper file', () => {
                const mockContentlet = NEW_FILE_MOCK.entity;
                uploadService.uploadDotAsset.mockReturnValue(of(mockContentlet));

                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

                const file = new File([''], 'filename', { type: 'text/html' });
                spectator.component.fileSelected([file] as unknown as FileList);

                expect(spyHandleUploadFile).toHaveBeenCalledTimes(1);
                expect(spyHandleUploadFile).toHaveBeenCalledWith(file);
            });

            it('should not call to fileSelected when a null file', () => {
                const mockContentlet = NEW_FILE_MOCK.entity;
                uploadService.uploadDotAsset.mockReturnValue(of(mockContentlet));

                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');
                spectator.component.fileSelected([] as unknown as FileList);

                expect(spyHandleUploadFile).not.toHaveBeenCalled();
            });
        });
    });

    describe('ImageField', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        field: IMAGE_FIELD_MOCK
                    } as unknown
                }))
        );

        it('should be created', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have a DotDropZoneComponent', () => {
            spectator.detectChanges();

            expect(spectator.query(DotDropZoneComponent)).toBeTruthy();
        });

        it('should show the proper actions', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('action-import-from-url'))).toBeTruthy();
            expect(spectator.query(byTestId('action-existing-file'))).toBeTruthy();
            expect(spectator.query(byTestId('action-new-file'))).toBeFalsy();
            expect(spectator.query(byTestId('action-generate-with-ai'))).toBeTruthy();
        });
    });

    describe('BinaryField', () => {
        beforeEach(
            () =>
                (spectator = createComponent({
                    props: {
                        field: BINARY_FIELD_MOCK
                    } as unknown
                }))
        );

        it('should be created', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have a DotDropZoneComponent', () => {
            spectator.detectChanges();

            expect(spectator.query(DotDropZoneComponent)).toBeTruthy();
        });

        it('should show the proper actions', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('action-import-from-url'))).toBeTruthy();
            expect(spectator.query(byTestId('action-existing-file'))).toBeFalsy();
            expect(spectator.query(byTestId('action-new-file'))).toBeTruthy();
            expect(spectator.query(byTestId('action-generate-with-ai'))).toBeTruthy();
        });
    });
});
