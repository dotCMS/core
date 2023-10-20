import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormGroupDirective } from '@angular/forms';

import { DotEditContentRadioFieldComponent } from './dot-edit-content-radio-field.component';

import {
    RADIO_FIELD_BOOLEAN_MOCK,
    RADIO_FIELD_FLOAT_MOCK,
    RADIO_FIELD_INTEGER_MOCK,
    RADIO_FIELD_TEXT_MOCK,
    createFormGroupDirectiveMock
} from '../../utils/mocks';

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
    describe('DotEditContentRadioFieldComponent with Text DataType', () => {
        it('should have a options array as radio with Text dataType', () => {
            const expectedList = [
                {
                    label: 'Uno',
                    value: 'uno'
                },
                {
                    label: 'Dos',
                    value: 'dos'
                }
            ];
            spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
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

        it('should dont have any radio selected if the form value and defaultValue is null', () => {
            spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
            spectator.component.formControl.setValue(null);
            spectator.detectComponentChanges();

            const inputChecked = spectator.queryAll('div.p-radiobutton-checked');
            expect(inputChecked.length).toBe(0);
        });

        it('should render radio selected if the form have value ', () => {
            spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
            spectator.component.formControl.setValue('uno');
            spectator.detectComponentChanges();

            expect(spectator.component.formControl.value).toEqual('uno');

            const inputChecked = spectator.queryAll('div.p-radiobutton-checked');
            expect(inputChecked.length).toBe(1);
        });
    });

    describe('DotEditContentRadioFieldComponent Boolean DataType', () => {
        it('should have a options array as radio with Boolean dataType', () => {
            const expectedList = [
                {
                    label: 'Falso',
                    value: false
                },
                {
                    label: 'Verdadero',
                    value: true
                }
            ];
            spectator.setInput('field', RADIO_FIELD_BOOLEAN_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });
    });

    describe('DotEditContentRadioFieldComponent Integer DataType', () => {
        it('should have a options array as radio with Integer dataType', () => {
            const expectedList = [
                {
                    label: 'Doce',
                    value: 12
                },
                {
                    label: 'Veinte',
                    value: 20
                },
                {
                    label: 'Treinta',
                    value: 30
                }
            ];
            spectator.setInput('field', RADIO_FIELD_INTEGER_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });
    });

    describe('DotEditContentRadioFieldComponent Float DataType', () => {
        it('should have a options array as radio with Float dataType', () => {
            const expectedList = [
                {
                    label: 'Cinco punto dos',
                    value: 5.2
                },
                {
                    label: 'Nueve punto 3',
                    value: 9.3
                }
            ];
            spectator.setInput('field', RADIO_FIELD_FLOAT_MOCK);
            spectator.detectChanges();
            expect(spectator.component.options).toEqual(expectedList);
        });

        it('should if have an object without value and label, the label must be the same as the value, and of the same type', () => {
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
    });
});
