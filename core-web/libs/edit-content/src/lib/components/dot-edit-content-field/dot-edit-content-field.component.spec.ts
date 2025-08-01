import { describe } from '@jest/globals';
import { MonacoEditorModule, MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { EditorComponent } from '@tinymce/tinymce-angular';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Provider, signal, Type } from '@angular/core';
import { ControlContainer, FormGroupDirective, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DialogService } from 'primeng/dynamicdialog';

import { BlockEditorModule, DotBlockEditorComponent } from '@dotcms/block-editor';
import {
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotKeyValueComponent, DotLanguageVariableSelectorComponent } from '@dotcms/ui';
import { monacoMock } from '@dotcms/utils-testing';

import { DotEditContentFieldComponent } from './dot-edit-content-field.component';

import { DotEditContentBinaryFieldComponent } from '../../fields/dot-edit-content-binary-field/dot-edit-content-binary-field.component';
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
        component: DotEditContentBinaryFieldComponent,
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
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ],
        providers: [
            FormGroupDirective,
            provideHttpClient(),
            provideHttpClientTesting(),
            ...(fieldTestBed?.providers || []),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({
            props: {
                field: fieldMock,
                ...(fieldTestBed?.props || {})
            },
            providers: [...(fieldTestBed?.providers || [])]
        });
    });

    describe(`${fieldMock.fieldType} - ${fieldMock.dataType}`, () => {
        if (fieldMock.fieldType !== FIELD_TYPES.CUSTOM_FIELD) {
            it('should render the label', () => {
                spectator.detectChanges();
                const label = spectator.query(byTestId(`label-${fieldMock.variable}`));
                expect(label?.textContent).toContain(fieldMock.name);
            });
        }

        if (fieldMock.fieldType !== FIELD_TYPES.RELATIONSHIP) {
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

        if (fieldTestBed.outsideFormControl) {
            it('should have a formControlName', () => {
                spectator.detectChanges();
                const field = spectator.debugElement.query(
                    By.css(`[data-testId="field-${fieldMock.variable}"]`)
                );
                expect(field.attributes['ng-reflect-name']).toBe(fieldMock.variable);
            });
        }

        if (fieldTestBed.props) {
            describe('With props', () => {
                fieldTestBed.props.forEach((prop) => {
                    it(`should have ${prop.key} property`, () => {
                        spectator.detectChanges();
                        const field = spectator.debugElement.query(
                            By.css(`[data-testId="field-${fieldMock.variable}"]`)
                        );
                        expect(field.componentInstance[prop.key]).toEqual(prop.valueExpected);
                    });
                });
            });
        }
    });
});
