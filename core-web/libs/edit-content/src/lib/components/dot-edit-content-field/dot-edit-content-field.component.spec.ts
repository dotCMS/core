import { describe } from '@jest/globals';
import { MonacoEditorLoaderService, MonacoEditorModule } from '@materia-ui/ngx-monaco-editor';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { EditorComponent } from '@tinymce/tinymce-angular';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Provider, signal, Type } from '@angular/core';
import {
    ControlContainer,
    FormControl,
    FormGroup,
    FormGroupDirective,
    ReactiveFormsModule
} from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { BlockEditorModule, DotBlockEditorComponent } from '@dotcms/block-editor';
import {
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotSystemConfigService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotCMSBaseTypesContentTypes, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotKeyValueComponent, DotLanguageVariableSelectorComponent } from '@dotcms/ui';
import { monacoMock } from '@dotcms/utils-testing';

import { DotEditContentFieldComponent } from './dot-edit-content-field.component';

import { DotBinaryFieldWrapperComponent } from '../../fields/dot-edit-content-binary-field/components/dot-binary-field-wrapper/dot-binary-field-wrapper.component';
import { DotEditContentCalendarFieldComponent } from '../../fields/dot-edit-content-calendar-field/dot-edit-content-calendar-field.component';
import { DotEditContentCategoryFieldComponent } from '../../fields/dot-edit-content-category-field/dot-edit-content-category-field.component';
import { DotEditContentCheckboxFieldComponent } from '../../fields/dot-edit-content-checkbox-field/dot-edit-content-checkbox-field.component';
import { DotEditContentCustomFieldComponent } from '../../fields/dot-edit-content-custom-field/dot-edit-content-custom-field.component';
import { DotEditContentFileFieldComponent } from '../../fields/dot-edit-content-file-field/dot-edit-content-file-field.component';
import { DotFileFieldUploadService } from '../../fields/dot-edit-content-file-field/services/upload-file/upload-file.service';
import { DotEditContentHostFolderFieldComponent } from '../../fields/dot-edit-content-host-folder-field/dot-edit-content-host-folder-field.component';
import { DotEditContentJsonFieldComponent } from '../../fields/dot-edit-content-json-field/dot-edit-content-json-field.component';
import { DotEditContentKeyValueComponent } from '../../fields/dot-edit-content-key-value/dot-edit-content-key-value.component';
import { DotEditContentMultiSelectFieldComponent } from '../../fields/dot-edit-content-multi-select-field/dot-edit-content-multi-select-field.component';
import { DotEditContentRadioFieldComponent } from '../../fields/dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentRelationshipFieldComponent } from '../../fields/dot-edit-content-relationship-field/dot-edit-content-relationship-field.component';
import { DotEditContentSelectFieldComponent } from '../../fields/dot-edit-content-select-field/dot-edit-content-select-field.component';
import { DotEditContentTagFieldComponent } from '../../fields/dot-edit-content-tag-field/dot-edit-content-tag-field.component';
import { DotEditContentTextAreaComponent } from '../../fields/dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from '../../fields/dot-edit-content-text-field/dot-edit-content-text-field.component';
import { DotEditContentWYSIWYGFieldComponent } from '../../fields/dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.component';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotEditContentMonacoEditorControlComponent } from '../../shared/dot-edit-content-monaco-editor-control/dot-edit-content-monaco-editor-control.component';
import { DotEditContentStore } from '../../store/edit-content.store';
import {
    BINARY_FIELD_CONTENTLET,
    createFormGroupDirectiveMock,
    DOT_MESSAGE_SERVICE_MOCK,
    FIELDS_MOCK,
    TREE_SELECT_MOCK
} from '../../utils/mocks';

interface DotEditFieldTestBed {
    component: Type<unknown>;
    imports?: Type<unknown>[];
    providers?: Provider[];
    declarations?: Type<unknown>[];
    props?: { [key: string]: unknown }[]; // ContentField Props, that we need to pass to the component inside
    outsideFormControl?: boolean; //If the component have [formControlName] hardcoded inside this ContentField component
}

