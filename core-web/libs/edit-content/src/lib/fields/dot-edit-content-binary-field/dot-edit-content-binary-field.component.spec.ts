import { expect, it } from '@jest/globals';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import {
    byTestId,
    createComponentFactory,
    createHostFactory,
    Spectator,
    SpectatorHost
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, NgZone } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotLicenseService, DotMessageService, DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DropZoneErrorType, DropZoneFileEvent } from '@dotcms/ui';
import { dotcmsContentletMock } from '@dotcms/utils-testing';

import { DotBinaryFieldUiMessageComponent } from './components/dot-binary-field-ui-message/dot-binary-field-ui-message.component';
import { DotEditContentBinaryFieldComponent } from './dot-edit-content-binary-field.component';
import { BinaryFieldMode, BinaryFieldStatus } from './interfaces';
import { DotBinaryFieldEditImageService } from './service/dot-binary-field-edit-image/dot-binary-field-edit-image.service';
import { DotBinaryFieldValidatorService } from './service/dot-binary-field-validator/dot-binary-field-validator.service';
import { DotBinaryFieldStore } from './store/binary-field.store';
import { getUiMessage } from './utils/binary-field-utils';
import { CONTENTTYPE_FIELDS_MESSAGE_MOCK, FIELD, fileMetaData } from './utils/mock';

const TEMP_FILE_MOCK: DotCMSTempFile = {
    fileName: 'image.png',
    folder: '/images',
    id: '12345',
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

describe('DotEditContentBinaryFieldComponent', () => {
    let spectator: Spectator<DotEditContentBinaryFieldComponent>;
    let store: DotBinaryFieldStore;

    let dotBinaryFieldEditImageService: DotBinaryFieldEditImageService;
    let ngZone: NgZone;

    const createComponent = createComponentFactory({
        component: DotEditContentBinaryFieldComponent,
        imports: [
            NoopAnimationsModule,
            ButtonModule,
            DialogModule,
            MonacoEditorModule,
            DotBinaryFieldUiMessageComponent,
            HttpClientTestingModule
        ],
        componentProviders: [DotBinaryFieldStore],
        providers: [
            DotBinaryFieldEditImageService,
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
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false,
            props: {
                field: FIELD,
                contentlet: null
            }
        });
        store = spectator.inject(DotBinaryFieldStore, true);
        dotBinaryFieldEditImageService = spectator.inject(DotBinaryFieldEditImageService, true);
        ngZone = spectator.inject(NgZone);
    });

    it('should emit temp file', () => {
        const spyEmit = jest.spyOn(spectator.component.valueUpdated, 'emit');
        spectator.detectChanges();
        store.setFile({
            id: TEMP_FILE_MOCK.id,
            titleImage: '',
            ...TEMP_FILE_MOCK.metadata
        });
        expect(spyEmit).toHaveBeenCalledWith(TEMP_FILE_MOCK.id);
    });

    describe('Dropzone', () => {
        beforeEach(async () => {
            spectator.detectChanges();
            store.setStatus(BinaryFieldStatus.INIT);
            await spectator.fixture.whenStable();
            spectator.detectChanges();
        });

        it('should show dropzone when statust is INIT', () => {
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
            store.setFile({
                id: TEMP_FILE_MOCK.id,
                titleImage: '',
                ...TEMP_FILE_MOCK.metadata
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should remove file and set INIT status when remove file ', async () => {
            const spyRemoveFile = jest.spyOn(store, 'removeFile');
            const dotBinarPreviewFile = spectator.fixture.debugElement.query(
                By.css('[data-testId="preview"]')
            );

            dotBinarPreviewFile.componentInstance.removeFile.emit();

            store.vm$.subscribe((state) => {
                expect(state).toEqual({
                    ...state,
                    status: BinaryFieldStatus.INIT,
                    value: '',
                    file: null
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
                const spy = jest.spyOn(dotBinaryFieldEditImageService, 'openImageEditor');
                const dotBinaryFieldPreviewComponent = spectator.fixture.debugElement.query(
                    By.css('dot-binary-field-preview')
                );
                dotBinaryFieldPreviewComponent.triggerEventHandler('editImage');
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
                            `binaryField-tempfile-${FIELD.variable}`,
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
            store.setFile({
                id: TEMP_FILE_MOCK.id,
                titleImage: '',
                ...TEMP_FILE_MOCK.metadata
            });
            spectator.detectChanges();

            await spectator.fixture.whenStable();

            expect(spectator.query(byTestId('preview'))).toBeTruthy();
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

        it('should open dialog with url componet component when click on url button', async () => {
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
                const mockFileAsset = {
                    ...dotcmsContentletMock,
                    baseType: 'FILEASSET',
                    metaData: {
                        ...fileMetaData
                    }
                };
                spectator.setInput('contentlet', mockFileAsset);
                spectator.detectChanges();
                expect(spy).toHaveBeenCalledWith({
                    ...mockFileAsset,
                    variable: FIELD.variable
                });
            });
        });

        describe('Contentlet - BaseTyp CONTENT', () => {
            it('should set the correct file asset', () => {
                const spy = jest
                    .spyOn(store, 'setFileFromContentlet')
                    .mockReturnValue(of(null).subscribe());
                const metaDataKey = `${FIELD.variable}MetaData`;
                const mockFileAsset = {
                    ...dotcmsContentletMock,
                    baseType: 'CONTENT',
                    [metaDataKey]: {
                        ...fileMetaData
                    }
                };
                spectator.setInput('contentlet', mockFileAsset);
                spectator.detectChanges();
                expect(spy).toHaveBeenCalledWith({
                    ...mockFileAsset,
                    variable: FIELD.variable
                });
            });
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
    selector: 'dot-custom-host',
    template: ''
})
class MockFormComponent {
    field = FIELD;
    form = new FormGroup({
        binaryField: new FormControl('')
    });
}

describe('DotEditContentBinaryFieldComponent - ControlValueAccesor', () => {
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
        ]
    });

    beforeEach(() => {
        spectator = createHost(` <form [formGroup]="form">
            <dot-edit-content-binary-field [field]="field" formControlName="binaryField"></dot-edit-content-binary-field>
        </form>`);
    });

    it('should set form value when binary file changes', () => {
        // Call the onChange method from ControlValueAccesor
        spectator.component.setTempFile(TEMP_FILE_MOCK);
        const formValue = spectator.hostComponent.form.get('binaryField').value; // Get the form value
        expect(formValue).toBe(TEMP_FILE_MOCK.id); // Check if the form value was set
    });
});
