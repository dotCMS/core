import { MonacoEditorComponent, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';
import { MockComponent } from 'ng-mocks';

import { fakeAsync, tick } from '@angular/core/testing';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessageService, DotUploadService } from '@dotcms/data-access';
import { DotFieldValidationMessageComponent, DotMessagePipe } from '@dotcms/ui';

import { DotBinaryFieldEditorComponent } from './dot-binary-field-editor.component';

import { DotBinaryFieldValidatorService } from '../../service/dot-binary-field-validator/dot-binary-field-validator.service';
import { TEMP_FILE_MOCK } from '../../store/binary-field.store.spec';
import { CONTENTTYPE_FIELDS_MESSAGE_MOCK } from '../../utils/mock';

const EDITOR_MOCK = {
    updateOptions: (_options) => {
        /* noops */
    },
    addCommand: () => {
        /* noops */
    },
    createContextKey: () => {
        /* noops */
    },
    addAction: () => {
        /* noops */
    },
    getOption: () => {
        /* noops */
    }
} as unknown;

globalThis.monaco = {
    languages: {
        getLanguages: () => {
            return [
                {
                    id: 'javascript',
                    extensions: ['.js'],
                    mimetypes: ['text/javascript']
                }
            ];
        }
    }
} as typeof monaco;

describe('DotBinaryFieldEditorComponent', () => {
    let component: DotBinaryFieldEditorComponent;
    let spectator: Spectator<DotBinaryFieldEditorComponent>;

    let dotBinaryFieldValidatorService: DotBinaryFieldValidatorService;

    let dotUploadService: DotUploadService;

    const createComponent = createComponentFactory({
        component: DotBinaryFieldEditorComponent,
        declarations: [MockComponent(MonacoEditorComponent)],
        imports: [
            MonacoEditorModule,
            InputTextModule,
            ButtonModule,
            DotMessagePipe,
            DotFieldValidationMessageComponent
        ],
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
            },
            {
                provide: DotMessageService,
                useValue: CONTENTTYPE_FIELDS_MESSAGE_MOCK
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        component = spectator.component;
        component.editorRef.editor = EDITOR_MOCK as monaco.editor.IStandaloneCodeEditor;
        dotUploadService = spectator.inject(DotUploadService, true);
        dotBinaryFieldValidatorService = spectator.inject(DotBinaryFieldValidatorService);
        dotBinaryFieldValidatorService.setAccept(['image/*', '.ts']);

        spectator.detectChanges();
    });

    describe('label', () => {
        it('should set label and have css class required', () => {
            const label = spectator.query(byTestId('editor-label'));

            expect(label.innerHTML.trim()).toBe('File Name');
            expect(label.className).toBe('p-label-input-required');
        });
    });

    describe('Editor', () => {
        it('should set editor language', fakeAsync(() => {
            component.form.setValue({
                name: 'script.js',
                content: 'test'
            });

            tick(1000);

            expect(component.editorOptions).toEqual({
                ...component.editorOptions,
                language: 'javascript'
            });
            expect(component.mimeType).toBe('text/javascript');
        }));

        it('should emit cancel event when cancel button is clicked', () => {
            const spy = jest.spyOn(component.cancel, 'emit');
            const cancelBtn = spectator.query(byTestId('cancel-button'));

            spectator.click(cancelBtn);

            expect(spy).toHaveBeenCalled();
        });

        it('should emit tempFileUploaded event when import button is clicked if form is valid', () => {
            const spy = jest.spyOn(component.tempFileUploaded, 'emit');
            const spyFormDisabled = jest.spyOn(component.form, 'disable');
            const spyFormEnabled = jest.spyOn(component.form, 'enable');
            const spyFileUpload = jest
                .spyOn(dotUploadService, 'uploadFile')
                .mockReturnValue(Promise.resolve(TEMP_FILE_MOCK));
            const importBtn = spectator.query('[data-testId="import-button"] button');

            component.form.setValue({
                name: 'file-name.ts',
                content: 'test'
            });

            spectator.click(importBtn);

            expect(spy).toHaveBeenCalledWith(TEMP_FILE_MOCK);
            expect(spyFileUpload).toHaveBeenCalled();
            expect(spyFormDisabled).toHaveBeenCalled();
            expect(spyFormEnabled).toHaveBeenCalled();
        });

        it('should not emit tempFileUploaded event when import button is clicked if form is invalid', () => {
            const spy = jest.spyOn(component.tempFileUploaded, 'emit');
            const spyFormDisabled = jest.spyOn(component.form, 'disable');
            const spyFormEnabled = jest.spyOn(component.form, 'enable');
            const spyFileUpload = jest
                .spyOn(dotUploadService, 'uploadFile')
                .mockReturnValue(Promise.resolve(TEMP_FILE_MOCK));
            const importBtn = spectator.query('[data-testId="import-button"] button');

            component.form.setValue({
                name: '',
                content: ''
            });

            spectator.click(importBtn);

            expect(spyFileUpload).not.toHaveBeenCalled();
            expect(spyFormDisabled).not.toHaveBeenCalled();
            expect(spyFormEnabled).not.toHaveBeenCalled();
            expect(spy).not.toHaveBeenCalled();
        });

        it('should mark name control as dirty when import button is clicked and name control is invalid', () => {
            const spyDirty = jest.spyOn(component.form.get('name'), 'markAsDirty');
            const spyDdateValueAndValidity = jest.spyOn(
                component.form.get('name'),
                'updateValueAndValidity'
            );
            const importBtn = spectator.query('[data-testId="import-button"] button');

            spectator.click(importBtn);

            expect(spyDirty).toHaveBeenCalled();
            expect(spyDdateValueAndValidity).toHaveBeenCalled();
        });

        it('should set form as invalid when accept is not valid', fakeAsync(() => {
            const spy = jest.spyOn(component.name, 'setErrors');

            component.form.setValue({
                name: 'test.ts',
                content: 'test'
            });

            tick(1000);

            expect(spy).toHaveBeenCalledWith({
                invalidExtension:
                    'This type of file is not supported. Please use a image/*, .ts file.'
            });
            expect(component.form.valid).toBe(false);
        }));

        afterEach(() => {
            jest.restoreAllMocks();
        });
    });
});
