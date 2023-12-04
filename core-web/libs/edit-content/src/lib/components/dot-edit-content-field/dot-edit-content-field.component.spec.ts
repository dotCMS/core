import { describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FactoryProvider, Type } from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { DotBlockEditorComponent } from '@dotcms/block-editor';
import {
    DotContentTypeService,
    DotLicenseService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';

import { DotEditContentFieldComponent } from './dot-edit-content-field.component';

import { DotEditContentBinaryFieldComponent } from '../../fields/dot-edit-content-binary-field/dot-edit-content-binary-field.component';
import { DotEditContentCalendarFieldComponent } from '../../fields/dot-edit-content-calendar-field/dot-edit-content-calendar-field.component';
import { DotEditContentCheckboxFieldComponent } from '../../fields/dot-edit-content-checkbox-field/dot-edit-content-checkbox-field.component';
import { DotEditContentCustomFieldComponent } from '../../fields/dot-edit-content-custom-field/dot-edit-content-custom-field.component';
import { DotEditContentJsonFieldComponent } from '../../fields/dot-edit-content-json-field/dot-edit-content-json-field.component';
import { DotEditContentMultiSelectFieldComponent } from '../../fields/dot-edit-content-multi-select-field/dot-edit-content-multi-select-field.component';
import { DotEditContentRadioFieldComponent } from '../../fields/dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentSelectFieldComponent } from '../../fields/dot-edit-content-select-field/dot-edit-content-select-field.component';
import { DotEditContentTagFieldComponent } from '../../fields/dot-edit-content-tag-field/dot-edit-content-tag-field.component';
import { DotEditContentTextAreaComponent } from '../../fields/dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from '../../fields/dot-edit-content-text-field/dot-edit-content-text-field.component';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import {
    FIELDS_MOCK,
    FIELDS_WITH_CONTENTLET_MOCK,
    createFormGroupDirectiveMock
} from '../../utils/mocks';

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
const FIELD_TYPES_COMPONENTS: Record<
    FIELD_TYPES,
    | Type<unknown>
    | { component: Type<unknown>; providers?: FactoryProvider[]; declarations?: Type<unknown>[] }
> = {
    // We had to use unknown because components have different types.
    [FIELD_TYPES.TEXT]: DotEditContentTextFieldComponent,
    [FIELD_TYPES.TEXTAREA]: DotEditContentTextAreaComponent,
    [FIELD_TYPES.SELECT]: DotEditContentSelectFieldComponent,
    [FIELD_TYPES.RADIO]: DotEditContentRadioFieldComponent,
    [FIELD_TYPES.DATE]: DotEditContentCalendarFieldComponent,
    [FIELD_TYPES.DATE_AND_TIME]: DotEditContentCalendarFieldComponent,
    [FIELD_TYPES.TIME]: DotEditContentCalendarFieldComponent,
    [FIELD_TYPES.TAG]: DotEditContentTagFieldComponent,
    [FIELD_TYPES.CHECKBOX]: DotEditContentCheckboxFieldComponent,
    [FIELD_TYPES.MULTI_SELECT]: DotEditContentMultiSelectFieldComponent,
    [FIELD_TYPES.BLOCK_EDITOR]: DotBlockEditorComponent,
    [FIELD_TYPES.CUSTOM_FIELD]: {
        component: DotEditContentCustomFieldComponent,
        providers: [
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowActionsFireService)
        ]
    },
    [FIELD_TYPES.BINARY]: DotEditContentBinaryFieldComponent,
    [FIELD_TYPES.JSON]: {
        component: DotEditContentJsonFieldComponent,
        declarations: [MockComponent(DotEditContentJsonFieldComponent)]
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

describe.each([...FIELDS_MOCK])('DotEditContentFieldComponent all fields', (fieldMock) => {
    const fieldTestBed = FIELD_TYPES_COMPONENTS[fieldMock.fieldType];
    let spectator: Spectator<DotEditContentFieldComponent>;
    const createComponent = createComponentFactory({
        imports: [HttpClientTestingModule],
        declarations: [...(fieldTestBed?.declarations || [])],
        component: DotEditContentFieldComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ],
        providers: [FormGroupDirective, DotEditContentService, ...(fieldTestBed?.providers || [])]
    });

    beforeEach(async () => {
        spectator = createComponent({
            props: {
                field: fieldMock
            },
            providers: [
                {
                    provide: DotLicenseService,
                    useValue: {
                        isEnterprise: () => of(true)
                    }
                },
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { params: { contentType: 'test' } } }
                }
            ]
        });
    });

    describe(`${fieldMock.fieldType} - ${fieldMock.dataType}`, () => {
        it('should render the label', () => {
            spectator.detectChanges();
            const label = spectator.query(byTestId(`label-${fieldMock.variable}`));
            expect(label?.textContent).toContain(fieldMock.name);
        });

        it('should render the hint if present', () => {
            spectator.detectChanges();
            const hint = spectator.query(byTestId(`hint-${fieldMock.variable}`));
            expect(hint?.textContent).toContain(fieldMock.hint);
        });

        it('should render the correct field type', () => {
            spectator.detectChanges();
            const field = spectator.debugElement.query(
                By.css(`[data-testId="field-${fieldMock.variable}"]`)
            );
            const FIELD_TYPE = fieldTestBed.component ? fieldTestBed.component : fieldTestBed;
            expect(field.componentInstance instanceof FIELD_TYPE).toBeTruthy();
        });
    });
});

describe.each([...FIELDS_WITH_CONTENTLET_MOCK])(
    'DotEditContentFieldComponent all fields with contentlets',
    ({ fieldMock, contentlet }) => {
        let spectator: Spectator<DotEditContentFieldComponent>;
        const createComponent = createComponentFactory({
            component: DotEditContentFieldComponent,
            imports: [HttpClientTestingModule],
            componentViewProviders: [
                {
                    provide: ControlContainer,
                    useValue: createFormGroupDirectiveMock()
                }
            ],
            providers: [mockProvider(DotLicenseService), FormGroupDirective]
        });
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    field: fieldMock,
                    contentlet
                }
            });
        });
        describe(`${fieldMock.fieldType} - ${fieldMock.dataType}`, () => {
            it('should have contentlet', () => {
                spectator.detectChanges();
                const field = spectator.debugElement.query(
                    By.css(`[data-testId="field-${fieldMock.variable}"]`)
                );
                expect(field.componentInstance.contentlet).toEqual(contentlet);
            });
        });
    }
);