/* We need this declare to dont have import errors from CommandType of Tiptap */
declare module '@tiptap/core' {
    interface Commands {
        [key: string]: {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            [key: string]: (...args) => any;
        };
    }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as any).monaco = monacoMock;

// This holds the mapping between the field type and the component that should be used to render it.
// We need to hold this record here, because for some reason the references just fall to undefined.
const FIELD_TYPES_COMPONENTS: Record<FIELD_TYPES, Type<unknown> | DotEditFieldTestBed> = {
    // We had to use unknown because components have different types.
    [FIELD_TYPES.TEXT]: DotEditContentTextFieldComponent,
    [FIELD_TYPES.RELATIONSHIP]: {
        component: DotEditContentRelationshipFieldComponent,
        providers: [mockProvider(DialogService)]
    },
    [FIELD_TYPES.FILE]: {
        component: DotEditContentFileFieldComponent,
        providers: [
            {
                provide: DotFileFieldUploadService,
                useValue: {}
            }
        ]
    },
    [FIELD_TYPES.IMAGE]: {
        component: DotEditContentFileFieldComponent,
        providers: [
            {
                provide: DotFileFieldUploadService,
                useValue: {}
            }
        ]
    },
    [FIELD_TYPES.TEXTAREA]: DotEditContentTextAreaComponent,
    [FIELD_TYPES.SELECT]: DotEditContentSelectFieldComponent,
    [FIELD_TYPES.RADIO]: DotEditContentRadioFieldComponent,
    [FIELD_TYPES.DATE]: DotEditContentCalendarFieldComponent,
    [FIELD_TYPES.DATE_AND_TIME]: DotEditContentCalendarFieldComponent,
    [FIELD_TYPES.TIME]: DotEditContentCalendarFieldComponent,
    [FIELD_TYPES.HOST_FOLDER]: {
        component: DotEditContentHostFolderFieldComponent,
        providers: [
            mockProvider(DotEditContentService, {
                getSitesTreePath: jest.fn().mockReturnValue(of(TREE_SELECT_MOCK))
            })
        ]
    },
    [FIELD_TYPES.TAG]: {
        component: DotEditContentTagFieldComponent,
        providers: [{ provide: DotEditContentService, useValue: { getTags: () => of([]) } }]
    },
    [FIELD_TYPES.CHECKBOX]: DotEditContentCheckboxFieldComponent,
    [FIELD_TYPES.MULTI_SELECT]: DotEditContentMultiSelectFieldComponent,
    [FIELD_TYPES.BLOCK_EDITOR]: {
        component: DotBlockEditorComponent,
        declarations: [MockComponent(DotBlockEditorComponent)],
        imports: [BlockEditorModule],
        outsideFormControl: true
    },
    [FIELD_TYPES.CUSTOM_FIELD]: {
        component: DotEditContentCustomFieldComponent,
        providers: [
            mockProvider(DotEditContentService),
            {
                provide: DotLicenseService,
                useValue: {
                    isEnterprise: () => of(true)
                }
            }
        ]
    },
    [FIELD_TYPES.BINARY]: {
        component: DotBinaryFieldWrapperComponent,
        providers: [
            {
                provide: DotLicenseService,
                useValue: {
                    isEnterprise: () => of(true)
                }
            },
            {
                provide: DotMessageService,
                useValue: DOT_MESSAGE_SERVICE_MOCK
            }
        ],
        props: [
            {
                contentlet: BINARY_FIELD_CONTENTLET
            }
        ],
        outsideFormControl: true
    },
    [FIELD_TYPES.JSON]: {
        component: DotEditContentJsonFieldComponent,
        imports: [ReactiveFormsModule, MonacoEditorModule],
        providers: [
            mockProvider(DotMessageDisplayService),
            { provide: MonacoEditorLoaderService, useValue: { isMonacoLoaded$: of(true) } }
        ],
        declarations: [
            MockComponent(DotLanguageVariableSelectorComponent),
            MockComponent(DotEditContentMonacoEditorControlComponent)
        ]
    },
    [FIELD_TYPES.KEY_VALUE]: {
        component: DotEditContentKeyValueComponent,
        declarations: [MockComponent(DotKeyValueComponent)],
        providers: [mockProvider(DotMessageDisplayService)]
    },
    [FIELD_TYPES.WYSIWYG]: {
        component: DotEditContentWYSIWYGFieldComponent,
        providers: [
            {
                provide: DotFileFieldUploadService,
                useValue: {}
            },
            {
                provide: DotWorkflowActionsFireService,
                useValue: {}
            },
            {
                provide: DotEditContentStore,
                useValue: {
                    showSidebar: signal(false)
                }
            }
        ],
        declarations: [MockComponent(EditorComponent)]
    },
    [FIELD_TYPES.CATEGORY]: {
        component: DotEditContentCategoryFieldComponent
    },
    [FIELD_TYPES.CONSTANT]: {
        component: null // this field is not being rendered for now.
    },
    [FIELD_TYPES.HIDDEN]: {
        component: null // this field is not being rendered for now.
    },
    [FIELD_TYPES.LINE_DIVIDER]: {
        component: null
    }
};

