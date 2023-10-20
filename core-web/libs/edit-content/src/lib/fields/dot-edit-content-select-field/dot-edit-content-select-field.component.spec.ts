import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotEditContentSelectFieldComponent } from './dot-edit-content-select-field.component';

import {
    SELECT_FIELD_BOOLEAN_MOCK,
    SELECT_FIELD_FLOAT_MOCK,
    SELECT_FIELD_INTEGER_MOCK,
    SELECT_FIELD_TEXT_MOCK,
    createFormGroupDirectiveMock
} from '../../utils/mocks';

describe('DotEditContentSelectFieldComponent', () => {
    let spectator: Spectator<DotEditContentSelectFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSelectFieldComponent,
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        providers: [FormGroupDirective],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('DotEditContentSelectFieldComponent with Text DataType', () => {
        it('should have a options array as select with Text dataType', () => {
            const expectedList = [
                {
                    label: 'Option 1',
                    value: 'Test,1'
                },
                {
                    label: 'Option 2',
                    value: '2'
                },
                {
                    label: 'Option 3',
                    value: '3'
                },
                {
                    label: '123-ad',
                    value: '123-ad'
                },
                {
                    label: 'rules and weird code',
                    value: 'rules and weird code'
                }
            ];
            spectator.setInput('field', SELECT_FIELD_TEXT_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });

        it('should if the formControl has no value (Or not defaultValue), the formControl must have the value of the first element, and render the label', () => {
            spectator.setInput('field', SELECT_FIELD_TEXT_MOCK);
            spectator.component.formControl.setValue(null);
            spectator.detectChanges();
            expect(spectator.component.formControl.value).toEqual('Test,1');

            const spanElement = spectator.query('span.p-dropdown-label');
            expect(spanElement).toBeTruthy();
            expect(spanElement.textContent).toEqual('Option 1');
        });

        it('should render the selected value on dropdown', () => {
            spectator.setInput('field', SELECT_FIELD_TEXT_MOCK);
            spectator.component.formControl.setValue('2');
            spectator.detectChanges();

            const spanElement = spectator.query('span.p-dropdown-label');
            expect(spanElement).toBeTruthy();
            expect(spanElement.textContent).toEqual('Option 2');
        });
    });

    describe('DotEditContentSelectFieldComponent with Bool DataType', () => {
        it('should have a options array as select with Bool dataType', () => {
            const expectedList = [
                {
                    label: 'Verdadero',
                    value: true
                },
                {
                    label: 'Falso',
                    value: false
                }
            ];
            spectator.setInput('field', SELECT_FIELD_BOOLEAN_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });
    });
    describe('DotEditContentSelectFieldComponent with Float DataType', () => {
        it('should have a options array as select with Float dataType', () => {
            const expectedList = [
                {
                    label: 'Cien punto cinco',
                    value: 100.5
                },
                {
                    label: 'Diez punto tres',
                    value: 10.3
                }
            ];
            spectator.setInput('field', SELECT_FIELD_FLOAT_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });
    });

    describe('DotEditContentSelectFieldComponent with Bool DataType', () => {
        it('should have a options array as select with Integer dataType', () => {
            const expectedList = [
                {
                    label: 'Cien',
                    value: 100
                },
                {
                    label: 'Mil',
                    value: 1000
                },
                {
                    label: 'Diez mil',
                    value: 10000
                }
            ];
            spectator.setInput('field', SELECT_FIELD_INTEGER_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });

        it('should if have an object without value and label, the label must be the same as the value, and of the same type', () => {
            const SELECT_FIELD_INTEGER_MOCK_WITHOUT_VALUE_AND_LABEL = {
                ...SELECT_FIELD_INTEGER_MOCK,
                values: '1000'
            };
            spectator.setInput('field', SELECT_FIELD_INTEGER_MOCK_WITHOUT_VALUE_AND_LABEL);
            spectator.detectChanges();

            const expectedList = [
                {
                    label: '1000',
                    value: 1000
                }
            ];
            expect(spectator.component.options).toEqual(expectedList);
        });
    });
});
