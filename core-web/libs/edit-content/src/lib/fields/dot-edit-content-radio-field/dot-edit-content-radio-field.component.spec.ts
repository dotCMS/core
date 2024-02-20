import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { ControlContainer, FormControl, FormGroup, FormGroupDirective } from '@angular/forms';

import { RadioButton } from 'primeng/radiobutton';

import { DotEditContentRadioFieldComponent } from './dot-edit-content-radio-field.component';

import {
    RADIO_FIELD_BOOLEAN_MOCK,
    RADIO_FIELD_FLOAT_MOCK,
    RADIO_FIELD_INTEGER_MOCK,
    RADIO_FIELD_TEXT_MOCK,
    createFormGroupDirectiveMock
} from '../../utils/mocks';

describe('DotEditContentRadioFieldComponent', () => {
    describe('test with value', () => {
        let spectator: Spectator<DotEditContentRadioFieldComponent>;

        const FAKE_FORM_GROUP = new FormGroup({
            radio: new FormControl('one')
        });

        const createComponent = createComponentFactory({
            component: DotEditContentRadioFieldComponent,
            componentViewProviders: [
                {
                    provide: ControlContainer,
                    useValue: createFormGroupDirectiveMock(FAKE_FORM_GROUP)
                }
            ],
            providers: [FormGroupDirective],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent();
        });

        it('should render radio selected if the form have value', () => {
            spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
            spectator.detectComponentChanges();

            const inputChecked = spectator.queryAll(RadioButton).filter((radio) => radio.checked);
            expect(inputChecked.length).toBe(1);
        });
    });

    describe('test without value', () => {
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
            spectator = createComponent({});
        });

        it('should dont have any value if the form value or defaultValue is null', () => {
            spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
            spectator.detectComponentChanges();

            const inputChecked = spectator.queryAll(RadioButton).filter((radio) => radio.checked);
            expect(inputChecked.length).toBe(0);
        });

        it('should set the key/value the same when bad formatting options passed', () => {
            const RADIO_FIELD_TEXT_MOCK_WITHOUT_VALUE_AND_LABEL = {
                ...RADIO_FIELD_TEXT_MOCK,
                values: 'one\r\ntwo'
            };
            spectator.setInput('field', RADIO_FIELD_TEXT_MOCK_WITHOUT_VALUE_AND_LABEL);
            spectator.detectComponentChanges();

            expect(spectator.queryAll(RadioButton).map((radio) => radio.value)).toEqual([
                'one',
                'two'
            ]);
        });

        it('should render radio options', () => {
            spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
            spectator.detectComponentChanges();
        });

        it('should have label with for attribute and text equal to radio options', () => {
            spectator.setInput('field', RADIO_FIELD_TEXT_MOCK);
            spectator.detectComponentChanges();
            spectator.queryAll(RadioButton).forEach((radio) => {
                expect(spectator.query(`label[for="${radio.inputId}"]`)).toBeTruthy();
                expect(spectator.query(`label[for="${radio.inputId}"]`).textContent).toEqual(
                    radio.label
                );
            });
        });

        it('should set the key/value the same when bad formatting options passed', () => {
            const RADIO_FIELD_FLOAT_MOCK_WITHOUT_VALUE_AND_LABEL = {
                ...RADIO_FIELD_FLOAT_MOCK,
                values: '100.5'
            };
            spectator.setInput('field', RADIO_FIELD_FLOAT_MOCK_WITHOUT_VALUE_AND_LABEL);
            spectator.detectComponentChanges();

            const expectedList = [
                {
                    label: '100.5',
                    value: 100.5
                }
            ];
            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'number')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
        });
    });

    describe('test DataType', () => {
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
            spectator = createComponent({});
        });
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
            spectator.detectComponentChanges();

            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'string')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
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
            spectator.detectComponentChanges();

            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'boolean')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
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
            spectator.detectComponentChanges();

            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'number')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
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
            spectator.detectComponentChanges();

            expect(
                spectator
                    .queryAll(RadioButton)
                    .every((radioOption) => typeof radioOption.value === 'number')
            ).toBeTruthy();

            expectedList.forEach((option) => {
                expect(
                    spectator
                        .queryAll(RadioButton)
                        .find((radioOption) => radioOption.value === option.value)
                ).toBeTruthy();
            });
        });
    });
});