describe('FIELD_TYPES and FIELDS_MOCK', () => {
    it('should be in sync', () => {
        expect(
            Object.values(FIELD_TYPES).every((fieldType) =>
                FIELDS_MOCK.find((f) => f.fieldType === fieldType)
            )
        ).toBeTruthy();
    });
});

const FIELDS_TO_BE_RENDER = FIELDS_MOCK.filter(
    (field) =>
        field.fieldType !== FIELD_TYPES.CONSTANT &&
        field.fieldType !== FIELD_TYPES.HIDDEN &&
        field.fieldType !== FIELD_TYPES.LINE_DIVIDER
);

describe.each([...FIELDS_TO_BE_RENDER])('DotEditContentFieldComponent all fields', (fieldMock) => {
    const fieldTestBed = FIELD_TYPES_COMPONENTS[fieldMock.fieldType];
    let spectator: Spectator<DotEditContentFieldComponent>;

    const createComponent = createComponentFactory({
        imports: [...(fieldTestBed?.imports || [])],
        declarations: [...(fieldTestBed?.declarations || [])],
        component: DotEditContentFieldComponent,
        providers: [
            FormGroupDirective,
            provideHttpClient(),
            provideHttpClientTesting(),
            ...(fieldTestBed?.providers || []),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(
                    of({
                        logos: { loginScreen: '/assets/logo.png', navBar: 'NA' },
                        colors: { primary: '#000000', secondary: '#FFFFFF', background: '#F5F5F5' },
                        releaseInfo: { buildDate: 'Jan 01, 2025', version: 'test' },
                        systemTimezone: {
                            id: 'UTC',
                            label: 'Coordinated Universal Time',
                            offset: 0
                        },
                        languages: [
                            {
                                country: 'United States',
                                countryCode: 'US',
                                id: 1,
                                isoCode: 'en-us',
                                language: 'English',
                                languageCode: 'en'
                            }
                        ],
                        license: {
                            displayServerId: 'serverId',
                            isCommunity: true,
                            level: 100,
                            levelName: 'COMMUNITY'
                        },
                        cluster: { clusterId: 'cluster-id', companyKeyDigest: 'digest' }
                    })
                )
            }),
            mockProvider(CoreWebService)
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({
            props: {
                field: fieldMock,
                ...(fieldTestBed?.props || {})
            },
            providers: [
                ...(fieldTestBed?.providers || []),
                {
                    provide: ControlContainer,
                    useValue: createFormGroupDirectiveMock()
                }
            ]
        });
    });

    describe(`${fieldMock.fieldType} - ${fieldMock.dataType}`, () => {
        if (
            fieldMock.fieldType !== FIELD_TYPES.CUSTOM_FIELD &&
            fieldMock.fieldType !== FIELD_TYPES.DATE &&
            fieldMock.fieldType !== FIELD_TYPES.DATE_AND_TIME &&
            fieldMock.fieldType !== FIELD_TYPES.TIME
        ) {
            it('should render the label', () => {
                spectator.detectChanges();
                const label = spectator.query(byTestId(`label-${fieldMock.variable}`));
                expect(label?.textContent).toContain(fieldMock.name);
            });
        }

        if (
            fieldMock.fieldType !== FIELD_TYPES.DATE &&
            fieldMock.fieldType !== FIELD_TYPES.DATE_AND_TIME &&
            fieldMock.fieldType !== FIELD_TYPES.TIME
        ) {
            it('should render the hint if present', () => {
                spectator.detectChanges();
                const hint = spectator.query(byTestId(`hint-${fieldMock.variable}`));
                expect(hint?.textContent).toContain(fieldMock.hint);
            });
        }

        it('should render the correct field type', () => {
            spectator.detectChanges();
            const FIELD_TYPE = fieldTestBed.component ? fieldTestBed.component : fieldTestBed;
            const component = spectator.query(FIELD_TYPE);

            expect(component).toBeTruthy();
            expect(component instanceof FIELD_TYPE).toBeTruthy();
        });
    });
});

