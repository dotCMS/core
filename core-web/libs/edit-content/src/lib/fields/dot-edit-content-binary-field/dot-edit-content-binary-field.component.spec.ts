import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import {
    byTestId,
    createComponentFactory,
    createHostFactory,
    Spectator,
    SpectatorHost,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { Component, NgZone } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import {
    ControlContainer,
    FormControl,
    FormGroup,
    FormGroupDirective,
    ReactiveFormsModule
} from '@angular/forms';
import { By } from '@angular/platform-browser';

import { ButtonModule, Button } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotAiService,
    DotLicenseService,
    DotMessageService,
    DotUploadService
} from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DropZoneErrorType, DropZoneFileEvent } from '@dotcms/ui';
import { dotcmsContentletMock } from '@dotcms/utils-testing';

import { DotBinaryFieldPreviewComponent } from './components/dot-binary-field-preview/dot-binary-field-preview.component';
import { DotEditContentBinaryFieldComponent } from './dot-edit-content-binary-field.component';
import { BinaryFieldMode, BinaryFieldStatus } from './interfaces';
import { DotBinaryFieldEditImageService } from './service/dot-binary-field-edit-image/dot-binary-field-edit-image.service';
import { DotBinaryFieldValidatorService } from './service/dot-binary-field-validator/dot-binary-field-validator.service';
import { DotBinaryFieldStore } from './store/binary-field.store';
import { getUiMessage } from './utils/binary-field-utils';
import { CONTENTTYPE_FIELDS_MESSAGE_MOCK, fileMetaData } from './utils/mock';

