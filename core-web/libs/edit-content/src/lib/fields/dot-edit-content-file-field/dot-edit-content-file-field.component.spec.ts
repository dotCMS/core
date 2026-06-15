import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import {
    DotAiService,
    DotContentletService,
    DotMessageService,
    DotUploadFileService,
    DotUploadService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotDropZoneComponent, DropZoneErrorType, DropZoneFileEvent } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotFileFieldComponent } from './components/dot-file-field/dot-file-field.component';
import { DotFileFieldPreviewComponent } from './components/dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from './components/dot-file-field-ui-message/dot-file-field-ui-message.component';
import { IMAGE_EDITOR_LAUNCHER } from './services/image-editor/image-editor-launcher.model';
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
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

const mockLauncher = {
    isAvailable: jest.fn().mockReturnValue(false),
    open: jest.fn().mockReturnValue(of(null))
};

describe('DotFileFieldComponent', () => {
    let spectator: SpectatorHost<DotFileFieldComponent, MockFormComponent>;
    let store: InstanceType<typeof FileFieldStore>;
    let uploadService: jest.Mocked<DotFileFieldUploadService>;

    const createHost = createHostFactory({
        component: DotFileFieldComponent,
        host: MockFormComponent,
        detectChanges: false,
        componentMocks: [
            DotFileFieldPreviewComponent,
            DotFileFieldUiMessageComponent,
            DotDropZoneComponent
        ],
        // The file field self-provides FileFieldStore + DotFileFieldUploadService.
        // We also provide them (and mock the upload service's transitive deps) at
        // the module level so the harness can resolve them, then spy per test.
        providers: [
            FileFieldStore,
            DialogService,
            DotFileFieldUploadService,
            { provide: IMAGE_EDITOR_LAUNCHER, useValue: mockLauncher },
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotUploadFileService),
            mockProvider(DotUploadService),
            mockProvider(DotContentletService),
            mockProvider(DotMessageService, {
                get: jest.fn().mockReturnValue('Test Message')
            }),
            mockProvider(DotAiService, {
                checkPluginInstallation: jest.fn().mockReturnValue(of(true))
            })
        ]
    });

    const setup = (field: DotCMSContentTypeField, contentlet?: DotCMSContentlet) => {
        spectator = createHost(
            `<dot-file-field [field]="field" [contentlet]="contentlet" [hasError]="false" />`,
            {
                hostProps: {
                    field,
                    contentlet: contentlet ?? createFakeContentlet({ [field.variable]: null })
                }
            }
        );
        store = spectator.component.store;
        uploadService = spectator.inject(
            DotFileFieldUploadService,
            true
        ) as jest.Mocked<DotFileFieldUploadService>;
    };

    describe('FileField', () => {
        beforeEach(() => setup(FILE_FIELD_MOCK));

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
            jest.spyOn(uploadService, 'getContentById').mockReturnValue(of(mockContentlet));

            const spyGetAssetData = jest.spyOn(store, 'getAssetData');

            spectator.component.writeValue(mockContentlet.identifier);
            spectator.detectChanges();

            expect(spyGetAssetData).toHaveBeenCalledTimes(1);
            expect(spyGetAssetData).toHaveBeenCalledWith(mockContentlet.identifier);
        });

        it('should does not call getAssetData when an null value', () => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            jest.spyOn(uploadService, 'getContentById').mockReturnValue(of(mockContentlet));

            const spyGetAssetData = jest.spyOn(store, 'getAssetData');

            spectator.component.writeValue(null);
            spectator.detectChanges();

            expect(spyGetAssetData).not.toHaveBeenCalled();
        });

        it('should have a preview with a proper content', () => {
            const mockContentlet = NEW_FILE_MOCK.entity;
            jest.spyOn(uploadService, 'getContentById').mockReturnValue(of(mockContentlet));

            spectator.component.writeValue(mockContentlet.identifier);

            spectator.detectChanges();

            expect(spectator.query(DotFileFieldPreviewComponent)).toBeTruthy();
        });

        describe('fileDropped event', () => {
            it('should call to handleUploadFile when and proper file', () => {
                const mockContentlet = NEW_FILE_MOCK.entity;
                jest.spyOn(uploadService, 'uploadFile').mockReturnValue(
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
                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

                const file = new File([''], 'filename', { type: 'text/html' });
                spectator.detectChanges();
                spectator.component.fileSelected([file] as unknown as FileList);

                expect(spyHandleUploadFile).toHaveBeenCalledTimes(1);
                expect(spyHandleUploadFile).toHaveBeenCalledWith(file);
            });

            it('should not call to handleUploadFile when a null file', () => {
                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');
                spectator.detectChanges();
                spectator.component.fileSelected([] as unknown as FileList);

                expect(spyHandleUploadFile).not.toHaveBeenCalled();
            });
        });

        describe('Disabled State Management', () => {
            it('should set disabled state correctly through setDisabledState method', () => {
                spectator.detectChanges();

                expect(spectator.component.$isDisabled()).toBe(false);

                spectator.component.setDisabledState(true);
                expect(spectator.component.$isDisabled()).toBe(true);

                spectator.component.setDisabledState(false);
                expect(spectator.component.$isDisabled()).toBe(false);
            });

            it('should disable file input when field is disabled', () => {
                spectator.detectChanges();

                const fileInput = spectator.query(
                    byTestId('file-field__file-input')
                ) as HTMLInputElement;

                expect(fileInput.disabled).toBe(false);

                spectator.component.setDisabledState(true);
                spectator.detectChanges();
                expect(fileInput.disabled).toBe(true);
            });

            it('should prevent file selection when disabled', () => {
                spectator.detectChanges();
                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

                spectator.component.setDisabledState(true);
                const mockFiles = {
                    length: 1,
                    0: new File(['test'], 'test.txt', { type: 'text/plain' })
                } as unknown as FileList;

                spectator.component.fileSelected(mockFiles);

                expect(spyHandleUploadFile).not.toHaveBeenCalled();
            });

            it('should prevent file drop when disabled', () => {
                spectator.detectChanges();
                const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

                spectator.component.setDisabledState(true);

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

                spectator.component.handleFileDrop(mockEvent);

                expect(spyHandleUploadFile).not.toHaveBeenCalled();
            });

            it('should prevent opening dialogs when disabled', () => {
                spectator.detectChanges();
                const dialogService = spectator.inject(DialogService, true);
                const spyDialogOpen = jest.spyOn(dialogService, 'open');

                spectator.component.setDisabledState(true);

                spectator.component.showImportUrlDialog();
                spectator.component.showSelectExistingFileDialog();
                spectator.component.showFileEditorDialog();
                spectator.component.showAIImagePromptDialog();

                expect(spyDialogOpen).not.toHaveBeenCalled();
            });

            it('should add disabled CSS class to container when disabled', () => {
                spectator.detectChanges();

                const container = spectator.query('.dot-file-field__container');

                expect(container).not.toHaveClass('opacity-50');

                spectator.component.setDisabledState(true);
                spectator.detectChanges();
                expect(container).toHaveClass('opacity-50');
            });
        });
    });

    describe('ImageField', () => {
        beforeEach(() => setup(IMAGE_FIELD_MOCK));

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
        beforeEach(() => setup(BINARY_FIELD_MOCK));

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

    describe('BinaryField contentlet hydration', () => {
        const contentlet = createFakeContentlet({
            [BINARY_FIELD_MOCK.variable]: 'my-file.txt',
            metaData: { name: 'my-file.txt', title: 'my-file.txt', editableAsText: false }
        });

        beforeEach(() => setup(BINARY_FIELD_MOCK, contentlet));

        it('should hydrate from the contentlet metadata instead of fetching an asset by id', () => {
            const spyGetAssetData = jest.spyOn(store, 'getAssetData');
            const spySetFromContentlet = jest.spyOn(store, 'setFileFromContentlet');

            spectator.detectChanges();

            expect(spyGetAssetData).not.toHaveBeenCalled();
            expect(spySetFromContentlet).toHaveBeenCalledWith({
                contentlet,
                fieldVariable: BINARY_FIELD_MOCK.variable,
                value: 'my-file.txt'
            });
        });
    });

    describe('Edit image gating ($canEditImage)', () => {
        afterEach(() => mockLauncher.isAvailable.mockReturnValue(false));

        const setImagePreview = (isImage: boolean) =>
            store.setPreviewFile({
                source: 'temp',
                file: { id: 'temp-1', metadata: { isImage } }
            } as never);

        it('should be false when the launcher is not available even for an image', () => {
            mockLauncher.isAvailable.mockReturnValue(false);
            setup(IMAGE_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(true);

            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('should be false when there is no previewed file', () => {
            mockLauncher.isAvailable.mockReturnValue(true);
            setup(IMAGE_FIELD_MOCK);
            spectator.detectChanges();

            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('should be true for an Image field when the file is an image', () => {
            mockLauncher.isAvailable.mockReturnValue(true);
            setup(IMAGE_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(true);

            expect(spectator.component.$canEditImage()).toBe(true);
        });

        it('should be true for a File field when the file is an image', () => {
            mockLauncher.isAvailable.mockReturnValue(true);
            setup(FILE_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(true);

            expect(spectator.component.$canEditImage()).toBe(true);
        });

        it('should be true for a Binary field when the file is an image', () => {
            mockLauncher.isAvailable.mockReturnValue(true);
            setup(BINARY_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(true);

            expect(spectator.component.$canEditImage()).toBe(true);
        });

        it('should be false when the previewed file is not an image', () => {
            mockLauncher.isAvailable.mockReturnValue(true);
            setup(FILE_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(false);

            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('should open the launcher and apply the returned temp file on edit', () => {
            const tempFile = { id: 'edited-temp', metadata: { isImage: true } };
            mockLauncher.isAvailable.mockReturnValue(true);
            mockLauncher.open.mockReturnValue(of(tempFile));
            setup(IMAGE_FIELD_MOCK);
            spectator.detectChanges();

            const spyApply = jest.spyOn(store, 'applyTempFile');

            spectator.component.onEditImage();

            expect(mockLauncher.open).toHaveBeenCalled();
            expect(spyApply).toHaveBeenCalledWith(tempFile);
        });
    });
});