describe('DotEditContentFieldComponent - Binary Field Auto-fill', () => {
    let spectator: Spectator<DotEditContentFieldComponent>;
    let realForm: FormGroup;
    let formGroupDirective: FormGroupDirective;

    const createComponent = createComponentFactory({
        component: DotEditContentFieldComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(
                    of({
                        systemTimezone: {
                            id: 'UTC',
                            label: 'Coordinated Universal Time',
                            offset: 0
                        }
                    })
                )
            }),
            mockProvider(CoreWebService),
            mockProvider(DotMessageService),
            mockProvider(DotMessageDisplayService),
            mockProvider(DotLicenseService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });

    beforeEach(() => {
        // Create a real Angular form
        realForm = new FormGroup({
            title: new FormControl(''),
            fileName: new FormControl(''),
            binaryField: new FormControl('')
        });

        // Create FormGroupDirective with the real form
        formGroupDirective = new FormGroupDirective([], []);
        formGroupDirective.form = realForm;

        spectator = createComponent({
            props: {
                field: FIELDS_MOCK.find((f) => f.fieldType === FIELD_TYPES.BINARY),
                contentType: {
                    baseType: DotCMSBaseTypesContentTypes.FILEASSET
                } as DotCMSContentType
            } as unknown,
            providers: [
                {
                    provide: ControlContainer,
                    useValue: formGroupDirective
                }
            ]
        });
    });

    describe('onBinaryFieldValueUpdated', () => {
        beforeEach(() => {
            // Reset form to initial state
            realForm.get('title')?.setValue('');
            realForm.get('fileName')?.setValue('');
            realForm.get('title')?.markAsUntouched();
            realForm.get('fileName')?.markAsUntouched();
        });

        it('should auto-fill title and fileName when empty for FILEASSET content type', () => {
            const mockEvent = { value: 'temp123', fileName: 'document.pdf' };

            // Verify initial state - both controls should be empty
            expect(realForm.get('title')?.value).toBe('');
            expect(realForm.get('fileName')?.value).toBe('');

            // Call the method
            spectator.component.onBinaryFieldValueUpdated(mockEvent);

            // Verify that both controls were filled
            expect(realForm.get('title')?.value).toBe('document.pdf');
            expect(realForm.get('fileName')?.value).toBe('document.pdf');

            // Verify controls are marked as touched
            expect(realForm.get('title')?.touched).toBe(true);
            expect(realForm.get('fileName')?.touched).toBe(true);
        });

        it('should NOT overwrite existing values', () => {
            const mockEvent = { value: 'temp123', fileName: 'document.pdf' };

            // Set existing values
            realForm.get('title')?.setValue('Existing Title');
            realForm.get('fileName')?.setValue('existing-file.pdf');

            spectator.component.onBinaryFieldValueUpdated(mockEvent);

            // Verify values were not changed
            expect(realForm.get('title')?.value).toBe('Existing Title');
            expect(realForm.get('fileName')?.value).toBe('existing-file.pdf');
        });

        // These tests moved to separate describe blocks to avoid TestBed conflicts

        it('should handle missing form controls gracefully', () => {
            const mockEvent = { value: 'temp123', fileName: 'document.pdf' };

            // Create a form without title and fileName controls
            const emptyForm = new FormGroup({
                binaryField: new FormControl('')
            });
            const emptyFormDirective = new FormGroupDirective([], []);
            emptyFormDirective.form = emptyForm;

            // Replace the form in the component
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (spectator.component as any)['#parentForm'] = emptyForm;

            expect(() => {
                spectator.component.onBinaryFieldValueUpdated(mockEvent);
            }).not.toThrow();
        });

        // These tests moved to separate describe blocks to avoid TestBed conflicts
    });

    describe('shouldAutoFillFields', () => {
        it('should return true for FILEASSET baseType', () => {
            const contentType = {
                baseType: DotCMSBaseTypesContentTypes.FILEASSET
            } as DotCMSContentType;

            const result = spectator.component['shouldAutoFillFields'](contentType);

            expect(result).toBe(true);
        });

        it('should return false for non-FILEASSET baseType', () => {
            const contentType = {
                baseType: 'CONTENT'
            } as DotCMSContentType;

            const result = spectator.component['shouldAutoFillFields'](contentType);

            expect(result).toBe(false);
        });

        it('should return false for null contentType', () => {
            const result = spectator.component['shouldAutoFillFields'](null);

            expect(result).toBe(false);
        });
    });
});

