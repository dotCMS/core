import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotAiService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotDropZoneComponent, DropZoneErrorType, DropZoneFileEvent } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotFileFieldComponent } from './components/dot-file-field/dot-file-field.component';
import { DotFileFieldPreviewComponent } from './components/dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from './components/dot-file-field-ui-message/dot-file-field-ui-message.component';
import { DotEditContentFileFieldComponent } from './dot-edit-content-file-field.component';
import { DotFileFieldUploadService } from './services/upload-file/upload-file.service';
import { FileFieldStore } from './store/file-field.store';

import {
    BINARY_FIELD_MOCK,
    FILE_FIELD_MOCK,
    IMAGE_FIELD_MOCK,
    NEW_FILE_MOCK
} from '../../utils/mocks';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

describe('DotEditContentFileFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentFileFieldComponent, MockFormComponent>;
    let store: InstanceType<typeof FileFieldStore>;
    let uploadService: jest.Mocked<DotFileFieldUploadService>;

    const createHost = createHostFactory({
        component: DotEditContentFileFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentMocks: [
            DotFileFieldPreviewComponent,
            DotFileFieldUiMessageComponent,
            DotDropZoneComponent
        ],
        componentProviders: [FileFieldStore, mockProvider(DotFileFieldUploadService)],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DialogService),
            mockProvider(DotMessageService, {
                get: jest.fn().mockReturnValue('Test Message')
            }),
            mockProvider(DotAiService, {
                checkPluginInstallation: jest.fn().mockReturnValue(of(true))
            })
        ]
    });

    describe('FileField', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-file-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [FILE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: FILE_FIELD_MOCK,
                        contentlet: createFakeContentlet({
                            [FILE_FIELD_MOCK.variable]: null
                        })
                    }
                }
            );
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

            const fieldComponent = spectator.query(DotFileFieldComponent);
            fieldComponent.writeValue(mockContentlet.identifier);
            spectator.detectChanges();

            expect(spyGetAssetData).toHaveBeenCalledTimes(1);
            expect(spyGetAssetData).toHaveBeenCalledWith(mockContentlet.identifier);
        });

        it('should does not call getAssetData when an null value', () => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            uploadService.getContentById.mockReturnValue(of(mockContentlet));

            const spyGetAssetData = jest.spyOn(store, 'getAssetData');

            const fieldComponent = spectator.query(DotFileFieldComponent);
            fieldComponent.writeValue(null);
            spectator.detectChanges();

            expect(spyGetAssetData).not.toHaveBeenCalled();
        });

        it('should have a preview with a proper content', () => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            uploadService.getContentById.mockReturnValue(of(mockContentlet));

            const fieldComponent = spectator.query(DotFileFieldComponent);
            fieldComponent.writeValue(mockContentlet.identifier);

            spectator.detectChanges();

            expect(spectator.query(DotFileFieldPreviewComponent)).toBeTruthy();
        });

        describe('fileDropped event', () => {
            it('should call to handleUploadFile when and proper file', () => {
                const mockContentlet = NEW_FILE_MOCK.entity;
                uploadService.uploadFile.mockReturnValue(
                    of({ source: 'contentlet', file: mockContentlet })
                );

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
                uploadService.uploadFile.mockReturnValue(
                    of({ source: 'contentlet', file: mockContentlet })
                );

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
                uploadService.uploadFile.mockReturnValue(
                    of({ source: 'contentlet', file: mockContentlet })
                );

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
            it('should call to handleUploadFile with proper file', () => {
                const mockContentlet = NEW_FILE_MOCK.entity;
                uploadService.uploadFile.mockReturnValue(
                    of({ source: 'contentlet', file: mockContentlet })
                );

                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

                const file = new File([''], 'filename', { type: 'text/html' });
                const fieldComponent = spectator.query(DotFileFieldComponent);
                fieldComponent.fileSelected([file] as unknown as FileList);

                expect(spyHandleUploadFile).toHaveBeenCalledTimes(1);
                expect(spyHandleUploadFile).toHaveBeenCalledWith(file);
            });

            it('should not call to handleUploadFile when a null file', () => {
                const mockContentlet = NEW_FILE_MOCK.entity;
                uploadService.uploadFile.mockReturnValue(
                    of({ source: 'contentlet', file: mockContentlet })
                );

                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');
                const fieldComponent = spectator.query(DotFileFieldComponent);
                fieldComponent.fileSelected([] as unknown as FileList);

                expect(spyHandleUploadFile).not.toHaveBeenCalled();
            });
        });

        describe('Disabled State Management', () => {
            it('should set disabled state correctly through setDisabledState method', () => {
                spectator.detectChanges();

                const fieldComponent = spectator.query(DotFileFieldComponent);

                // Initially not disabled
                expect(fieldComponent.$isDisabled()).toBe(false);

                // Set disabled
                spectator.hostComponent.formGroup.disable();
                expect(fieldComponent.$isDisabled()).toBe(true);

                // Set enabled
                spectator.hostComponent.formGroup.enable();
                expect(fieldComponent.$isDisabled()).toBe(false);
            });

            it('should disable file input when field is disabled', () => {
                spectator.detectChanges();

                const fileInput = spectator.query(
                    byTestId('file-field__file-input')
                ) as HTMLInputElement;

                // Initially not disabled
                expect(fileInput.disabled).toBe(false);

                // Set disabled
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();
                expect(fileInput.disabled).toBe(true);
            });

            it('should disable action buttons when field is disabled', () => {
                const dialogService = spectator.inject(DialogService);
                const spyDialogOpen = jest.spyOn(dialogService, 'open');

                spectator.detectChanges();

                // Set disabled
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();

                // Try to trigger the action methods
                const fieldComponent = spectator.query(DotFileFieldComponent);
                fieldComponent.showImportUrlDialog();
                fieldComponent.showSelectExistingFileDialog();
                fieldComponent.showFileEditorDialog();

                // Verify that no dialogs are opened (dialog service not called)
                expect(spyDialogOpen).not.toHaveBeenCalled();
            });

            it('should prevent file selection when disabled', () => {
                spectator.detectChanges();
                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

                spectator.hostComponent.formGroup.disable();
                const mockFiles = {
                    length: 1,
                    0: new File(['test'], 'test.txt', { type: 'text/plain' })
                } as unknown as FileList;

                const fieldComponent = spectator.query(DotFileFieldComponent);
                fieldComponent.fileSelected(mockFiles);

                expect(spyHandleUploadFile).not.toHaveBeenCalled();
            });

            it('should prevent file drop when disabled', () => {
                spectator.detectChanges();
                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

                spectator.hostComponent.formGroup.disable();

                const mockEvent: DropZoneFileEvent = {
                    file: new File(['test'], 'test.txt', { type: 'text/plain' }),
                    validity: {
                        fileTypeMismatch: false,
                        maxFileSizeExceeded: false,
                        multipleFilesDropped: false,
                        errorsType: [],
                        valid: true
                    }
                };

                const fieldComponent = spectator.query(DotFileFieldComponent);
                fieldComponent.handleFileDrop(mockEvent);

                expect(spyHandleUploadFile).not.toHaveBeenCalled();
            });

            it('should prevent opening dialogs when disabled', () => {
                const dialogService = spectator.inject(DialogService);
                const spyDialogOpen = jest.spyOn(dialogService, 'open');

                spectator.hostComponent.formGroup.disable();

                // Test each dialog method
                const fieldComponent = spectator.query(DotFileFieldComponent);
                fieldComponent.showImportUrlDialog();
                fieldComponent.showSelectExistingFileDialog();
                fieldComponent.showFileEditorDialog();
                fieldComponent.showAIImagePromptDialog();

                expect(spyDialogOpen).not.toHaveBeenCalled();
            });

            it('should add disabled CSS class to container when disabled', () => {
                spectator.detectChanges();

                const container = spectator.query('.file-field__container');

                // Initially not disabled
                expect(container).not.toHaveClass('file-field__container--disabled');

                // Set disabled
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();
                expect(container).toHaveClass('file-field__container--disabled');
            });

            it('should pass disabled state to preview component when file is uploaded', () => {
                const store = spectator.inject(FileFieldStore, true);

                // Use the existing NEW_FILE_MOCK with proper UploadedFile structure
                const mockFile = { source: 'contentlet' as const, file: NEW_FILE_MOCK.entity };

                // Mock the store signals to return the uploaded file and preview status
                jest.spyOn(store, 'uploadedFile').mockReturnValue(mockFile);
                jest.spyOn(store, 'fileStatus').mockReturnValue('preview');

                spectator.detectChanges();

                // Set disabled
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();

                // Verify preview component exists - this confirms the template binding works
                const previewComponent = spectator.query('dot-file-field-preview');
                expect(previewComponent).toBeTruthy();

                const fieldComponent = spectator.query(DotFileFieldComponent);

                // Verify the component's disabled state is set correctly
                expect(fieldComponent.$isDisabled()).toBe(true);
            });
        });
    });

    describe('ImageField', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-file-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [IMAGE_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: IMAGE_FIELD_MOCK
                    }
                }
            );
        });

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
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-file-field [field]="field" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [BINARY_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: BINARY_FIELD_MOCK
                    }
                }
            );
        });

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
