import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { Dropdown } from 'primeng/dropdown';

import { DotEditContentSelectFieldComponent } from './dot-edit-content-select-field.component';

import {
    SELECT_FIELD_BOOLEAN_MOCK,
    SELECT_FIELD_TEXT_MOCK,
    createFormGroupDirectiveMock,
    SELECT_FIELD_INTEGER_MOCK,
    SELECT_FIELD_FLOAT_MOCK
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

    it('should set the first value to the control if no value or defaultValue', () => {
        spectator.setInput('field', SELECT_FIELD_TEXT_MOCK);
        spectator.component.formControl.setValue(null);

        spectator.component.ngOnInit();

        expect(spectator.component.formControl.value).toEqual('Test,1');

        const spanElement = spectator.query('span.p-dropdown-label');
        expect(spanElement).toBeTruthy();
        expect(spanElement.textContent).toEqual('Option 1');
    });

    it('should set the value from control to dropdown', () => {
        spectator.setInput('field', SELECT_FIELD_TEXT_MOCK);
        spectator.component.formControl.setValue('2');
        spectator.detectChanges();

        const spanElement = spectator.query('span.p-dropdown-label');
        expect(spanElement).toBeTruthy();
        expect(spanElement.textContent).toEqual('Option 2');
    });

    it('should set the key/value the same when bad formatting options passed', () => {
        const SELECT_FIELD_INTEGER_MOCK_WITHOUT_VALUE_AND_LABEL = {
            ...SELECT_FIELD_INTEGER_MOCK,
            values: '1000'
        };
        spectator.setInput('field', SELECT_FIELD_INTEGER_MOCK_WITHOUT_VALUE_AND_LABEL);
        spectator.detectComponentChanges();

        const expectedList = [
            {
                label: '1000',
                value: 1000
            }
        ];
        expect(spectator.query(Dropdown).options).toEqual(expectedList);
    });

    describe('test DataType', () => {
        it('should have options array as select with Text', () => {
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
            spectator.detectComponentChanges();
            expect(spectator.query(Dropdown).options).toEqual(expectedList);
        });

        it('should have options array as select with Bool', () => {
            const expectedList = [
                {
                    label: 'Truthy',
                    value: true
                },
                {
                    label: 'Falsy',
                    value: false
                }
            ];
            spectator.setInput('field', SELECT_FIELD_BOOLEAN_MOCK);
            spectator.detectComponentChanges();
            expect(spectator.query(Dropdown).options).toEqual(expectedList);
        });

        it('should have options array as select with Float', () => {
            const expectedList = [
                {
                    label: 'One hundred point five',
                    value: 100.5
                },
                {
                    label: 'Three point five',
                    value: 10.3
                }
            ];
            spectator.setInput('field', SELECT_FIELD_FLOAT_MOCK);
            spectator.detectComponentChanges();
            expect(spectator.query(Dropdown).options).toEqual(expectedList);
        });

        it('should have options array as select with Integer', () => {
            const expectedList = [
                {
                    label: 'One hundred',
                    value: 100
                },
                {
                    label: 'One thousand',
                    value: 1000
                },
                {
                    label: 'Ten thousand',
                    value: 10000
                }
            ];
            spectator.setInput('field', SELECT_FIELD_INTEGER_MOCK);
            spectator.detectComponentChanges();
            expect(spectator.query(Dropdown).options).toEqual(expectedList);
        });
    });
});