import { BINARY_FIELD_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

const TEMP_FILE_MOCK: DotCMSTempFile = {
    fileName: 'image.png',
    folder: '/images',
    id: '123456',
    image: true,
    length: 1000,
    referenceUrl:
        'https://images.unsplash.com/photo-1575936123452-b67c3203c357?auto=format&fit=crop&q=80&w=1000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8aW1hZ2V8ZW58MHx8MHx8fDA%3D',
    thumbnailUrl: 'image.png',
    mimeType: 'mimeType',
    metadata: fileMetaData
};

const file = new File([''], 'filename');
const validity = {
    valid: true,
    fileTypeMismatch: false,
    maxFileSizeExceeded: false,
    multipleFilesDropped: false,
    errorsType: [DropZoneErrorType.FILE_TYPE_MISMATCH]
};

const DROP_ZONE_FILE_EVENT: DropZoneFileEvent = {
    file,
    validity
};

const MOCK_DOTCMS_FILE = {
    ...dotcmsContentletMock,
    binaryField: '12345',
    baseType: 'CONTENT',
    binaryFieldMetaData: fileMetaData
};

describe('DotEditContentBinaryFieldComponent', () => {
    let spectator: Spectator<DotEditContentBinaryFieldComponent>;
    let store: DotBinaryFieldStore;

    let dotBinaryFieldEditImageService: SpyObject<DotBinaryFieldEditImageService>;
    let dotAiService: DotAiService;
    let ngZone: NgZone;

    const createComponent = createComponentFactory({
        component: DotEditContentBinaryFieldComponent,
        componentProviders: [
            DotBinaryFieldStore,
            DotBinaryFieldEditImageService,
            DotAiService,
            DialogService
        ],
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        providers: [
            provideHttpClient(),
            DotBinaryFieldValidatorService,
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
            },
            {
                provide: DotMessageService,
                useValue: CONTENTTYPE_FIELDS_MESSAGE_MOCK
            },
            FormGroupDirective
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false,
            props: {
                field: {
                    ...BINARY_FIELD_MOCK
                },
                contentlet: null
            }
        });
        store = spectator.inject(DotBinaryFieldStore, true);
        dotBinaryFieldEditImageService = spectator.inject(DotBinaryFieldEditImageService, true);
        dotAiService = spectator.inject(DotAiService, true);
        ngZone = spectator.inject(NgZone);
    });

    it('shouldnt show url import button if not setted in settings', () => {
        const importFromURLButton = spectator.query(byTestId('action-url-btn'));

        expect(importFromURLButton).toBeNull();
    });

    it('shouldnt show code editor button if not setted in settings', async () => {
        const codeEditorButton = spectator.query(byTestId('action-editor-btn'));

        expect(codeEditorButton).toBeNull();
    });

    it('should emit temp file', () => {
        const spyEmit = jest.spyOn(spectator.component.valueUpdated, 'emit');
        spectator.detectChanges();
        store.setTempFile(TEMP_FILE_MOCK);
        expect(spyEmit).toHaveBeenCalledWith({
            value: TEMP_FILE_MOCK.id,
            fileName: TEMP_FILE_MOCK.fileName
        });
    });

    it('should not emit new value is is equal to current value', () => {
        spectator.setInput('contentlet', MOCK_DOTCMS_FILE);
        const spyEmit = jest.spyOn(spectator.component.valueUpdated, 'emit');
        spectator.component.writeValue(MOCK_DOTCMS_FILE.binaryField);
        store.setValue(MOCK_DOTCMS_FILE.binaryField);
        spectator.detectChanges();
        expect(spyEmit).not.toHaveBeenCalled();
    });

    describe('Dropzone', () => {
        beforeEach(async () => {
            spectator.setInput('contentlet', MOCK_DOTCMS_FILE);
            spectator.detectChanges();
            store.setStatus(BinaryFieldStatus.INIT);
            await spectator.fixture.whenStable();
            spectator.detectChanges();
        });

        it('should show dropzone when status is INIT', () => {
            expect(spectator.query('dot-drop-zone')).toBeTruthy();
        });

        it('should handle file drop', () => {
            const spyUploadFile = jest.spyOn(store, 'handleUploadFile');
            const spyInvalidFile = jest.spyOn(store, 'invalidFile');
            const dropZone = spectator.fixture.debugElement.query(By.css('dot-drop-zone'));

            dropZone.triggerEventHandler('fileDropped', DROP_ZONE_FILE_EVENT);

            expect(spyUploadFile).toHaveBeenCalledWith(DROP_ZONE_FILE_EVENT.file);
            expect(spyInvalidFile).not.toHaveBeenCalled();
        });

        it('should handle file drop error', () => {
            const spyUploadFile = jest.spyOn(store, 'handleUploadFile');
            const spyInvalidFile = jest.spyOn(store, 'invalidFile');
            const dropZone = spectator.fixture.debugElement.query(By.css('dot-drop-zone'));

            dropZone.triggerEventHandler('fileDropped', {
                ...DROP_ZONE_FILE_EVENT,
                validity: {
                    ...DROP_ZONE_FILE_EVENT.validity,
                    fileTypeMismatch: true,
                    valid: false
                }
            });

            expect(spyInvalidFile).toHaveBeenCalledWith(
                getUiMessage('FILE_TYPE_MISMATCH', 'image/*, .html, .ts')
            );
            expect(spyUploadFile).not.toHaveBeenCalled();
        });

        it('should handle file dragover', () => {
            const dropZone = spectator.fixture.debugElement.query(By.css('dot-drop-zone'));
            const spyDropZoneActive = jest.spyOn(store, 'setDropZoneActive');
            dropZone.triggerEventHandler('fileDragOver', {});

            expect(spyDropZoneActive).toHaveBeenCalledWith(true);
        });

        it('should handle file dragleave', () => {
            const dropZone = spectator.fixture.debugElement.query(By.css('dot-drop-zone'));
            const spyDropZoneActive = jest.spyOn(store, 'setDropZoneActive');
            dropZone.triggerEventHandler('fileDragLeave', {});

            expect(spyDropZoneActive).toHaveBeenCalledWith(false);
        });

        it('should open file picker when click on choose file button', () => {
            const spyOpenFilePicker = jest.spyOn(spectator.component, 'openFilePicker');
            const spyInputFile = jest.spyOn(spectator.component.inputFile.nativeElement, 'click');
            const chooseFile = spectator.query(byTestId('choose-file-btn')) as HTMLButtonElement;
            chooseFile.click();
            expect(chooseFile.getAttribute('type')).toBe('button');
            expect(spyOpenFilePicker).toHaveBeenCalled();
            expect(spyInputFile).toHaveBeenCalled();
        });

        it('should handle file selection', () => {
            const spyUploadFile = jest.spyOn(store, 'handleUploadFile');
            const inputElement = spectator.fixture.debugElement.query(
                By.css('[data-testId="binary-field__file-input"]')
            ).nativeElement;
            const file = new File(['test'], 'test.png', { type: 'image/png' });
            const event = new Event('change');
            Object.defineProperty(event, 'target', { value: { files: [file] } });
            inputElement.dispatchEvent(event);

            expect(spyUploadFile).toHaveBeenCalledWith(file);
        });
    });

    describe('Preview', () => {
        beforeEach(async () => {
            store.setStatus(BinaryFieldStatus.PREVIEW);
            store.setTempFile(TEMP_FILE_MOCK);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should remove file and set INIT status when remove file ', async () => {
            const spyRemoveFile = jest.spyOn(store, 'removeFile');
            const dotBinaryPreviewFile = spectator.fixture.debugElement.query(
                By.css('[data-testId="preview"]')
            );

            dotBinaryPreviewFile.componentInstance.removeFile.emit();

            store.vm$.subscribe((state) => {
                expect(state).toEqual({
                    ...state,
                    status: BinaryFieldStatus.INIT,
                    value: '',
                    contentlet: null,
                    tempFile: null
                });
            });

            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const dropZone = spectator.fixture.debugElement.query(By.css('dot-drop-zone'));

            expect(dropZone).toBeTruthy();
            expect(spyRemoveFile).toHaveBeenCalled();
        });

        describe('Edit Image', () => {
            it('should open edit image dialog when click on edit image button', () => {
                spectator.detectChanges();
                const spy = jest.spyOn(dotBinaryFieldEditImageService, 'openImageEditor');
                spectator.triggerEventHandler(DotBinaryFieldPreviewComponent, 'editImage', null);
                expect(spy).toHaveBeenCalled();
            });

            it('should emit the tempId of the edited image', () => {
                // Needed because the openImageEditor method is using a DOM custom event
                ngZone.run(
                    fakeAsync(() => {
                        const spy = jest.spyOn(dotBinaryFieldEditImageService, 'openImageEditor');
                        const spyTempFile = jest.spyOn(store, 'setFileFromTemp');
                        const dotBinaryFieldPreviewComponent = spectator.fixture.debugElement.query(
                            By.css('dot-binary-field-preview')
                        );
                        dotBinaryFieldPreviewComponent.triggerEventHandler('editImage');
                        const customEvent = new CustomEvent(
                            `binaryField-tempfile-${BINARY_FIELD_MOCK.variable}`,
                            {
                                detail: { tempFile: TEMP_FILE_MOCK }
                            }
                        );
                        document.dispatchEvent(customEvent);

                        tick(1000);

                        expect(spy).toHaveBeenCalled();
                        expect(spyTempFile).toHaveBeenCalledWith(TEMP_FILE_MOCK);
                    })
                );
            });
        });
    });

    describe('Template', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should show dropzone when status is INIT', async () => {
            store.setStatus(BinaryFieldStatus.INIT);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            expect(spectator.query(byTestId('dropzone'))).toBeTruthy();
        });

        it('should show loading when status is UPLOADING', async () => {
            store.setStatus(BinaryFieldStatus.UPLOADING);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            expect(spectator.query(byTestId('loading'))).toBeTruthy();
        });

        it('should show preview when status is PREVIEW', async () => {
            store.setTempFile(TEMP_FILE_MOCK);
            spectator.detectChanges();

            await spectator.fixture.whenStable();

            expect(spectator.query(byTestId('preview'))).toBeTruthy();
        });
    });

    describe('systemOptions all false', () => {
        beforeEach(() => {
            const systemOptions = {
                allowURLImport: false,
                allowCodeWrite: false,
                allowGenerateImg: false
            };

            const JSONString = JSON.stringify(systemOptions);

            const newField = {
                ...BINARY_FIELD_MOCK,
                fieldVariables: [
                    ...BINARY_FIELD_MOCK.fieldVariables,
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        fieldId: '5df3f8fc49177c195740bcdc02ec2db7',
                        id: '1ff1ff05-b9fb-4239-ad3d-b2cfaa9a8406',
                        key: 'systemOptions',
                        value: JSONString
                    }
                ]
            };

            spectator = createComponent({
                detectChanges: false,
                props: {
                    field: newField,
                    contentlet: null
                }
            });
        });

        it('should show url import button if not setted in settings', () => {
            spectator.detectChanges();
            const importFromURLButton = spectator.query(byTestId('action-url-btn'));

            expect(importFromURLButton).toBeNull();
        });

        it('should show code editor button if not setted in settings', async () => {
            spectator.detectChanges();
            const codeEditorButton = spectator.query(byTestId('action-editor-btn'));

            expect(codeEditorButton).toBeNull();
        });

        it('should show code ai button if not setted in settings', async () => {
            spectator.detectChanges();
            const codeEditorButton = spectator.query(byTestId('action-ai-btn'));

            expect(codeEditorButton).toBeNull();
        });
    });

    describe('Ai option', () => {
        beforeEach(() => {
            const systemOptions = {
                allowURLImport: true,
                allowCodeWrite: true,
                allowGenerateImg: true
            };

            const JSONString = JSON.stringify(systemOptions);

            const newField = {
                ...BINARY_FIELD_MOCK,
                fieldVariables: [
                    ...BINARY_FIELD_MOCK.fieldVariables,
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        fieldId: '5df3f8fc49177c195740bcdc02ec2db7',
                        id: '1ff1ff05-b9fb-4239-ad3d-b2cfaa9a8406',
                        key: 'systemOptions',
                        value: JSONString
                    }
                ]
            };

            spectator = createComponent({
                detectChanges: false,
                props: {
                    field: newField,
                    contentlet: null
                }
            });
        });

        it('should show ai button', async () => {
            spectator.detectChanges();
            const codeEditorButton = spectator.query(byTestId('action-ai-btn'));
            expect(codeEditorButton).toBeTruthy();
        });

        it('should AI button is disabled when plugin is not installed', async () => {
            dotAiService.checkPluginInstallation = jest.fn().mockReturnValue(of(false));
            spectator.detectChanges();
            const buttons = spectator.queryAll(Button);
            const aiBtn = buttons[2];
            expect(aiBtn.disabled).toBe(true);
            expect(aiBtn.styleClass).toContain('pointer-events-auto');
        });
    });

    describe('Dialog', () => {
        beforeEach(async () => {
            jest.spyOn(store, 'setFileFromContentlet').mockReturnValue(of(null).subscribe());

            spectator.detectChanges();
            await spectator.fixture.whenStable();
            spectator.detectChanges();
        });

        it('should open dialog with code component when click on edit button', async () => {
            const spySetMode = jest.spyOn(store, 'setMode');
            const editorBtn = spectator.query(byTestId('action-editor-btn')) as HTMLButtonElement;
            editorBtn.click();

            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const editorElement = document.querySelector('[data-testid="editor-mode"]'); // This element is added to the body by the dialog
            const isDialogOpen = spectator.fixture.componentInstance.openDialog;

            expect(editorElement).toBeTruthy();
            expect(isDialogOpen).toBeTruthy();
            expect(spySetMode).toHaveBeenCalledWith(BinaryFieldMode.EDITOR);
        });

        it('should open dialog with url component component when click on url button', async () => {
            const spySetMode = jest.spyOn(store, 'setMode');
            const urlBtn = spectator.query(byTestId('action-url-btn')) as HTMLButtonElement;
            urlBtn.click();

            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const urlElement = document.querySelector('[data-testid="url-mode"]'); // This element is added to the body by the dialog
            const isDialogOpen = spectator.fixture.componentInstance.openDialog;

            expect(urlElement).toBeTruthy();
            expect(isDialogOpen).toBeTruthy();
            expect(spySetMode).toHaveBeenCalledWith(BinaryFieldMode.URL);
        });
    });

    describe('Set File', () => {
        describe('Contentlet - BaseTyp FILEASSET', () => {
            it('should set the correct file asset', () => {
                const spy = jest
                    .spyOn(store, 'setFileFromContentlet')
                    .mockReturnValue(of(null).subscribe());
                const mock = {
                    ...MOCK_DOTCMS_FILE,
                    baseType: 'FILEASSET',
                    metaData: fileMetaData
                };
                spectator.setInput('contentlet', mock);
                spectator.detectChanges();
                expect(spy).toHaveBeenCalledWith({
                    ...mock,
                    fieldVariable: BINARY_FIELD_MOCK.variable,
                    value: mock[BINARY_FIELD_MOCK.variable]
                });
            });
        });

        describe('Contentlet - BaseTyp CONTENT', () => {
            it('should set the correct file asset', () => {
                const spy = jest
                    .spyOn(store, 'setFileFromContentlet')
                    .mockReturnValue(of(null).subscribe());
                const variable = BINARY_FIELD_MOCK.variable;
                spectator.setInput('contentlet', MOCK_DOTCMS_FILE);
                spectator.detectChanges();
                expect(spy).toHaveBeenCalledWith({
                    ...MOCK_DOTCMS_FILE,
                    fieldVariable: variable,
                    value: MOCK_DOTCMS_FILE[variable]
                });
            });
        });

        it('should not set file when metadata is not present', () => {
            const spy = jest
                .spyOn(store, 'setFileFromContentlet')
                .mockReturnValue(of(null).subscribe());
            const mock = {
                ...MOCK_DOTCMS_FILE,
                binaryFieldMetaData: null
            };
            spectator.setInput('contentlet', mock);
            spectator.detectChanges();
            expect(spy).not.toHaveBeenCalled();
        });
    });

    describe('Disabled State Management', () => {
        beforeEach(() => {
            spectator.detectChanges();
            store.setStatus(BinaryFieldStatus.INIT);
        });

        it('should set disabled state correctly through setDisabledState method', () => {
            spectator.detectChanges();

            // Initially not disabled
            expect(spectator.component.$disabled()).toBe(false);

            // Set disabled
            spectator.component.setDisabledState(true);
            expect(spectator.component.$disabled()).toBe(true);

            // Set enabled
            spectator.component.setDisabledState(false);
            expect(spectator.component.$disabled()).toBe(false);
        });

        it('should disable file input when field is disabled', () => {
            spectator.detectChanges();

            const fileInput = spectator.query(
                byTestId('binary-field__file-input')
            ) as HTMLInputElement;

            // Initially not disabled
            expect(fileInput.disabled).toBe(false);

            // Set disabled
            spectator.component.setDisabledState(true);
            spectator.detectChanges();
            expect(fileInput.disabled).toBe(true);
        });

        it('should disable action buttons when field is disabled', () => {
            spectator.detectChanges();

            // Set disabled
            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            const urlBtnComponent = spectator.query(byTestId('action-url-btn'));
            const editorBtnComponent = spectator.query(byTestId('action-editor-btn'));

            // Get the actual button elements inside the PrimeNG components
            const actualUrlBtn = urlBtnComponent?.querySelector('button') as HTMLButtonElement;
            const actualEditorBtn = editorBtnComponent?.querySelector(
                'button'
            ) as HTMLButtonElement;

            // Verify buttons are actually disabled
            expect(actualUrlBtn?.disabled).toBe(true);
            expect(actualEditorBtn?.disabled).toBe(true);
        });

        it('should disable AI button when component is disabled', () => {
            // Setup to show AI button
            const systemOptions = {
                allowURLImport: true,
                allowCodeWrite: true,
                allowGenerateImg: true
            };

            const JSONString = JSON.stringify(systemOptions);
            const newField = {
                ...BINARY_FIELD_MOCK,
                fieldVariables: [
                    ...BINARY_FIELD_MOCK.fieldVariables,
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                        fieldId: '5df3f8fc49177c195740bcdc02ec2db7',
                        id: '1ff1ff05-b9fb-4239-ad3d-b2cfaa9a8406',
                        key: 'systemOptions',
                        value: JSONString
                    }
                ]
            };

            spectator = createComponent({
                detectChanges: false,
                props: {
                    field: newField,
                    contentlet: null
                }
            });
            store = spectator.inject(DotBinaryFieldStore, true);

            spectator.detectChanges();

            const aiBtnComponent = spectator.query(byTestId('action-ai-btn'));

            // Verify button exists
            expect(aiBtnComponent).toBeTruthy();

            // Set component disabled - should disable AI button
            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            // Get the actual button element inside the PrimeNG component
            const actualAiBtn = aiBtnComponent?.querySelector('button') as HTMLButtonElement;

            // Button should be disabled when component is disabled
            expect(actualAiBtn?.disabled).toBe(true);

            // Re-enable component
            spectator.component.setDisabledState(false);
            spectator.detectChanges();

            // Note: Button may still be disabled due to AI plugin check (initialValue: false)
            // but the disabled state logic should be working correctly
            expect(spectator.component.$disabled()).toBe(false);
        });

        it('should prevent file selection when disabled', () => {
            const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            const inputElement = spectator.query(
                byTestId('binary-field__file-input')
            ) as HTMLInputElement;
            const file = new File(['test'], 'test.png', { type: 'image/png' });
            const event = new Event('change');
            Object.defineProperty(event, 'target', { value: { files: [file] } });

            inputElement.dispatchEvent(event);

            expect(spyHandleUploadFile).not.toHaveBeenCalled();
        });

        it('should prevent file drop when disabled', () => {
            const spyHandleUploadFile = jest.spyOn(store, 'handleUploadFile');

            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            spectator.component.handleFileDrop(DROP_ZONE_FILE_EVENT);

            expect(spyHandleUploadFile).not.toHaveBeenCalled();
        });

        it('should prevent opening dialogs when disabled', () => {
            const spySetMode = jest.spyOn(store, 'setMode');

            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            // Test each dialog mode
            spectator.component.openDialog(BinaryFieldMode.URL);
            spectator.component.openDialog(BinaryFieldMode.EDITOR);
            spectator.component.openDialog(BinaryFieldMode.AI);

            expect(spySetMode).not.toHaveBeenCalled();
            expect(spectator.component.dialogOpen).toBe(false);
        });

        it('should prevent file picker opening when disabled', () => {
            const spyInputClick = jest.spyOn(spectator.component.inputFile.nativeElement, 'click');

            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            spectator.component.openFilePicker();

            expect(spyInputClick).not.toHaveBeenCalled();
        });

        it('should prevent file removal when disabled', () => {
            const spyRemoveFile = jest.spyOn(store, 'removeFile');

            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            spectator.component.removeFile();

            expect(spyRemoveFile).not.toHaveBeenCalled();
        });

        it('should add disabled CSS class to container when disabled', () => {
            spectator.detectChanges();

            const container = spectator.query('.binary-field__container');

            // Initially not disabled
            expect(container).not.toHaveClass('binary-field__container--disabled');

            // Set disabled
            spectator.component.setDisabledState(true);
            spectator.detectChanges();
            expect(container).toHaveClass('binary-field__container--disabled');
        });

        it('should pass disabled state to preview component when file is uploaded', () => {
            // Set up preview state
            store.setStatus(BinaryFieldStatus.PREVIEW);
            store.setTempFile(TEMP_FILE_MOCK);
            spectator.detectChanges();

            // Set disabled
            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            const previewComponent = spectator.query(DotBinaryFieldPreviewComponent);
            expect(previewComponent).toBeTruthy();
            expect(previewComponent.disabled).toBe(true);
        });
    });

    afterEach(() => {
        jest.resetAllMocks();
    });
});