describe('DotEditContentFieldComponent - Binary Field Auto-fill (Non-FILEASSET)', () => {
    let spectator: Spectator<DotEditContentFieldComponent>;
    let testForm: FormGroup;

    const createComponent = createComponentFactory({
        component: DotEditContentFieldComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(
                    of({
                        systemTimezone: {
                            id: 'UTC',
                            label: 'Coordinated Universal Time',
                            offset: 0
                        }
                    })
                )
            }),
            mockProvider(CoreWebService),
            mockProvider(DotMessageService),
            mockProvider(DotMessageDisplayService),
            mockProvider(DotLicenseService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });

    beforeEach(() => {
        testForm = new FormGroup({
            title: new FormControl(''),
            fileName: new FormControl(''),
            binaryField: new FormControl('')
        });

        const formGroupDirective = new FormGroupDirective([], []);
        formGroupDirective.form = testForm;

        spectator = createComponent({
            props: {
                field: FIELDS_MOCK.find((f) => f.fieldType === FIELD_TYPES.BINARY),
                contentType: {
                    baseType: 'CONTENT',
                    variable: 'BlogPost'
                } as DotCMSContentType
            } as unknown,
            providers: [
                {
                    provide: ControlContainer,
                    useValue: formGroupDirective
                }
            ]
        });
    });

    it('should NOT execute for non-FILEASSET content types', () => {
        const mockEvent = { value: 'temp123', fileName: 'document.pdf' };

        spectator.component.onBinaryFieldValueUpdated(mockEvent);

        // Verify values were not changed (should still be empty)
        expect(testForm.get('title')?.value).toBe('');
        expect(testForm.get('fileName')?.value).toBe('');
    });
});

describe('DotEditContentFieldComponent - Binary Field Auto-fill (Null ContentType)', () => {
    let spectator: Spectator<DotEditContentFieldComponent>;
    let testForm: FormGroup;

    const createComponent = createComponentFactory({
        component: DotEditContentFieldComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(
                    of({
                        systemTimezone: {
                            id: 'UTC',
                            label: 'Coordinated Universal Time',
                            offset: 0
                        }
                    })
                )
            }),
            mockProvider(CoreWebService),
            mockProvider(DotMessageService),
            mockProvider(DotMessageDisplayService),
            mockProvider(DotLicenseService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });

    beforeEach(() => {
        testForm = new FormGroup({
            title: new FormControl(''),
            fileName: new FormControl(''),
            binaryField: new FormControl('')
        });

        const formGroupDirective = new FormGroupDirective([], []);
        formGroupDirective.form = testForm;

        spectator = createComponent({
            props: {
                field: FIELDS_MOCK.find((f) => f.fieldType === FIELD_TYPES.BINARY),
                contentType: null
            } as unknown,
            providers: [
                {
                    provide: ControlContainer,
                    useValue: formGroupDirective
                }
            ]
        });
    });

    it('should handle null contentType gracefully', () => {
        const mockEvent = { value: 'temp123', fileName: 'document.pdf' };

        expect(() => {
            spectator.component.onBinaryFieldValueUpdated(mockEvent);
        }).not.toThrow();

        // Verify values were not changed (should still be empty)
        expect(testForm.get('title')?.value).toBe('');
        expect(testForm.get('fileName')?.value).toBe('');
    });
});

