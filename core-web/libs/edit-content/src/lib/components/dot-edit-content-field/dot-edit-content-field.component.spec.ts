import { describe } from '@jest/globals';
import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';
import { ControlContainer, FormGroupDirective, ReactiveFormsModule } from '@angular/forms';

import { DotFieldRequiredDirective } from '@dotcms/ui';

import { DotEditContentFieldComponent } from './dot-edit-content-field.component';

import { DotEditContentFieldsModule } from '../../fields/dot-edit-content-fields.module';
import { FIELDS_MOCK, createFormGroupDirectiveMock } from '../../utils/mocks';

describe.each([...FIELDS_MOCK])('DotFieldComponent', (fieldMock) => {
    let spectator: Spectator<DotEditContentFieldComponent>;
    const createComponent = createComponentFactory({
        component: DotEditContentFieldComponent,
        imports: [
            DotEditContentFieldComponent,
            CommonModule,
            ReactiveFormsModule,
            DotEditContentFieldsModule,
            DotFieldRequiredDirective
        ],
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

    it('should render the field', () => {
        spectator.detectChanges();
        const field = spectator.query(byTestId(`field-${fieldMock.variable}`));
        expect(field).toBeTruthy();
    });
});
