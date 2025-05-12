import { describe } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { EditorComponent } from '@tinymce/tinymce-angular';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Provider, signal, Type } from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { BlockEditorModule, DotBlockEditorComponent } from '@dotcms/block-editor';
import {
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSFieldTypes } from '@dotcms/dotcms-models';
import { DotKeyValueComponent } from '@dotcms/ui';

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
import { DotEditContentService } from '../../services/dot-edit-content.service';
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

// This holds the mapping between the field type and the component that should be used to render it.
// We need to hold this record here, because for some reason the references just fall to undefined.
const FIELD_TYPES_COMPONENTS: Record<DotCMSFieldTypes, DotEditFieldTestBed> = {
    // We had to use unknown because components have different types.
    [DotCMSFieldTypes.TEXT]: {
        component: DotEditContentTextFieldComponent
    },
    [DotCMSFieldTypes.RELATIONSHIP]: {
        component: DotEditContentRelationshipFieldComponent
    },
    [DotCMSFieldTypes.FILE]: {
        component: DotEditContentFileFieldComponent,
        providers: [
            {
                provide: DotFileFieldUploadService,
                useValue: {}
            }
        ]
    },
    [DotCMSFieldTypes.IMAGE]: {
        component: DotEditContentFileFieldComponent,
        providers: [
            {
                provide: DotFileFieldUploadService,
                useValue: {}
            }
        ]
    },
    [DotCMSFieldTypes.TEXTAREA]: {
        component: DotEditContentTextAreaComponent
    },
    [DotCMSFieldTypes.SELECT]: {
        component: DotEditContentSelectFieldComponent
    },
    [DotCMSFieldTypes.RADIO]: {
        component: DotEditContentRadioFieldComponent
    },
    [DotCMSFieldTypes.DATE]: {
        component: DotEditContentCalendarFieldComponent
    },
    [DotCMSFieldTypes.DATE_AND_TIME]: {
        component: DotEditContentCalendarFieldComponent
    },
    [DotCMSFieldTypes.TIME]: {
        component: DotEditContentCalendarFieldComponent
    },
    [DotCMSFieldTypes.HOST_FOLDER]: {
        component: DotEditContentHostFolderFieldComponent,
        providers: [
            mockProvider(DotEditContentService, {
                getSitesTreePath: jest.fn().mockReturnValue(of(TREE_SELECT_MOCK))
            })
        ]
    },
    [DotCMSFieldTypes.TAG]: {
        component: DotEditContentTagFieldComponent,
        providers: [{ provide: DotEditContentService, useValue: { getTags: () => of([]) } }]
    },
    [DotCMSFieldTypes.CHECKBOX]: {
        component: DotEditContentCheckboxFieldComponent
    },
    [DotCMSFieldTypes.MULTI_SELECT]: {
        component: DotEditContentMultiSelectFieldComponent
    },
    [DotCMSFieldTypes.BLOCK_EDITOR]: {
        component: DotBlockEditorComponent,
        declarations: [MockComponent(DotBlockEditorComponent)],
        imports: [BlockEditorModule],
        outsideFormControl: true
    },
    [DotCMSFieldTypes.CUSTOM_FIELD]: {
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
    [DotCMSFieldTypes.BINARY]: {
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
    [DotCMSFieldTypes.JSON]: {
        component: DotEditContentJsonFieldComponent,
        declarations: [MockComponent(DotEditContentJsonFieldComponent)]
    },
    [DotCMSFieldTypes.KEY_VALUE]: {
        component: DotEditContentKeyValueComponent,
        declarations: [MockComponent(DotKeyValueComponent)],
        providers: [mockProvider(DotMessageDisplayService)]
    },
    [DotCMSFieldTypes.WYSIWYG]: {
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
    [DotCMSFieldTypes.CATEGORY]: {
        component: DotEditContentCategoryFieldComponent
    },
    [DotCMSFieldTypes.CONSTANT]: {
        component: null // this field is not being rendered for now.
    },
    [DotCMSFieldTypes.HIDDEN]: {
        component: null // this field is not being rendered for now.
    },
    [DotCMSFieldTypes.LINE_DIVIDER]: {
        component: null
    },
    [DotCMSFieldTypes.ROW]: {
        component: null
    },
    [DotCMSFieldTypes.COLUMN]: {
        component: null
    },
    [DotCMSFieldTypes.TAB_DIVIDER]: {
        component: null
    },
    [DotCMSFieldTypes.COLUMN_BREAK]: {
        component: null
    }
};

describe('FIELD_TYPES and FIELDS_MOCK', () => {
    it('should be in sync', () => {
        expect(
            Object.values(DotCMSFieldTypes)
                .filter(
                    (fieldType) =>
                        fieldType !== DotCMSFieldTypes.LINE_DIVIDER &&
                        fieldType !== DotCMSFieldTypes.ROW &&
                        fieldType !== DotCMSFieldTypes.COLUMN &&
                        fieldType !== DotCMSFieldTypes.TAB_DIVIDER &&
                        fieldType !== DotCMSFieldTypes.COLUMN_BREAK
                )
                .every((fieldType) => FIELDS_MOCK.find((f) => f.fieldType === fieldType))
        ).toBeTruthy();
    });
});

const FIELDS_TO_BE_RENDER = FIELDS_MOCK.filter(
    (field) =>
        field.fieldType !== DotCMSFieldTypes.CONSTANT &&
        field.fieldType !== DotCMSFieldTypes.HIDDEN &&
        field.fieldType !== DotCMSFieldTypes.LINE_DIVIDER &&
        field.fieldType !== DotCMSFieldTypes.ROW &&
        field.fieldType !== DotCMSFieldTypes.COLUMN &&
        field.fieldType !== DotCMSFieldTypes.TAB_DIVIDER &&
        field.fieldType !== DotCMSFieldTypes.COLUMN_BREAK
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
            } as unknown,
            providers: [...(fieldTestBed?.providers || [])]
        });
    });

    describe(`${fieldMock.fieldType} - ${fieldMock.dataType}`, () => {
        if (fieldMock.fieldType !== DotCMSFieldTypes.CUSTOM_FIELD) {
            it('should render the label', () => {
                spectator.detectChanges();
                const label = spectator.query(byTestId(`label-${fieldMock.variable}`));
                expect(label?.textContent).toContain(fieldMock.name);
            });
        }

        if (fieldMock.fieldType !== DotCMSFieldTypes.RELATIONSHIP) {
            it('should render the hint if present', () => {
                spectator.detectChanges();
                const hint = spectator.query(byTestId(`hint-${fieldMock.variable}`));
                expect(hint?.textContent).toContain(fieldMock.hint);
            });
        }

        it('should render the correct field type', () => {
            spectator.detectChanges();
            const FIELD_TYPE = fieldTestBed.component;
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
    });
});
