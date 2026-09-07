import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotAiService,
    DotContentletService,
    DotMessageService,
    DotSiteService,
    DotUploadFileService,
    DotUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSContentTypeField, DotCMSContentlet, DotSite } from '@dotcms/dotcms-models';
import {
    ASSET_PICKER_LAUNCHER,
    AngularAssetPickerLauncher,
    DotAssetPickerComponent,
    DotAssetPickerConfig,
    DotDropZoneComponent,
    DropZoneErrorType,
    DropZoneFileEvent
} from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotFileFieldComponent } from './components/dot-file-field/dot-file-field.component';
import { DotFileFieldPreviewComponent } from './components/dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from './components/dot-file-field-ui-message/dot-file-field-ui-message.component';
import {
    LegacyDialogImageEditorLauncher,
    LegacyDojoImageEditorLauncher
} from './services/image-editor';
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
    open: jest.fn().mockReturnValue(of(null))
};

/** The AssetPicker needs a site to browse. */
const SITE_MOCK: DotSite = {
    identifier: 'site-1',
    hostname: 'demo.dotcms.com',
    aliases: null,
    archived: false
};

describe('DotFileFieldComponent', () => {
    let spectator: SpectatorHost<DotFileFieldComponent, MockFormComponent>;
    let store: InstanceType<typeof FileFieldStore>;
    let uploadService: jest.Mocked<DotFileFieldUploadService>;
    let dialogLauncher: LegacyDialogImageEditorLauncher;
    let dojoLauncher: LegacyDojoImageEditorLauncher;

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
            mockProvider(DotSiteService, {
                getCurrentSite: jest.fn().mockReturnValue(of(SITE_MOCK))
            }),
            FileFieldStore,
            DialogService,
            DotFileFieldUploadService,
            LegacyDialogImageEditorLauncher,
            LegacyDojoImageEditorLauncher,
            mockProvider(DotWorkflowActionsFireService),
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
            }),
            // Angular Edit Content host: the launcher is what makes "Select Existing File" open the
            // AssetPicker. Its legacy-host counterpart is
            // `components/dot-file-field/dot-file-field.component.legacy-picker.spec.ts`.
            { provide: ASSET_PICKER_LAUNCHER, useClass: AngularAssetPickerLauncher }
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
        dialogLauncher = spectator.inject(LegacyDialogImageEditorLauncher, true);
        dojoLauncher = spectator.inject(LegacyDojoImageEditorLauncher, true);
        jest.spyOn(dialogLauncher, 'open').mockImplementation(mockLauncher.open);
        jest.spyOn(dojoLauncher, 'open').mockImplementation(mockLauncher.open);
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
                inputType: FILE_FIELD_MOCK.fieldType,
                systemOptionsOverrides: {}
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

                expect(container).not.toHaveClass('dot-file-field__container--disabled');

                spectator.component.setDisabledState(true);
                spectator.detectChanges();
                expect(container).toHaveClass('dot-file-field__container--disabled');
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

    describe('BinaryField form value sync (handleStoreValueChange)', () => {
        const savedValue = '/dA/abc123/binaryField/document.pdf';
        const contentlet = createFakeContentlet({
            [BINARY_FIELD_MOCK.variable]: savedValue,
            binaryFieldMetaData: {
                name: 'document.pdf',
                title: 'document.pdf',
                editableAsText: false
            }
        });

        it('should not call onChange with empty string when reopening saved content', () => {
            setup(BINARY_FIELD_MOCK, contentlet);

            const onChange = jest.fn();
            spectator.component.registerOnChange(onChange);

            spectator.component.writeValue(savedValue);
            spectator.detectChanges();

            expect(onChange).not.toHaveBeenCalledWith('');
            expect(onChange).not.toHaveBeenCalled();
        });

        it('should call onChange when the user uploads a new file', () => {
            setup(BINARY_FIELD_MOCK, contentlet);

            const onChange = jest.fn();
            spectator.component.registerOnChange(onChange);

            spectator.component.writeValue(savedValue);
            spectator.detectChanges();

            onChange.mockClear();
            // Directly patch store value to trigger handleStoreValueChange,
            // avoiding the preview-component rendering path which requires a
            // complete UploadedFile shape that mocks cannot easily satisfy.
            store.setValue('new-temp-id');
            spectator.detectChanges();

            expect(onChange).toHaveBeenCalledWith('new-temp-id');
        });

        it('should sync writeValue to the store immediately', () => {
            setup(BINARY_FIELD_MOCK, contentlet);

            const spySetValue = jest.spyOn(store, 'setValue');

            spectator.component.writeValue(savedValue);

            expect(spySetValue).toHaveBeenCalledWith(savedValue);
            expect(store.value()).toBe(savedValue);
        });
    });

    describe('Edit image gating ($canEditImage)', () => {
        afterEach(() => mockLauncher.open.mockClear());

        const setImagePreview = (isImage: boolean) =>
            store.setPreviewFile({
                source: 'temp',
                file: { id: 'temp-1', metadata: { isImage } }
            } as never);

        it('should be false when enableImageEditor is false even for a binary image', () => {
            spectator = createHost(
                `<dot-file-field [field]="field" [contentlet]="contentlet" [hasError]="false" [enableImageEditor]="false" />`,
                {
                    hostProps: {
                        field: BINARY_FIELD_MOCK,
                        contentlet: createFakeContentlet({ [BINARY_FIELD_MOCK.variable]: null })
                    }
                }
            );
            store = spectator.component.store;
            spectator.detectChanges();

            setImagePreview(true);

            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('should be false for a Binary field when there is no previewed file', () => {
            setup(BINARY_FIELD_MOCK);
            spectator.detectChanges();

            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('should be true for a Binary field when the file is an image', () => {
            setup(BINARY_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(true);

            expect(spectator.component.$canEditImage()).toBe(true);
        });

        it('should be false for a Binary field when the file is not an image', () => {
            setup(BINARY_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(false);

            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('should be false for an Image field even when the file is an image', () => {
            setup(IMAGE_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(true);

            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('should be false for a File field even when the file is an image', () => {
            setup(FILE_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(true);

            expect(spectator.component.$canEditImage()).toBe(false);
        });

        it('should open the dialog launcher and apply the returned temp file on edit', () => {
            const tempFile = { id: 'edited-temp', metadata: { isImage: true } };
            mockLauncher.open.mockReturnValue(of(tempFile));
            setup(BINARY_FIELD_MOCK);
            spectator.detectChanges();

            setImagePreview(true);

            const spyApply = jest.spyOn(store, 'applyEditedImage').mockImplementation();

            spectator.component.onEditImage();

            expect(dialogLauncher.open).toHaveBeenCalled();
            expect(dojoLauncher.open).not.toHaveBeenCalled();
            expect(spyApply).toHaveBeenCalled();
        });

        it('should open the Dojo launcher when useLegacyDojoImageEditor is true', () => {
            const tempFile = { id: 'edited-temp', metadata: { isImage: true } };
            mockLauncher.open.mockReturnValue(of(tempFile));
            spectator = createHost(
                `<dot-file-field [field]="field" [contentlet]="contentlet" [hasError]="false" [useLegacyDojoImageEditor]="true" />`,
                {
                    hostProps: {
                        field: BINARY_FIELD_MOCK,
                        contentlet: createFakeContentlet({ [BINARY_FIELD_MOCK.variable]: null })
                    }
                }
            );
            store = spectator.component.store;
            dialogLauncher = spectator.inject(LegacyDialogImageEditorLauncher, true);
            dojoLauncher = spectator.inject(LegacyDojoImageEditorLauncher, true);
            jest.spyOn(dialogLauncher, 'open').mockImplementation(mockLauncher.open);
            jest.spyOn(dojoLauncher, 'open').mockImplementation(mockLauncher.open);
            spectator.detectChanges();

            setImagePreview(true);
            spectator.component.onEditImage();

            expect(dojoLauncher.open).toHaveBeenCalled();
            expect(dialogLauncher.open).not.toHaveBeenCalled();
        });
    });

    describe('select existing asset (AssetPicker)', () => {
        /** The site signal is created once by the factory, so a test that nulls it would leak. */
        /**
         * `DotSiteService` is root-provided and mocked once for the file, so re-seed the return
         * value per test rather than mutating a signal.
         */
        const setSite = (site: DotSite | null) =>
            (spectator.inject(DotSiteService).getCurrentSite as jest.Mock).mockReturnValue(
                site ? of(site) : of(null)
            );

        const openPicker = (field: DotCMSContentTypeField, contentlet?: DotCMSContentlet) => {
            setup(field, contentlet);
            setSite(SITE_MOCK);
            spectator.detectChanges();

            const dialogService = spectator.inject(DialogService, true);
            const spyOpen = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose: of(undefined),
                close: jest.fn()
            } as unknown as DynamicDialogRef);

            spectator.component.showSelectExistingFileDialog();

            return spyOpen;
        };

        /** The picker config the dialog was opened with. */
        const configOf = (spyOpen: jest.SpyInstance): DotAssetPickerConfig =>
            spyOpen.mock.calls[0][1].data as DotAssetPickerConfig;

        /** The `DialogService.open` options, minus the picker config. */
        const optionsOf = (spyOpen: jest.SpyInstance) => spyOpen.mock.calls[0][1];

        it('should open the AssetPicker, not the browser selector', () => {
            const spyOpen = openPicker(FILE_FIELD_MOCK);

            expect(spyOpen).toHaveBeenCalledWith(
                DotAssetPickerComponent,
                expect.objectContaining({ data: expect.anything() })
            );
        });

        describe('dialog chrome', () => {
            it('should hide PrimeNG’s header so the picker can render its own', () => {
                const options = optionsOf(openPicker(FILE_FIELD_MOCK));

                expect(options.showHeader).toBe(false);
                expect(options.header).toBeUndefined();
            });

            it('should not autofocus on open', () => {
                // Autofocus lands on the picker's search input and paints the theme's focus halo
                // the moment the dialog appears.
                expect(optionsOf(openPicker(FILE_FIELD_MOCK)).focusOnShow).toBe(false);
            });

            it('should let the picker fill the dialog so full screen can grow it', () => {
                const options = optionsOf(openPicker(FILE_FIELD_MOCK));

                expect(options.height).toBeTruthy();
                expect(options.contentStyle).toEqual(
                    expect.objectContaining({ height: '100%', padding: '0' })
                );
            });

            it('should size the windowed dialog without an inline max-width', () => {
                // An inline max-width survives `.p-dialog-maximized` (which only overrides
                // width/height), so it would clamp the dialog once it goes full screen.
                const options = optionsOf(openPicker(FILE_FIELD_MOCK));

                expect(options.width).toBe('min(90vw, 114rem)');
                expect(options.style).toBeUndefined();
            });

            it('should enable PrimeNG’s maximized state without adding its button', () => {
                const options = optionsOf(openPicker(FILE_FIELD_MOCK));

                expect(options.maximizable).toBe(true);
                // PrimeNG renders the maximize button inside the header we hid.
                expect(options.showHeader).toBe(false);
            });
        });

        describe('File field', () => {
            it('should open in file mode: no type or mime restriction', () => {
                const config = configOf(openPicker(FILE_FIELD_MOCK));

                expect(config.baseTypes).toBeUndefined();
                expect(config.mimeTypes).toBeUndefined();
            });

            it('should pass the site being edited', () => {
                const config = configOf(openPicker(FILE_FIELD_MOCK));

                expect(config.site).toEqual(SITE_MOCK);
            });

            it('should carry the "Add File" title in the config', () => {
                const config = configOf(openPicker(FILE_FIELD_MOCK));

                // The mocked message service returns a fixed string, so the assertion that
                // distinguishes File from Image is which key was resolved.
                expect(spectator.inject(DotMessageService).get).toHaveBeenCalledWith(
                    'dot.asset.picker.header.file'
                );
                expect(config.title).toBeTruthy();
            });
        });

        describe('Image field', () => {
            it('should restrict to the dotAsset and File Asset base types', () => {
                const config = configOf(openPicker(IMAGE_FIELD_MOCK));

                expect(config.baseTypes).toEqual(['DOTASSET', 'FILEASSET']);
            });

            it('should apply the image mime restriction', () => {
                const config = configOf(openPicker(IMAGE_FIELD_MOCK));

                expect(config.mimeTypes).toEqual(['image/*']);
            });

            it('should carry the "Add Image" title in the config', () => {
                const config = configOf(openPicker(IMAGE_FIELD_MOCK));

                expect(spectator.inject(DotMessageService).get).toHaveBeenCalledWith(
                    'dot.asset.picker.header.image'
                );
                expect(config.title).toBeTruthy();
            });
        });

        describe('locale', () => {
            it("should use the contentlet's language when editing", () => {
                const config = configOf(
                    openPicker(FILE_FIELD_MOCK, createFakeContentlet({ languageId: 2 }))
                );

                expect(config.languageId).toBe('2');
            });
        });

        describe('guards', () => {
            it('should not open when no site has resolved yet', () => {
                setup(FILE_FIELD_MOCK);
                spectator.detectChanges();

                // Cold start: no site resolves.
                setSite(null);

                const dialogService = spectator.inject(DialogService, true);
                const spyOpen = jest.spyOn(dialogService, 'open');

                spectator.component.showSelectExistingFileDialog();

                expect(spyOpen).not.toHaveBeenCalled();
            });

            it('should not stack a second picker while the site lookup is still in flight', () => {
                // The site lookup is what makes opening asynchronous. `#dialogRef` is still null
                // while it runs, so without a pending flag a double click opens two dialogs — and
                // only the second is reachable, leaving the first live and able to write a value.
                const site$ = new Subject<DotSite>();
                setup(FILE_FIELD_MOCK);
                (spectator.inject(DotSiteService).getCurrentSite as jest.Mock).mockReturnValue(
                    site$.asObservable()
                );
                spectator.detectChanges();

                const spyOpen = jest
                    .spyOn(spectator.inject(DialogService, true), 'open')
                    .mockReturnValue({
                        onClose: of(undefined),
                        close: jest.fn()
                    } as unknown as DynamicDialogRef);

                spectator.component.showSelectExistingFileDialog();
                spectator.component.showSelectExistingFileDialog();
                site$.next(SITE_MOCK);

                expect(spyOpen).toHaveBeenCalledTimes(1);
            });

            it('should open again after the picker closed', () => {
                setup(FILE_FIELD_MOCK);
                setSite(SITE_MOCK);
                spectator.detectChanges();

                // Released on cancel too, or the button is dead for the rest of the session.
                const spyOpen = jest
                    .spyOn(spectator.inject(DialogService, true), 'open')
                    .mockReturnValue({
                        onClose: of(undefined),
                        close: jest.fn()
                    } as unknown as DynamicDialogRef);

                spectator.component.showSelectExistingFileDialog();
                spectator.component.showSelectExistingFileDialog();

                expect(spyOpen).toHaveBeenCalledTimes(2);
            });

            it('should allow a retry after the site lookup failed', () => {
                setup(FILE_FIELD_MOCK);
                const siteService = spectator.inject(DotSiteService);
                const getCurrentSite = siteService.getCurrentSite as jest.Mock;
                getCurrentSite.mockReturnValue(throwError(() => new Error('no site')));
                spectator.detectChanges();

                // The mock is shared across this file's tests, so only calls from here on count.
                getCurrentSite.mockClear();

                spectator.component.showSelectExistingFileDialog();
                spectator.component.showSelectExistingFileDialog();

                // The guard has to be released on error too, or the button dies for the session.
                expect(getCurrentSite).toHaveBeenCalledTimes(2);
            });
        });

        describe('close contract', () => {
            it('should set the preview from the returned contentlet', () => {
                const asset = createFakeContentlet({ identifier: 'asset-1' });
                setup(FILE_FIELD_MOCK);
                setSite(SITE_MOCK);
                spectator.detectChanges();

                const spySetPreview = jest.spyOn(spectator.component.store, 'setPreviewFile');
                jest.spyOn(spectator.inject(DialogService, true), 'open').mockReturnValue({
                    onClose: of(asset),
                    close: jest.fn()
                } as unknown as DynamicDialogRef);

                spectator.component.showSelectExistingFileDialog();

                expect(spySetPreview).toHaveBeenCalledWith({ source: 'contentlet', file: asset });
            });

            it('should leave the field untouched on cancel', () => {
                setup(FILE_FIELD_MOCK);
                setSite(SITE_MOCK);
                spectator.detectChanges();

                const spySetPreview = jest.spyOn(spectator.component.store, 'setPreviewFile');
                jest.spyOn(spectator.inject(DialogService, true), 'open').mockReturnValue({
                    onClose: of(undefined),
                    close: jest.fn()
                } as unknown as DynamicDialogRef);

                spectator.component.showSelectExistingFileDialog();

                expect(spySetPreview).not.toHaveBeenCalled();
            });
        });
    });
});
