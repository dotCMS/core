import { describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { Type } from '@angular/core';
import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotEditContentFieldComponent } from './dot-edit-content-field.component';

import { DotEditContentCalendarFieldComponent } from '../../fields/dot-edit-content-calendar-field/dot-edit-content-calendar-field.component';
import { DotEditContentRadioFieldComponent } from '../../fields/dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentSelectFieldComponent } from '../../fields/dot-edit-content-select-field/dot-edit-content-select-field.component';
import { DotEditContentTagFieldComponent } from '../../fields/dot-edit-content-tag-field/dot-edit-content-tag-field.component';
import { DotEditContentTextAreaComponent } from '../../fields/dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from '../../fields/dot-edit-content-text-field/dot-edit-content-text-field.component';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { FIELDS_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

// This holds the mapping between the field type and the component that should be used to render it.
// We need to hold this record here, because for some reason the references just fall to undefined.
const FIELD_TYPES_COMPONENTS: Record<FIELD_TYPES, Type<unknown>> = {
    // We had to use unknown because components have different types.
    [FIELD_TYPES.TEXT]: DotEditContentTextFieldComponent,
    [FIELD_TYPES.TEXTAREA]: DotEditContentTextAreaComponent,
    [FIELD_TYPES.SELECT]: DotEditContentSelectFieldComponent,
    [FIELD_TYPES.RADIO]: DotEditContentRadioFieldComponent,
    [FIELD_TYPES.DATE]: DotEditContentCalendarFieldComponent,
    [FIELD_TYPES.DATE_AND_TIME]: DotEditContentCalendarFieldComponent,
    [FIELD_TYPES.TIME]: DotEditContentCalendarFieldComponent,
    [FIELD_TYPES.TAG]: DotEditContentTagFieldComponent
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
    let spectator: Spectator<DotEditContentFieldComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentFieldComponent,
        componentViewProviders: [
            {
                provide: ControlContainer,
                useValue: createFormGroupDirectiveMock()
            }
        ],
        providers: [FormGroupDirective]
    });

    beforeEach(async () => {
        spectator = createComponent({
            props: {
                field: fieldMock
            }
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
            expect(hint?.textContent ?? '').toContain(fieldMock.hint ?? '');
        });

        it('should render the correct field type', () => {
            spectator.detectChanges();
            const field = spectator.debugElement.query(
                By.css(`[data-testId="field-${fieldMock.variable}"]`)
            );

            expect(
                field.componentInstance instanceof FIELD_TYPES_COMPONENTS[fieldMock.fieldType]
            ).toBeTruthy();
        });
    });
});
