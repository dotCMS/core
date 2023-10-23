import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotEditContentRadioFieldComponent } from './dot-edit-content-radio-field.component';

import {
    RADIO_FIELD_BOOLEAN_MOCK,
    RADIO_FIELD_FLOAT_MOCK,
    RADIO_FIELD_INTEGER_MOCK,
    RADIO_FIELD_TEXT_MOCK,
    createFormGroupDirectiveMock
} from '../../shared/utils/mocks';

describe('DotEditContentRadioFieldComponent', () => {
    let spectator: Spectator<DotEditContentRadioFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentRadioFieldComponent,
        componentViewProviders: [
            { provide: ControlContainer, useValue: createFormGroupDirectiveMock() }
        ],
        providers: [FormGroupDirective],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });
    it('should if have an object without value and label, the label must be the same as the value, and of the same type', () => {
        const RADIO_FIELD_TEXT_MOCK_WITHOUT_VALUE_AND_LABEL = {
            ...RADIO_FIELD_TEXT_MOCK,
            values: 'Dot'
        };
        spectator.setInput('field', RADIO_FIELD_TEXT_MOCK_WITHOUT_VALUE_AND_LABEL);
        spectator.detectChanges();

        const expectedList = [
            {
                label: 'Dot',
                value: 'Dot'
            }
        ];
        expect(spectator.component.options).toEqual(expectedList);
    });

    it('should render radio options', () => {
        spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
        spectator.detectComponentChanges();

        const radioOptions = spectator.queryAll('p-radiobutton');
        expect(radioOptions.length).toBe(2);
    });

    it('should dont have any value if the form value or defaultValue is null', () => {
        spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
        spectator.component.formControl.setValue(null);
        spectator.detectComponentChanges();

        const inputChecked = spectator.queryAll('div.p-radiobutton-checked');
        expect(inputChecked.length).toBe(0);
    });

    it('should render radio selected if the form have value ', () => {
        spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
        spectator.component.formControl.setValue('one');
        spectator.detectComponentChanges();

        expect(spectator.component.formControl.value).toEqual('one');

        const inputChecked = spectator.queryAll('div.p-radiobutton-checked');
        expect(inputChecked.length).toBe(1);
    });

    it('should set the key/value the same when bad formatting options passed', () => {
        const RADIO_FIELD_FLOAT_MOCK_WITHOUT_VALUE_AND_LABEL = {
            ...RADIO_FIELD_FLOAT_MOCK,
            values: '100.5'
        };
        spectator.setInput('field', RADIO_FIELD_FLOAT_MOCK_WITHOUT_VALUE_AND_LABEL);
        spectator.detectChanges();

        const expectedList = [
            {
                label: '100.5',
                value: 100.5
            }
        ];
        expect(spectator.component.options).toEqual(expectedList);
    });

    describe('test DataType', () => {
        it('should have a options array as radio with Text dataType', () => {
            const expectedList = [
                {
                    label: 'One',
                    value: 'one'
                },
                {
                    label: 'Two',
                    value: 'two'
                }
            ];
            spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });

        it('should have a options array as radio with Boolean dataType', () => {
            const expectedList = [
                {
                    label: 'Falsy',
                    value: false
                },
                {
                    label: 'Truthy',
                    value: true
                }
            ];
            spectator.setInput('field', RADIO_FIELD_BOOLEAN_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });
        it('should have a options array as radio with Integer dataType', () => {
            const expectedList = [
                {
                    label: 'Twelve',
                    value: 12
                },
                {
                    label: 'Twenty',
                    value: 20
                },
                {
                    label: 'Thirty',
                    value: 30
                }
            ];
            spectator.setInput('field', RADIO_FIELD_INTEGER_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });

        it('should have a options array as radio with Float dataType', () => {
            const expectedList = [
                {
                    label: 'Five point two',
                    value: 5.2
                },
                {
                    label: 'Nine point three',
                    value: 9.3
                }
            ];
            spectator.setInput('field', RADIO_FIELD_FLOAT_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });
    });
});
