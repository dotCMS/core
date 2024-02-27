import { describe } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { ControlContainer, FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

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

const dotMessageServiceMock = {
    init: jest.fn().mockImplementation(() => {
        // mocking init
    }),
    get: jest.fn().mockImplementation(() => {
        // mocking get
    })
};

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
            },
            providers: [
                {
                    provide: DotMessageService,
                    useValue: dotMessageServiceMock
                },
                mockProvider(ConfirmationService)
            ]
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
