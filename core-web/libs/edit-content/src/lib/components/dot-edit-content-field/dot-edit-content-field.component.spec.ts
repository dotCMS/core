import { describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DotEditContentFieldComponent } from './dot-edit-content-field.component';
import { FIELD_TYPES, FIELD_TYPES_COMPONENTS } from './utils';

import { FIELDS_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

describe('FIELD_TYPES and FIELDS_MOCK', () => {
    it('should be in sync', () => {
        expect(
            Object.values(FIELD_TYPES).every((fieldType) =>
                FIELDS_MOCK.find((f) => f.fieldType === fieldType)
            )
        ).toBeTruthy();
    });
});

describe.each([...FIELDS_MOCK])('DotFieldComponent', (fieldMock) => {
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

    it('should render the label', () => {
        spectator.detectChanges();
        const label = spectator.query(byTestId(`label-${fieldMock.variable}`));
        expect(label?.textContent).toContain(fieldMock.name);
    });

    it('should render the hint', () => {
        spectator.detectChanges();
        const hint = spectator.query(byTestId(`hint-${fieldMock.variable}`));
        expect(hint?.textContent).toContain(fieldMock.hint);
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
