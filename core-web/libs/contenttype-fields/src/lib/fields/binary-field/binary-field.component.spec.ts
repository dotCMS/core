import { expect, it } from '@jest/globals';
import { MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DropZoneFileEvent } from '@dotcms/ui';

import { DotBinaryFieldComponent } from './binary-field.component';
import { DotBinaryFieldUiMessageComponent } from './components/dot-binary-field-ui-message/dot-binary-field-ui-message.component';
import {
    BINARY_FIELD_MODE,
    BINARY_FIELD_STATUS,
    DotBinaryFieldStore
} from './store/binary-field.store';

import { getUiMessage } from '../../utils/binary-field-utils';
import { CONTENTTYPE_FIELDS_MESSAGE_MOCK } from '../../utils/mock';

const TEMP_FILE_MOCK: DotCMSTempFile = {
    fileName: 'image.png',
    folder: '/images',
    id: '12345',
    image: true,
    length: 1000,
    referenceUrl: '/reference/url',
    thumbnailUrl: 'image.png',
    mimeType: 'mimeType'
};

const file = new File([''], 'filename');
const validity = {
    valid: true,
    fileTypeMismatch: false,
    maxFileSizeExceeded: false,
    multipleFilesDropped: false
};

const DROP_ZONE_FILE_EVENT: DropZoneFileEvent = {
    file,
    validity
};

describe('DotBinaryFieldComponent', () => {
    let spectator: Spectator<DotBinaryFieldComponent>;
    let store: DotBinaryFieldStore;

    const createComponent = createComponentFactory({
        component: DotBinaryFieldComponent,
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
                accept: ['image/*'],
                maxFileSize: 1000,
                helperText: 'helper text'
            }
        });
        store = spectator.inject(DotBinaryFieldStore, true);
    });

    it('should emit temp file', () => {
        const spyEmit = jest.spyOn(spectator.component.tempFile, 'emit');
        spectator.detectChanges();
        store.setTempFile(TEMP_FILE_MOCK);
        expect(spyEmit).toHaveBeenCalledWith(TEMP_FILE_MOCK);
    });

    describe('Dropzone', () => {
        beforeEach(async () => {
            store.setStatus(BINARY_FIELD_STATUS.INIT);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
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
                getUiMessage('FILE_TYPE_MISMATCH', 'image/*')
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
            store.setStatus(BINARY_FIELD_STATUS.PREVIEW);
            store.setTempFile(TEMP_FILE_MOCK);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should remove file and set INIT status when clickin on remove button', async () => {
            const spyRemoveFile = jest.spyOn(store, 'removeFile');
            const remove = spectator.query(byTestId('action-remove-btn')) as HTMLButtonElement;
            remove.click();

            store.vm$.subscribe((state) => {
                expect(state).toEqual({
                    ...state,
                    status: BINARY_FIELD_STATUS.INIT,
                    tempFile: null,
                    file: null
                });
            });

            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const dropZone = spectator.fixture.debugElement.query(By.css('dot-drop-zone'));

            expect(dropZone).toBeTruthy();
            expect(spyRemoveFile).toHaveBeenCalled();
        });
    });

    describe('Template', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should show dropzone when status is INIT', async () => {
            store.setStatus(BINARY_FIELD_STATUS.INIT);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            expect(spectator.query(byTestId('dropzone'))).toBeTruthy();
        });

        it('should show loading when status is UPLOADING', async () => {
            store.setStatus(BINARY_FIELD_STATUS.UPLOADING);
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            expect(spectator.query(byTestId('loading'))).toBeTruthy();
        });

        it('should show preview when status is PREVIEW', async () => {
            store.setStatus(BINARY_FIELD_STATUS.PREVIEW);
            store.setTempFile(TEMP_FILE_MOCK);
            spectator.detectChanges();

            await spectator.fixture.whenStable();

            expect(spectator.query(byTestId('preview'))).toBeTruthy();
        });

        it('should show helper text', () => {
            expect(spectator.query(byTestId('helper-text')).innerHTML).toBe('helper text');
        });
    });

    describe('Dialog', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should open dialog with code component when click on edit button', async () => {
            const spyOpenDialog = jest.spyOn(store, 'openDialog');
            const editorBtn = spectator.query(byTestId('action-editor-btn')) as HTMLButtonElement;
            editorBtn.click();

            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const editorElement = spectator.query(byTestId('editor'));

            expect(editorElement).toBeTruthy();
            expect(spyOpenDialog).toHaveBeenCalledWith(BINARY_FIELD_MODE.EDITOR);
        });

        it('should open dialog with url componet component when click on url button', async () => {
            const spyOpenDialog = jest.spyOn(store, 'openDialog');
            const urlBtn = spectator.query(byTestId('action-url-btn')) as HTMLButtonElement;
            urlBtn.click();

            spectator.detectChanges();
            await spectator.fixture.whenStable();

            const urlElement = spectator.query(byTestId('url'));

            expect(urlElement).toBeTruthy();
            expect(spyOpenDialog).toHaveBeenCalledWith(BINARY_FIELD_MODE.URL);
        });
    });

    afterEach(() => {
        jest.resetAllMocks();
    });
});