describe('DotEditContentFieldComponent - Binary Field Auto-fill (Title Only)', () => {
    let spectator: Spectator<DotEditContentFieldComponent>;
    let titleOnlyForm: FormGroup;

    const createComponent = createComponentFactory({
        component: DotEditContentFieldComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(
                    of({
                        systemTimezone: {
                            id: 'UTC',
                            label: 'Coordinated Universal Time',
                            offset: 0
                        }
                    })
                )
            }),
            mockProvider(CoreWebService),
            mockProvider(DotMessageService),
            mockProvider(DotMessageDisplayService),
            mockProvider(DotLicenseService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });

    beforeEach(() => {
        titleOnlyForm = new FormGroup({
            title: new FormControl(''),
            binaryField: new FormControl('')
        });

        const formGroupDirective = new FormGroupDirective([], []);
        formGroupDirective.form = titleOnlyForm;

        spectator = createComponent({
            props: {
                field: FIELDS_MOCK.find((f) => f.fieldType === FIELD_TYPES.BINARY),
                contentType: {
                    baseType: DotCMSBaseTypesContentTypes.FILEASSET
                } as DotCMSContentType
            } as unknown,
            providers: [
                {
                    provide: ControlContainer,
                    useValue: formGroupDirective
                }
            ]
        });
    });

    it('should only fill empty title control when fileName control does not exist', () => {
        const mockEvent = { value: 'temp123', fileName: 'document.pdf' };

        spectator.component.onBinaryFieldValueUpdated(mockEvent);

        expect(titleOnlyForm.get('title')?.value).toBe('document.pdf');
        expect(titleOnlyForm.get('title')?.touched).toBe(true);
    });
});

describe('DotEditContentFieldComponent - Binary Field Auto-fill (FileName Only)', () => {
    let spectator: Spectator<DotEditContentFieldComponent>;
    let fileNameOnlyForm: FormGroup;

    const createComponent = createComponentFactory({
        component: DotEditContentFieldComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotSystemConfigService, {
                getSystemConfig: jest.fn().mockReturnValue(
                    of({
                        systemTimezone: {
                            id: 'UTC',
                            label: 'Coordinated Universal Time',
                            offset: 0
                        }
                    })
                )
            }),
            mockProvider(CoreWebService),
            mockProvider(DotMessageService),
            mockProvider(DotMessageDisplayService),
            mockProvider(DotLicenseService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    });

    beforeEach(() => {
        fileNameOnlyForm = new FormGroup({
            fileName: new FormControl(''),
            binaryField: new FormControl('')
        });

        const formGroupDirective = new FormGroupDirective([], []);
        formGroupDirective.form = fileNameOnlyForm;

        spectator = createComponent({
            props: {
                field: FIELDS_MOCK.find((f) => f.fieldType === FIELD_TYPES.BINARY),
                contentType: {
                    baseType: DotCMSBaseTypesContentTypes.FILEASSET
                } as DotCMSContentType
            } as unknown,
            providers: [
                {
                    provide: ControlContainer,
                    useValue: formGroupDirective
                }
            ]
        });
    });

    it('should only fill empty fileName control when title control does not exist', () => {
        const mockEvent = { value: 'temp123', fileName: 'document.pdf' };

        spectator.component.onBinaryFieldValueUpdated(mockEvent);

        expect(fileNameOnlyForm.get('fileName')?.value).toBe('document.pdf');
        expect(fileNameOnlyForm.get('fileName')?.touched).toBe(true);
    });
});