/**
 *
 * @class MockFormComponent
 */
@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
class MockFormComponent {
    field = BINARY_FIELD_MOCK;
    contentlet = MOCK_DOTCMS_FILE;
    form = new FormGroup({
        binaryField: new FormControl('')
    });
}

describe('DotEditContentBinaryFieldComponent - ControlValueAccessor', () => {
    let spectator: SpectatorHost<DotEditContentBinaryFieldComponent, MockFormComponent>;
    const createHost = createHostFactory({
        component: DotEditContentBinaryFieldComponent,
        host: MockFormComponent,
        imports: [
            ButtonModule,
            DialogModule,
            MonacoEditorModule,
            ReactiveFormsModule,
            DotEditContentBinaryFieldComponent
        ],
        providers: [DotAiService, provideHttpClient()]
    });

    beforeEach(() => {
        spectator = createHost(` <form [formGroup]="form">
            <dot-edit-content-binary-field [contentlet]="contentlet" [field]="field" formControlName="binaryField"></dot-edit-content-binary-field>
        </form>`);
    });

    it('should set form value when binary file changes', () => {
        // Call the onChange method from ControlValueAccessor
        spectator.component.setTempFile(TEMP_FILE_MOCK);
        const formValue = spectator.hostComponent.form.get('binaryField').value; // Get the form value
        expect(formValue).toBe(TEMP_FILE_MOCK.id); // Check if the form value was set
    });
});
